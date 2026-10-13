package com.shequ.module.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shequ.common.exception.BusinessException;
import com.shequ.common.result.CursorResult;
import com.shequ.common.result.ErrorCode;
import com.shequ.common.result.PageResult;
import com.shequ.module.admin.service.AdminAuthService;
import com.shequ.module.message.dto.ConversationCreateDTO;
import com.shequ.module.message.dto.MessageSendDTO;
import com.shequ.module.message.entity.Conversation;
import com.shequ.module.message.entity.Message;
import com.shequ.module.message.mapper.ConversationMapper;
import com.shequ.module.message.mapper.MessageMapper;
import com.shequ.module.message.service.MessageService;
import com.shequ.module.message.vo.AdminConversationVO;
import com.shequ.module.message.vo.ConversationVO;
import com.shequ.module.message.vo.MessageSendVO;
import com.shequ.module.message.vo.MessageVO;
import com.shequ.module.message.vo.UnreadCountVO;
import com.shequ.module.report.service.BlacklistService;
import com.shequ.module.task.service.TaskService;
import com.shequ.module.user.entity.User;
import com.shequ.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 站内信实现（#36～#40）。
 * <p>
 * 未读数实时口径走 Redis 计数器（#40 只查 Redis，配合 5s/30s 动态轮询），
 * t_message.read_flag 落库兜底：进会话时把对方消息置已读并清计数器，
 * Redis 丢失后未读数按 0 处理（可接受，宁可少报不误报）。
 * <p>
 * 实现注记 3 三口过滤之三（站内信）：存在任一方向拉黑关系时禁止发送（#39 返回 40301）。
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final UserService userService;
    private final AdminAuthService adminAuthService;
    private final TaskService taskService;
    private final BlacklistService blacklistService;
    private final StringRedisTemplate redis;

    // ------------------------------------------------------------------ #36 发起 / 复用会话

    @Override
    public long createConversation(long userId, ConversationCreateDTO dto) {
        String type = dto.getType();
        long peerId;
        Long taskId = null;

        switch (type) {
            case Conversation.TYPE_TRADE -> {
                if (dto.getPeerUserId() == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少对方用户 ID");
                }
                peerId = dto.getPeerUserId();
                if (peerId == userId) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "不能与自己的物品发起会话");
                }
                if (userService.getById(peerId) == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "对方用户不存在");
                }
            }
            case Conversation.TYPE_TASK_CONSULT -> {
                if (dto.getTaskId() == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少任务 ID");
                }
                if (!taskService.existsById(dto.getTaskId())) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在或已删除");
                }
                peerId = Conversation.DEFAULT_ADMIN_ID;
                taskId = dto.getTaskId();
            }
            case Conversation.TYPE_CUSTOMER -> peerId = Conversation.DEFAULT_ADMIN_ID;
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "会话类型不合法");
        }

        // 会话复用：同类型 + 同对象（TRADE 再叠加任务维度）。
        // TRADE 双方共用一行，故反向也要能命中——否则卖家自己点「联系卖家」会再建一条平行会话。
        Long taskIdFinal = taskId;
        LambdaQueryWrapper<Conversation> reuseQuery = Wrappers.<Conversation>lambdaQuery()
                .eq(Conversation::getType, type)
                .eq(taskIdFinal != null, Conversation::getTaskId, taskIdFinal);
        if (Conversation.TYPE_TRADE.equals(type)) {
            reuseQuery.and(w -> w.and(x -> x.eq(Conversation::getUserId, userId)
                            .eq(Conversation::getPeerId, peerId))
                    .or(x -> x.eq(Conversation::getUserId, peerId)
                            .eq(Conversation::getPeerId, userId)));
        } else {
            reuseQuery.eq(Conversation::getUserId, userId).eq(Conversation::getPeerId, peerId);
        }
        Conversation existing = conversationMapper.selectOne(reuseQuery
                .orderByDesc(Conversation::getId)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing.getId();
        }

        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = new Conversation();
        conversation.setType(type);
        conversation.setUserId(userId);
        conversation.setPeerId(peerId);
        conversation.setTaskId(taskId);
        conversation.setCreateTime(now);
        conversation.setUpdateTime(now);
        conversationMapper.insert(conversation);
        return conversation.getId();
    }

    // ------------------------------------------------------------------ #37 会话列表

    @Override
    public CursorResult<ConversationVO> conversations(long userId, String type, int cursor, int size) {
        int pageSize = normalizeSize(size);
        List<Conversation> rows = conversationMapper.selectList(Wrappers.<Conversation>lambdaQuery()
                .eq(StringUtils.hasText(type), Conversation::getType, type)
                .lt(cursor > 0, Conversation::getId, cursor)
                // TRADE 会话双方共用同一行（发起人 = user_id，卖家 = peer_id），故卖家一侧也要能列出来
                .and(w -> w.eq(Conversation::getUserId, userId)
                        .or(o -> o.eq(Conversation::getType, Conversation.TYPE_TRADE)
                                .eq(Conversation::getPeerId, userId)))
                .orderByDesc(Conversation::getId)
                .last("LIMIT " + (pageSize + 1)));

        boolean hasMore = rows.size() > pageSize;
        List<Conversation> page = hasMore ? new ArrayList<>(rows.subList(0, pageSize)) : rows;

        // 批量装配：TRADE 的对方是用户（注意可能是我发起、也可能是对方发起，取"另一边"）；任务咨询关联任务标题
        Set<Long> peerUserIds = page.stream()
                .filter(c -> Conversation.TYPE_TRADE.equals(c.getType()))
                .map(c -> otherSide(c, userId))
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, User> peers = peerUserIds.isEmpty() ? Map.of()
                : userService.listByIds(peerUserIds).stream()
                        .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
        Map<Long, String> adminNames = new HashMap<>();
        Map<Long, String> taskTitles = new HashMap<>();

        List<ConversationVO> list = new ArrayList<>(page.size());
        for (Conversation conversation : page) {
            ConversationVO vo = new ConversationVO();
            vo.setConversationId(conversation.getId());
            vo.setType(conversation.getType());
            vo.setPeer(peerOf(conversation, userId, peers, adminNames));
            vo.setTaskRef(taskRefOf(conversation, taskTitles));
            vo.setLastMessage(conversation.getLastMessage());
            vo.setLastTime(conversation.getLastTime());
            vo.setUnreadCount(unreadOf(conversation.getId(), userId));
            list.add(vo);
        }
        long nextCursor = page.isEmpty() ? 0 : page.get(page.size() - 1).getId();
        return new CursorResult<>(list, nextCursor, hasMore);
    }

    // ------------------------------------------------------------------ #38 会话内消息

    @Override
    public CursorResult<MessageVO> messages(long userId, long conversationId, int cursor, int size) {
        Conversation conversation = requireMember(userId, conversationId);
        int pageSize = normalizeSize(size);

        List<Message> rows = messageMapper.selectList(Wrappers.<Message>lambdaQuery()
                .eq(Message::getConversationId, conversationId)
                .lt(cursor > 0, Message::getId, cursor)
                .orderByDesc(Message::getId)
                .last("LIMIT " + (pageSize + 1)));

        boolean hasMore = rows.size() > pageSize;
        List<Message> page = hasMore ? new ArrayList<>(rows.subList(0, pageSize)) : rows;

        List<MessageVO> list = new ArrayList<>(page.size());
        for (Message message : page) {
            list.add(new MessageVO(message.getId(), message.getSenderId(), message.getContent(),
                    message.getCreateTime(), Objects.equals(message.getReadFlag(), 1)));
        }

        // 进入会话自动已读：对方发的消息落库置已读 + 清我的 Redis 计数器
        markRead(conversation, userId);
        return new CursorResult<>(list, page.isEmpty() ? 0 : page.get(page.size() - 1).getId(), hasMore);
    }

    // ------------------------------------------------------------------ #39 发送消息

    @Override
    @Transactional
    public MessageSendVO send(long userId, long conversationId, MessageSendDTO dto) {
        Conversation conversation = requireMember(userId, conversationId);
        // 实现注记 3 之三（站内信口）：TRADE 会话存在拉黑关系则禁止发送（判断"与我对话的另一方"）
        if (Conversation.TYPE_TRADE.equals(conversation.getType())
                && blacklistService.blockedBetween(userId, otherSide(conversation, userId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "因拉黑关系，无法发送消息");
        }

        LocalDateTime now = LocalDateTime.now();
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setContent(dto.getContent());
        message.setReadFlag(0);
        message.setCreateTime(now);
        messageMapper.insert(message);

        conversation.setLastMessage(dto.getContent());
        conversation.setLastTime(now);
        conversation.setUpdateTime(now);
        conversationMapper.updateById(conversation);

        // 未读计数：给"与我对话的另一方"加一（共用一行会话，故不能固定用 peer_id）
        redis.opsForValue().increment(
                com.shequ.common.constant.RedisKeys.convUnread(conversationId, otherSide(conversation, userId)));
        return new MessageSendVO(message.getId(), now);
    }

    // ------------------------------------------------------------------ #40 未读数

    @Override
    public UnreadCountVO unreadCount(long userId) {
        List<Conversation> conversations = conversationMapper.selectList(Wrappers.<Conversation>lambdaQuery()
                // 同上：TRADE 会话作为卖家一侧也有未读
                .and(w -> w.eq(Conversation::getUserId, userId)
                        .or(o -> o.eq(Conversation::getType, Conversation.TYPE_TRADE)
                                .eq(Conversation::getPeerId, userId))));

        Map<String, Integer> byType = new LinkedHashMap<>();
        byType.put(Conversation.TYPE_TASK_CONSULT, 0);
        byType.put(Conversation.TYPE_CUSTOMER, 0);
        byType.put(Conversation.TYPE_TRADE, 0);
        int total = 0;
        for (Conversation conversation : conversations) {
            int unread = unreadOf(conversation.getId(), userId);
            if (unread > 0) {
                byType.merge(conversation.getType(), unread, Integer::sum);
                total += unread;
            }
        }
        return new UnreadCountVO(total, byType);
    }

    // ------------------------------------------------------------------ #55 后台站内信工作台

    @Override
    public PageResult<AdminConversationVO> adminConversations(long adminId, String type, int page, int size) {
        int pageNo = page <= 0 ? 1 : page;
        int pageSize = normalizeSize(size);
        int offset = (pageNo - 1) * pageSize;

        Long total = conversationMapper.selectCount(Wrappers.<Conversation>lambdaQuery()
                .eq(StringUtils.hasText(type), Conversation::getType, type));

        // 任务咨询优先（文档 2.10 #55 明确要求），其余按会话新→旧。
        // 这里用 .last 承载整段 ORDER BY（不再调 orderBy*，否则会与之拼接冲突）。
        List<Conversation> rows = conversationMapper.selectList(Wrappers.<Conversation>lambdaQuery()
                .eq(StringUtils.hasText(type), Conversation::getType, type)
                .last("ORDER BY (type = '" + Conversation.TYPE_TASK_CONSULT + "') DESC, id DESC"
                        + " LIMIT " + pageSize + " OFFSET " + offset));

        // 批量装配会话发起人（后台看的是 App 用户，不是 App 端的"对方"）
        Set<Long> userIds = rows.stream().map(Conversation::getUserId).collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, Integer> unreadMap = adminUnreadOf(adminId, rows);
        Map<Long, String> taskTitles = new HashMap<>();

        List<AdminConversationVO> list = new ArrayList<>(rows.size());
        for (Conversation c : rows) {
            AdminConversationVO vo = new AdminConversationVO();
            vo.setConversationId(c.getId());
            vo.setType(c.getType());

            AdminConversationVO.UserInfo userInfo = new AdminConversationVO.UserInfo();
            userInfo.setUserId(c.getUserId());
            User user = users.get(c.getUserId());
            userInfo.setNickname(user == null ? "已注销用户" : user.getNickname());
            userInfo.setAvatarUrl(user == null ? null : user.getAvatarUrl());
            vo.setUser(userInfo);

            vo.setTaskRef(taskRefOf(c, taskTitles));
            vo.setLastMessage(c.getLastMessage());
            vo.setLastTime(c.getLastTime());
            vo.setUnreadCount(unreadMap.getOrDefault(c.getId(), 0));
            list.add(vo);
        }

        long totalCount = total == null ? 0L : total;
        boolean hasMore = (long) offset + list.size() < totalCount;
        return new PageResult<>(list, totalCount, pageNo, pageSize, hasMore);
    }

    @Override
    public CursorResult<MessageVO> adminMessages(long adminId, long conversationId, int cursor, int size) {
        requireConversation(conversationId);
        int pageSize = normalizeSize(size);

        List<Message> rows = messageMapper.selectList(Wrappers.<Message>lambdaQuery()
                .eq(Message::getConversationId, conversationId)
                .lt(cursor > 0, Message::getId, cursor)
                .orderByDesc(Message::getId)
                .last("LIMIT " + (pageSize + 1)));

        boolean hasMore = rows.size() > pageSize;
        List<Message> page = hasMore ? new ArrayList<>(rows.subList(0, pageSize)) : rows;

        List<MessageVO> list = new ArrayList<>(page.size());
        for (Message message : page) {
            list.add(new MessageVO(message.getId(), message.getSenderId(), message.getContent(),
                    message.getCreateTime(), Objects.equals(message.getReadFlag(), 1)));
        }

        // 管理员进会话：把用户发来的消息置为"管理员已读"（不动 App 用户自己的未读计数器）
        markAdminRead(conversationId, adminId);
        return new CursorResult<>(list, page.isEmpty() ? 0 : page.get(page.size() - 1).getId(), hasMore);
    }

    @Override
    @Transactional
    public MessageSendVO adminReply(long adminId, long conversationId, MessageSendDTO dto) {
        Conversation conversation = requireConversation(conversationId);

        LocalDateTime now = LocalDateTime.now();
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(adminId);
        message.setContent(dto.getContent());
        message.setReadFlag(0);
        message.setCreateTime(now);
        messageMapper.insert(message);

        conversation.setLastMessage(dto.getContent());
        conversation.setLastTime(now);
        conversation.setUpdateTime(now);
        conversationMapper.updateById(conversation);

        // 给 App 用户（会话发起人）加未读：用户进会话时按 convUnread(cid, userId) 清零
        redis.opsForValue().increment(
                com.shequ.common.constant.RedisKeys.convUnread(conversationId, conversation.getUserId()));
        return new MessageSendVO(message.getId(), now);
    }

    // ------------------------------------------------------------------ 私有方法

    /**
     * 管理员视角未读数：用户发来、管理员还没看的条数。
     * TRADE 会话是买卖双方之间的事、管理员不是参与方，不计入工作台未读。
     */
    private Map<Long, Integer> adminUnreadOf(long adminId, List<Conversation> rows) {
        List<Long> ids = rows.stream()
                .filter(c -> !Conversation.TYPE_TRADE.equals(c.getType()))
                .map(Conversation::getId)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Message> unread = messageMapper.selectList(Wrappers.<Message>lambdaQuery()
                .select(Message::getConversationId)
                .in(Message::getConversationId, ids)
                .ne(Message::getSenderId, adminId)
                .eq(Message::getReadFlag, 0));
        Map<Long, Integer> map = new HashMap<>();
        for (Message message : unread) {
            map.merge(message.getConversationId(), 1, Integer::sum);
        }
        return map;
    }

    private Conversation requireConversation(long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        return conversation;
    }

    private void markAdminRead(long conversationId, long adminId) {
        messageMapper.update(null, Wrappers.<Message>lambdaUpdate()
                .set(Message::getReadFlag, 1)
                .eq(Message::getConversationId, conversationId)
                .ne(Message::getSenderId, adminId)
                .eq(Message::getReadFlag, 0));
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /**
     * 会话成员校验：TRADE 会话双方共用同一行（发起人 = user_id，卖家 = peer_id），两边都能访问；
     * TASK_CONSULT / CUSTOMER 的对方是管理员（走后台），这里只认发起人。
     */
    private Conversation requireMember(long userId, long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !isMember(conversation, userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        return conversation;
    }

    /** 是否会话成员。 */
    private boolean isMember(Conversation conversation, long userId) {
        if (Objects.equals(conversation.getUserId(), userId)) {
            return true;
        }
        return Conversation.TYPE_TRADE.equals(conversation.getType())
                && Objects.equals(conversation.getPeerId(), userId);
    }

    /**
     * 会话中"与我对话的另一方"：我是发起人则返回 peer_id，我（卖家）是被发起方则返回 user_id。
     * 用于取对方昵称、未读计数、拉黑判断——共用一行会话时不能固定用 peer_id。
     */
    private long otherSide(Conversation conversation, long userId) {
        return Objects.equals(conversation.getUserId(), userId)
                ? conversation.getPeerId()
                : conversation.getUserId();
    }

    private ConversationVO.Peer peerOf(Conversation conversation, long viewerId, Map<Long, User> tradePeers,
                                       Map<Long, String> adminNames) {
        ConversationVO.Peer peer = new ConversationVO.Peer();
        if (Conversation.TYPE_TRADE.equals(conversation.getType())) {
            long peerId = otherSide(conversation, viewerId);
            peer.setUserId(peerId);
            User user = tradePeers.get(peerId);
            peer.setNickname(user == null ? "已注销用户" : user.getNickname());
            peer.setAvatarUrl(user == null ? null : user.getAvatarUrl());
        } else {
            peer.setUserId(conversation.getPeerId());
            peer.setNickname(adminNames.computeIfAbsent(conversation.getPeerId(),
                    id -> adminAuthService.nicknameOf(id)));
        }
        return peer;
    }

    private ConversationVO.TaskRef taskRefOf(Conversation conversation, Map<Long, String> taskTitles) {
        if (!Conversation.TYPE_TASK_CONSULT.equals(conversation.getType())
                || conversation.getTaskId() == null) {
            return null;
        }
        ConversationVO.TaskRef ref = new ConversationVO.TaskRef();
        ref.setTaskId(conversation.getTaskId());
        // 任务已下架/删除时仍展示会话，标题降级为固定文案
        ref.setTitle(taskTitles.computeIfAbsent(conversation.getTaskId(), id -> {
            try {
                return taskService.detail(id).getTitle();
            } catch (BusinessException e) {
                return "任务已删除";
            }
        }));
        return ref;
    }

    private int unreadOf(long conversationId, long userId) {
        String value = redis.opsForValue().get(com.shequ.common.constant.RedisKeys.convUnread(conversationId, userId));
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void markRead(Conversation conversation, long userId) {
        messageMapper.update(null, Wrappers.<Message>lambdaUpdate()
                .set(Message::getReadFlag, 1)
                .eq(Message::getConversationId, conversation.getId())
                .ne(Message::getSenderId, userId)
                .eq(Message::getReadFlag, 0));
        redis.delete(com.shequ.common.constant.RedisKeys.convUnread(conversation.getId(), userId));
    }

}
