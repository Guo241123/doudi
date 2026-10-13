package com.shequ.module.task.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shequ.common.exception.BusinessException;
import com.shequ.common.media.Media;
import com.shequ.common.media.MediaService;
import com.shequ.common.result.CursorResult;
import com.shequ.common.result.ErrorCode;
import com.shequ.common.storage.FileUrlResolver;
import com.shequ.module.task.dto.AdminTaskSaveDTO;
import com.shequ.module.task.entity.Task;
import com.shequ.module.task.mapper.TaskMapper;
import com.shequ.module.task.service.TaskService;
import com.shequ.module.task.vo.AttachmentVO;
import com.shequ.module.task.vo.TaskDetailVO;
import com.shequ.module.task.vo.TaskItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 任务服务实现（App 端只读）。
 * <p>
 * 游标分页按实现注记 7：{@code WHERE id < ? ORDER BY id DESC LIMIT ?}，
 * 多取一条用于判断 hasMore，避免额外 count 查询。
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private static final String TYPE_IMAGE = "IMAGE";
    private static final String TYPE_VIDEO = "VIDEO";

    private final TaskMapper taskMapper;
    private final MediaService mediaService;
    private final FileUrlResolver fileUrlResolver;

    /** 风险提示文案（★ 后端下发，前端只读） */
    @Value("${task.risk-notice:平台仅展示任务信息，请自行甄别真实性，切勿提前支付任何费用}")
    private String riskNotice;

    @Override
    public CursorResult<TaskItemVO> feed(int cursor, int size) {
        int pageSize = normalizeSize(size);
        List<Task> tasks = taskMapper.selectList(Wrappers.<Task>lambdaQuery()
                .eq(Task::getStatus, Task.STATUS_PUBLISHED)
                .lt(cursor > 0, Task::getId, cursor)
                .orderByDesc(Task::getId)
                .last("LIMIT " + (pageSize + 1)));

        boolean hasMore = tasks.size() > pageSize;
        List<Task> page = hasMore ? new ArrayList<>(tasks.subList(0, pageSize)) : tasks;

        Map<Long, String> covers = coverUrls(page);
        List<TaskItemVO> list = new ArrayList<>(page.size());
        for (Task task : page) {
            list.add(toItem(task, covers.get(task.getId())));
        }
        long nextCursor = page.isEmpty() ? 0 : page.get(page.size() - 1).getId();
        return new CursorResult<>(list, nextCursor, hasMore);
    }

    @Override
    public TaskDetailVO detail(long id) {
        Task task = taskMapper.selectById(id);
        if (task == null || !Objects.equals(task.getStatus(), Task.STATUS_PUBLISHED)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在或已下架");
        }

        List<Media> medias = mediaService.listByBiz(Media.BIZ_TASK, List.of(id));
        List<AttachmentVO> attachments = new ArrayList<>(medias.size());
        String coverUrl = null;
        for (Media media : medias) {
            boolean isVideo = Objects.equals(media.getMediaType(), Media.TYPE_VIDEO);
            if (!isVideo && coverUrl == null) {
                coverUrl = resolveUrl(media.getFileKey());
            }
            AttachmentVO vo = new AttachmentVO();
            vo.setType(isVideo ? TYPE_VIDEO : TYPE_IMAGE);
            vo.setUrl(resolveUrl(media.getFileKey()));
            attachments.add(vo);
        }

        TaskDetailVO vo = new TaskDetailVO();
        fillItem(vo, task, coverUrl);
        vo.setAttachments(attachments);
        vo.setRiskNotice(riskNotice);
        return vo;
    }

    // ------------------------------------------------------------------ #49 后台管理

    @Override
    @Transactional
    public long publish(long adminId, AdminTaskSaveDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        Task task = new Task();
        task.setAdminId(adminId);
        task.setTitle(dto.getTitle());
        task.setSummary(dto.getSummary());
        task.setRewardText(dto.getRewardText());
        task.setDeadline(dto.getDeadline());
        task.setStatus(Task.STATUS_PUBLISHED);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        taskMapper.insert(task);

        saveAttachments(task.getId(), dto.getAttachments(), now);
        return task.getId();
    }

    @Override
    @Transactional
    public void update(long adminId, long id, AdminTaskSaveDTO dto) {
        Task task = requireTask(id);
        task.setTitle(dto.getTitle());
        task.setSummary(dto.getSummary());
        task.setRewardText(dto.getRewardText());
        task.setDeadline(dto.getDeadline());
        // 编辑后回发布状态：误下架后编辑即重新上架，与二手"编辑回待审核"同思路
        task.setStatus(Task.STATUS_PUBLISHED);
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 附件整体替换：先软删旧媒体行留痕，再插新行（与二手编辑媒体同口径）
        mediaService.deleteByBiz(Media.BIZ_TASK, id);
        saveAttachments(id, dto.getAttachments(), LocalDateTime.now());
    }

    @Override
    public void offShelf(long adminId, long id) {
        Task task = requireTask(id);
        if (Objects.equals(task.getStatus(), Task.STATUS_OFF_SHELF)) {
            return; // 幂等：重复下架直接成功
        }
        task.setStatus(Task.STATUS_OFF_SHELF);
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    @Override
    public void delete(long adminId, long id) {
        requireTask(id);
        taskMapper.deleteById(id); // 全局逻辑删除：自动软删
    }

    @Override
    public boolean existsById(long id) {
        return taskMapper.selectById(id) != null;
    }

    // ------------------------------------------------------------------ 私有方法

    /** 后台操作前置：任务必须存在且未软删。 */
    private Task requireTask(long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在或已删除");
        }
        return task;
    }

    /** 附件落 t_media：fileKey 过滤空串；内容安全未接入，机审状态置通过。 */
    private void saveAttachments(long taskId, List<String> attachments, LocalDateTime now) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        List<Media> medias = new ArrayList<>(attachments.size());
        int sort = 0;
        for (String key : attachments) {
            if (!StringUtils.hasText(key)) {
                continue;
            }
            Media media = new Media();
            media.setBizType(Media.BIZ_TASK);
            media.setBizId(taskId);
            media.setMediaType(Media.TYPE_IMAGE);
            media.setFileKey(key);
            media.setSort(sort++);
            media.setAuditStatus(1);
            media.setCreateTime(now);
            media.setUpdateTime(now);
            medias.add(media);
        }
        if (!medias.isEmpty()) {
            mediaService.saveAll(medias);
        }
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /**
     * 列表页封面：一次查全部媒体，取每个任务 sort 最小的一张图片，避免 N+1。
     */
    private Map<Long, String> coverUrls(List<Task> tasks) {
        Map<Long, String> covers = new HashMap<>();
        if (tasks.isEmpty()) {
            return covers;
        }
        List<Long> ids = tasks.stream().map(Task::getId).toList();
        for (Media media : mediaService.listByBiz(Media.BIZ_TASK, ids)) {
            if (Objects.equals(media.getMediaType(), Media.TYPE_IMAGE)) {
                covers.putIfAbsent(media.getBizId(), resolveUrl(media.getFileKey()));
            }
        }
        return covers;
    }

    private void fillItem(TaskItemVO vo, Task task, String coverUrl) {
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setSummary(task.getSummary());
        vo.setRewardText(task.getRewardText());
        vo.setPublishTime(task.getCreateTime());
        vo.setDeadline(task.getDeadline());
        vo.setCoverUrl(coverUrl);
    }

    private TaskItemVO toItem(Task task, String coverUrl) {
        TaskItemVO vo = new TaskItemVO();
        fillItem(vo, task, coverUrl);
        return vo;
    }

    /**
     * fileKey → 可访问 URL，统一由 {@link FileUrlResolver} 决定（本地存储 → /files/**；OSS → endpoint）。
     */
    private String resolveUrl(String fileKey) {
        return fileUrlResolver.resolve(fileKey);
    }

}
