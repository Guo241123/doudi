-- 社区任务与二手交易 App 数据库初始化脚本（W1 用户表 / W3 任务表与通用媒体表）
-- 执行：mysql -uroot -p < schema.sql
-- 说明：全部语句可重复执行（DDL 用 IF NOT EXISTS，种子数据用 INSERT IGNORE + 固定主键）

CREATE DATABASE IF NOT EXISTS shequ DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE shequ;

CREATE TABLE IF NOT EXISTS t_user
(
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    mobile              VARCHAR(11)     NOT NULL COMMENT '手机号',
    password_hash       VARCHAR(100)    NOT NULL COMMENT '密码 BCrypt 哈希（加盐）',
    nickname            VARCHAR(32)     NOT NULL COMMENT '昵称（1~12 字，允许重名）',
    gender              CHAR(1)         NOT NULL DEFAULT 'U' COMMENT '性别 M/F/U',
    avatar_url          VARCHAR(255)             DEFAULT NULL COMMENT '头像完整 URL（OSS+CDN）',
    bio                 VARCHAR(100)             DEFAULT NULL COMMENT '个人简介（≤100 字）',
    status              VARCHAR(20)     NOT NULL DEFAULT 'NORMAL' COMMENT '状态 NORMAL/BANNED/DEACTIVATING',
    ban_reason          VARCHAR(255)             DEFAULT NULL COMMENT '封禁原因',
    deactivate_deadline DATETIME                 DEFAULT NULL COMMENT '注销处理截止时间（提交后 7 天）',
    register_time       DATETIME        NOT NULL COMMENT '注册时间',
    update_time         DATETIME        NOT NULL COMMENT '更新时间',
    is_deleted          TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除 0 正常 / 1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mobile (mobile)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ---------------------------------------------------------------------------
-- task 模块（数据库设计 三、4）
-- 任务无"接单"概念，状态只有 0 发布中 / 1 已下架，无状态流转字段
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_task
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务 ID',
    admin_id    BIGINT          NOT NULL COMMENT '发布管理员 ID',
    title       VARCHAR(50)     NOT NULL COMMENT '标题',
    summary     VARCHAR(200)    NOT NULL COMMENT '简述',
    reward_text VARCHAR(100)    NOT NULL COMMENT '报酬/积分说明（纯文字，平台无积分体系）',
    deadline    DATETIME        NOT NULL COMMENT '截止时间',
    status      TINYINT         NOT NULL DEFAULT 0 COMMENT '0 发布中 / 1 已下架',
    create_time DATETIME        NOT NULL COMMENT '创建时间（即发布时间）',
    update_time DATETIME        NOT NULL COMMENT '更新时间',
    is_deleted  TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除 0 正常 / 1 已删除',
    PRIMARY KEY (id),
    KEY idx_status_id (status, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务表';

-- ---------------------------------------------------------------------------
-- media 通用媒体表（数据库设计 五、7，★ 跨模块复用：TASK / ITEM / POST / AVATAR / AD）
-- 不用 JSON 列存图片列表：机审状态要逐个管理、失败要定位重审
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_media
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '媒体 ID',
    biz_type     VARCHAR(20)     NOT NULL COMMENT '业务类型 TASK/ITEM/POST/AVATAR/AD',
    biz_id       BIGINT          NOT NULL COMMENT '所属业务 ID',
    media_type   TINYINT         NOT NULL DEFAULT 1 COMMENT '1 图片 / 2 视频',
    file_key     VARCHAR(200)    NOT NULL COMMENT 'OSS fileKey',
    sort         INT             NOT NULL DEFAULT 0 COMMENT '排序（轮播顺序）',
    audit_status TINYINT         NOT NULL DEFAULT 0 COMMENT '0 待机审 / 1 通过 / 2 不通过',
    create_time  DATETIME        NOT NULL COMMENT '创建时间',
    update_time  DATETIME        NOT NULL COMMENT '更新时间',
    is_deleted   TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除 0 正常 / 1 已删除',
    PRIMARY KEY (id),
    KEY idx_biz (biz_type, biz_id, sort)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='通用媒体表（跨模块复用）';

-- ---------------------------------------------------------------------------
-- trade 模块（数据库设计 四、5 / 6）
-- 状态：0 待审核 / 1 在售 / 2 已售 / 3 已拒绝 / 4 已下架
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_trade_category
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类 ID',
    name        VARCHAR(10)     NOT NULL COMMENT '分类名（≤6 字）',
    sort        INT             NOT NULL DEFAULT 0 COMMENT '排序（交易页 2×4）',
    icon_key    VARCHAR(200)             DEFAULT NULL COMMENT '图标 fileKey',
    create_time DATETIME        NOT NULL COMMENT '创建时间',
    update_time DATETIME        NOT NULL COMMENT '更新时间',
    is_deleted  TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除 0 正常 / 1 已删除',
    PRIMARY KEY (id),
    KEY idx_sort (sort)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='二手交易分类表（后台可配置增删改排序）';

CREATE TABLE IF NOT EXISTS t_trade_item
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '物品 ID',
    user_id       BIGINT          NOT NULL COMMENT '卖家用户 ID',
    category_id   BIGINT          NOT NULL COMMENT '分类 ID',
    title         VARCHAR(30)     NOT NULL COMMENT '标题（≤30 字）',
    description   VARCHAR(500)    NOT NULL COMMENT '物品介绍（≤500 字）',
    price_fen     INT             NOT NULL COMMENT '价格（分，>0）',
    bargainable   TINYINT         NOT NULL DEFAULT 0 COMMENT '是否可议价 0 否 / 1 是',
    contact       VARCHAR(100)             DEFAULT NULL COMMENT '第三方联系方式（选填，接口层永不返回）',
    status        TINYINT         NOT NULL DEFAULT 0 COMMENT '0 待审核 / 1 在售 / 2 已售 / 3 已拒绝 / 4 已下架',
    reject_reason VARCHAR(100)             DEFAULT NULL COMMENT '拒绝原因',
    audit_time    DATETIME                 DEFAULT NULL COMMENT '审核时间',
    expire_at     DATETIME                 DEFAULT NULL COMMENT '发布时间 + 30 天（定时任务扫它自动下架）',
    create_time   DATETIME        NOT NULL COMMENT '创建时间（即发布时间）',
    update_time   DATETIME        NOT NULL COMMENT '更新时间',
    is_deleted    TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除 0 正常 / 1 已删除',
    PRIMARY KEY (id),
    KEY idx_cat_status_id (category_id, status, id),
    KEY idx_status_id (status, id),
    KEY idx_user (user_id, id),
    KEY idx_expire (status, expire_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='二手物品表';

-- ---------------------------------------------------------------------------
-- favorite 收藏表（数据库设计 五、11，★ 二手与帖子共用：target_type 1 二手 / 2 帖子）
-- 无 is_deleted：关系记录用物理删除，保证唯一键在取消后可再次收藏（幂等）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_favorite
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收藏 ID',
    user_id     BIGINT          NOT NULL COMMENT '收藏人 ID',
    target_type TINYINT         NOT NULL COMMENT '1 二手 / 2 帖子',
    target_id   BIGINT          NOT NULL COMMENT '二手物品 ID 或帖子 ID',
    create_time DATETIME        NOT NULL COMMENT '收藏时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    KEY idx_user_id (user_id, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='收藏表（跨模块复用）';

-- ---------------------------------------------------------------------------
-- 种子数据（联调/演示用；admin_id 暂填 1，待后台 admin 模块落地后替换为真实管理员）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO t_task (id, admin_id, title, summary, reward_text, deadline, status, create_time, update_time, is_deleted)
VALUES (1, 1, '社区周末市集 · 摊位协助', '负责摊位布置、物品登记与现场引导，约 4 小时。', '150 元 / 天，线下结算',
        '2026-10-20 18:00:00', 0, '2026-10-08 15:20:00', '2026-10-08 15:20:00', 0),
       (2, 1, '社区图书角整理志愿者', '图书分类上架、贴标签，可长期参与，时间灵活。', '积分 20 分 / 次（纯文字说明）',
        '2026-10-31 12:00:00', 0, '2026-10-07 10:05:00', '2026-10-07 10:05:00', 0),
       (3, 1, '社区活动摄影记录', '记录活动现场，自备相机，后期简单调色。', '面谈，线下结算',
        '2026-10-25 09:00:00', 0, '2026-10-05 19:42:00', '2026-10-05 19:42:00', 0);

INSERT IGNORE INTO t_media (id, biz_type, biz_id, media_type, file_key, sort, audit_status, create_time, update_time, is_deleted)
VALUES (1, 'TASK', 1, 1,
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=community%20street%20market%20stall&image_size=landscape_4_3',
        0, 1, '2026-10-08 15:20:00', '2026-10-08 15:20:00', 0),
       (2, 'TASK', 1, 1,
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=volunteer%20setting%20up%20market%20booth&image_size=landscape_4_3',
        1, 1, '2026-10-08 15:20:00', '2026-10-08 15:20:00', 0),
       (3, 'TASK', 2, 1,
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=library%20shelves%20organized%20books&image_size=landscape_4_3',
        0, 1, '2026-10-07 10:05:00', '2026-10-07 10:05:00', 0);

-- 二手交易默认 8 个分类（需求 4.2.1：数码/家电/服饰/图书/运动/家居/美妆/其他）
INSERT IGNORE INTO t_trade_category (id, name, sort, icon_key, create_time, update_time, is_deleted)
VALUES (1, '数码', 1, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=digital%20gadgets%20flatlay%20icon&image_size=square', '2026-10-01 00:00:00', '2026-10-01 00:00:00', 0),
       (2, '家电', 2, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=home%20appliances%20icon&image_size=square', '2026-10-01 00:00:00', '2026-10-01 00:00:00', 0),
       (3, '服饰', 3, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=clothing%20fashion%20icon&image_size=square', '2026-10-01 00:00:00', '2026-10-01 00:00:00', 0),
       (4, '图书', 4, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=stack%20of%20books%20icon&image_size=square', '2026-10-01 00:00:00', '2026-10-01 00:00:00', 0),
       (5, '运动', 5, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=sports%20equipment%20icon&image_size=square', '2026-10-01 00:00:00', '2026-10-01 00:00:00', 0),
       (6, '家居', 6, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=home%20furniture%20decor%20icon&image_size=square', '2026-10-01 00:00:00', '2026-10-01 00:00:00', 0),
       (7, '美妆', 7, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=cosmetics%20beauty%20products%20icon&image_size=square', '2026-10-01 00:00:00', '2026-10-01 00:00:00', 0),
       (8, '其他', 8, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=miscellaneous%20items%20box%20icon&image_size=square', '2026-10-01 00:00:00', '2026-10-01 00:00:00', 0);

-- 二手物品演示数据（user_id=1，卖家信息在存在该用户时才返回；expire_at = 发布时间 + 30 天）
INSERT IGNORE INTO t_trade_item (id, user_id, category_id, title, description, price_fen, bargainable, contact, status, reject_reason, audit_time, expire_at, create_time, update_time, is_deleted)
VALUES (1, 1, 1, '九成新 iPad Air 5 64G', '去年买的，一直带壳贴膜，无划痕，配件齐全，同城可自提。', 289900, 1, NULL, 1, NULL,
        '2026-10-08 10:05:00', '2026-11-07 10:00:00', '2026-10-08 10:00:00', '2026-10-08 10:00:00', 0),
       (2, 1, 4, '大学英语四级真题（全新未拆）', '买重复了，全新未拆封，含听力音频二维码。', 2500, 0, NULL, 1, NULL,
        '2026-10-06 09:35:00', '2026-11-05 09:30:00', '2026-10-06 09:30:00', '2026-10-06 09:30:00', 0),
       (3, 1, 2, '小米电饭煲 3L 家用', '搬家闲置，功能正常，可现场试用。', 9900, 1, NULL, 2, NULL,
        '2026-10-05 20:20:00', '2026-11-04 20:15:00', '2026-10-05 20:15:00', '2026-10-05 20:15:00', 0);

INSERT IGNORE INTO t_media (id, biz_type, biz_id, media_type, file_key, sort, audit_status, create_time, update_time, is_deleted)
VALUES (4, 'ITEM', 1, 1,
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=used%20tablet%20on%20desk&image_size=landscape_4_3',
        0, 1, '2026-10-08 10:00:00', '2026-10-08 10:00:00', 0),
       (5, 'ITEM', 1, 1,
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=tablet%20with%20protective%20case&image_size=landscape_4_3',
        1, 1, '2026-10-08 10:00:00', '2026-10-08 10:00:00', 0),
       (6, 'ITEM', 2, 1,
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=english%20exam%20books%20new&image_size=landscape_4_3',
        0, 1, '2026-10-06 09:30:00', '2026-10-06 09:30:00', 0),
       (7, 'ITEM', 3, 1,
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=rice%20cooker%20kitchen%20appliance&image_size=landscape_4_3',
        0, 1, '2026-10-05 20:15:00', '2026-10-05 20:15:00', 0);

-- ---------------------------------------------------------------------------
-- forum 模块（数据库设计 五、8 / 9 / 10）
-- 帖子先发后审：无待审核状态；status 0 已发布 / 1 已删除 / 2 公告
-- like_count / comment_count 为冗余计数列（避免列表页对每条帖子 COUNT，见数据库设计 §10 点赞写流程）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_post
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '帖子 ID',
    user_id       BIGINT          NOT NULL COMMENT '发布者用户 ID（公告帖为管理员账号）',
    content       VARCHAR(1000)   NOT NULL COMMENT '文字内容（≤1000 字）',
    status        TINYINT         NOT NULL DEFAULT 0 COMMENT '0 已发布 / 1 已删除 / 2 公告',
    like_count    INT             NOT NULL DEFAULT 0 COMMENT '点赞数冗余',
    comment_count INT             NOT NULL DEFAULT 0 COMMENT '评论数冗余',
    create_time   DATETIME        NOT NULL COMMENT '创建时间（即发布时间）',
    update_time   DATETIME        NOT NULL COMMENT '更新时间',
    is_deleted    TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除 0 正常 / 1 已删除',
    PRIMARY KEY (id),
    KEY idx_status_id (status, id),
    KEY idx_user (user_id, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='论坛帖子表';

CREATE TABLE IF NOT EXISTS t_comment
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论 ID',
    post_id     BIGINT          NOT NULL COMMENT '所属帖子 ID',
    user_id     BIGINT          NOT NULL COMMENT '评论人 ID',
    content     VARCHAR(200)    NOT NULL COMMENT '内容（≤200 字）',
    reply_to_id BIGINT                   DEFAULT NULL COMMENT '被回复评论 ID（可空，仅两级）',
    like_count  INT             NOT NULL DEFAULT 0 COMMENT '点赞数冗余',
    create_time DATETIME        NOT NULL COMMENT '创建时间',
    update_time DATETIME        NOT NULL COMMENT '更新时间',
    is_deleted  TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除 0 正常 / 1 已删除',
    PRIMARY KEY (id),
    KEY idx_post_hot (post_id, like_count),
    KEY idx_post_time (post_id, create_time),
    KEY idx_user (user_id, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='论坛评论表';

-- ---------------------------------------------------------------------------
-- like_record 点赞表（数据库设计 五、10，★ 帖子与评论共用：target_type 1 帖子 / 2 评论）
-- 无 is_deleted：关系记录用物理删除，唯一键保证幂等（同用户同目标仅计一次）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_like_record
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '点赞记录 ID',
    user_id     BIGINT          NOT NULL COMMENT '点赞人 ID',
    target_type TINYINT         NOT NULL COMMENT '1 帖子 / 2 评论',
    target_id   BIGINT          NOT NULL COMMENT '帖子 ID 或评论 ID',
    create_time DATETIME        NOT NULL COMMENT '点赞时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    KEY idx_target (target_type, target_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='点赞记录表（跨模块复用）';

-- 论坛演示数据（作者取现存用户 id=9；公告帖暂用同一账号，待 admin 模块落地后替换为管理员账号）
INSERT IGNORE INTO t_post (id, user_id, content, status, like_count, comment_count, create_time, update_time, is_deleted)
VALUES (1, 9, '【公告】社区集市本周六上午 9 点在中心广场开市，欢迎携闲置物品前来摆摊，报名请联系管理员。', 2, 0, 0,
        '2026-10-09 09:00:00', '2026-10-09 09:00:00', 0),
       (2, 9, '今天在社区图书角整理了一下午，发现好多好书无人问津，欢迎大家来借阅，也欢迎捐书～', 0, 1, 3,
        '2026-10-08 16:30:00', '2026-10-08 16:30:00', 0),
       (3, 9, '有没有一起晨跑的朋友？每天早上 7 点沿河步道，可以互相监督打卡。', 0, 0, 0,
        '2026-10-07 08:15:00', '2026-10-07 08:15:00', 0);

INSERT IGNORE INTO t_comment (id, post_id, user_id, content, reply_to_id, like_count, create_time, update_time, is_deleted)
VALUES (1, 2, 10, '图书角确实不错，上周借了一本小说，周末就还。', NULL, 2, '2026-10-08 17:00:00', '2026-10-08 17:00:00', 0),
       (2, 2, 9, '欢迎常来～捐书的话可以放在门口收纳箱。', NULL, 0, '2026-10-08 17:20:00', '2026-10-08 17:20:00', 0),
       (3, 2, 10, '好的，那我这周末带几本过去。', 2, 0, '2026-10-08 18:05:00', '2026-10-08 18:05:00', 0);

INSERT IGNORE INTO t_like_record (id, user_id, target_type, target_id, create_time)
VALUES (1, 9, 2, 1, '2026-10-08 17:30:00'),
       (2, 10, 2, 1, '2026-10-08 17:40:00'),
       (3, 9, 1, 2, '2026-10-08 16:50:00');

INSERT IGNORE INTO t_media (id, biz_type, biz_id, media_type, file_key, sort, audit_status, create_time, update_time, is_deleted)
VALUES (8, 'POST', 2, 1,
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=community%20library%20corner%20books%20shelf&image_size=landscape_4_3',
        0, 1, '2026-10-08 16:30:00', '2026-10-08 16:30:00', 0),
       (9, 'POST', 2, 1,
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=donated%20books%20box%20community&image_size=landscape_4_3',
        1, 1, '2026-10-08 16:30:00', '2026-10-08 16:30:00', 0),
       (10, 'POST', 3, 1,
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=morning%20run%20riverside%20path&image_size=landscape_4_3',
        0, 1, '2026-10-07 08:15:00', '2026-10-07 08:15:00', 0);

-- ---------------------------------------------------------------------------
-- admin 管理员表（接口文档 #47：管理员登录，username/password 与 App 手机号体系分离）
-- ID 与 t_user 独立编号空间，token 白名单键 admin:tokens:{id} 与用户互不冲突
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_admin
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '管理员 ID',
    username      VARCHAR(32)     NOT NULL COMMENT '登录名',
    password_hash VARCHAR(100)    NOT NULL COMMENT '密码 BCrypt 哈希（加盐）',
    nickname      VARCHAR(32)     NOT NULL COMMENT '显示昵称（公告作者/客服身份）',
    status        TINYINT         NOT NULL DEFAULT 0 COMMENT '0 启用 / 1 停用',
    create_time   DATETIME        NOT NULL COMMENT '创建时间',
    update_time   DATETIME        NOT NULL COMMENT '更新时间',
    is_deleted    TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除 0 正常 / 1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='管理员表';

-- 种子管理员：admin / <管理员密码占位，见本文件上方的初始化脚本或本地 .env>（联调用，上线前改密）
INSERT IGNORE INTO t_admin (id, username, password_hash, nickname, status, create_time, update_time, is_deleted)
VALUES (1, 'admin', '$2a$10$7DE5CQsjsBNRjihrb/4q2.hDMiZYOJ6QCuws1pxwVB3ex3/rBRhx.', '社区管理员', 0,
        '2026-10-10 20:00:00', '2026-10-10 20:00:00', 0);

-- ---------------------------------------------------------------------------
-- report 举报表（接口文档 #41/#42；处理入口在后台 #54，本轮先建表与提交）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_report
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '举报 ID',
    reporter_id BIGINT          NOT NULL COMMENT '举报人用户 ID',
    target_type VARCHAR(20)     NOT NULL COMMENT '目标类型 ITEM/POST/COMMENT/USER',
    target_id   BIGINT          NOT NULL COMMENT '目标 ID',
    reason      VARCHAR(20)     NOT NULL COMMENT '原因 SPAM/PORN/FRAUD/COPYRIGHT/OTHER',
    description VARCHAR(200)             DEFAULT NULL COMMENT '补充说明（≤200 字）',
    status      TINYINT         NOT NULL DEFAULT 0 COMMENT '0 待处理 / 1 已处理',
    create_time DATETIME        NOT NULL COMMENT '举报时间',
    update_time DATETIME        NOT NULL COMMENT '更新时间',
    is_deleted  TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除 0 正常 / 1 已删除',
    PRIMARY KEY (id),
    KEY idx_status (status, id),
    KEY idx_target (target_type, target_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='举报表';

-- ---------------------------------------------------------------------------
-- blacklist 拉黑表（接口文档 #43/#44；双方内容互不可见，实现注记 3 三口过滤的数据源）
-- 无 is_deleted：关系记录用物理删除，唯一键保证重复拉黑幂等
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_blacklist
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '拉黑记录 ID',
    user_id         BIGINT          NOT NULL COMMENT '拉黑人 ID',
    blocked_user_id BIGINT          NOT NULL COMMENT '被拉黑人 ID',
    create_time     DATETIME        NOT NULL COMMENT '拉黑时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_blocked (user_id, blocked_user_id),
    KEY idx_blocked (blocked_user_id),
    KEY idx_user_id (user_id, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='拉黑表（跨模块复用）';

-- ---------------------------------------------------------------------------
-- message 站内信（接口文档 #36～#40，三类会话同一套）
-- 未读数走 Redis 计数器（#40 只查 Redis），read_flag 落库兜底（Redis 丢失后仍可对账）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_conversation
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话 ID',
    type         VARCHAR(20)     NOT NULL COMMENT 'TASK_CONSULT/CUSTOMER/TRADE',
    user_id      BIGINT          NOT NULL COMMENT '发起用户 ID',
    peer_id      BIGINT          NOT NULL COMMENT '对方 ID（TRADE=卖家用户 ID；TASK_CONSULT/CUSTOMER=管理员 ID）',
    task_id      BIGINT                   DEFAULT NULL COMMENT '关联任务 ID（仅 TASK_CONSULT）',
    last_message VARCHAR(500)             DEFAULT NULL COMMENT '最后一条消息摘要',
    last_time    DATETIME                 DEFAULT NULL COMMENT '最后一条消息时间',
    create_time  DATETIME        NOT NULL COMMENT '创建时间',
    update_time  DATETIME        NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id, id),
    KEY idx_peer (peer_id, id),
    KEY idx_type_user (type, user_id, task_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='站内信会话表';

CREATE TABLE IF NOT EXISTS t_message
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息 ID',
    conversation_id BIGINT          NOT NULL COMMENT '会话 ID',
    sender_id       BIGINT          NOT NULL COMMENT '发送者 ID（用户或管理员）',
    content         VARCHAR(500)    NOT NULL COMMENT '内容（1～500 字）',
    read_flag       TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未读 / 1 已读',
    create_time     DATETIME        NOT NULL COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_conv_id (conversation_id, id),
    KEY idx_conv_read (conversation_id, read_flag)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='站内信消息表';

-- ---------------------------------------------------------------------------
-- analytics 埋点事件表（接口文档 #46，实现注记 9：内存队列批量落表，不引入 MQ）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_analytics_event
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '事件 ID',
    user_id     BIGINT                   DEFAULT NULL COMMENT '用户 ID（未登录为空）',
    device_id   VARCHAR(64)              DEFAULT NULL COMMENT '设备号（未登录必填）',
    name        VARCHAR(30)     NOT NULL COMMENT '事件名 APP_LAUNCH/PAGE_VIEW/AD_IMPRESSION/AD_CLICK/PUBLISH',
    props       VARCHAR(500)             DEFAULT NULL COMMENT '事件属性 JSON（如 page=forum）',
    event_time  DATETIME        NOT NULL COMMENT '事件发生时间（客户端上报）',
    create_time DATETIME        NOT NULL COMMENT '落库时间',
    KEY idx_name_time (name, event_time),
    KEY idx_user (user_id),
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='埋点事件表（批量写，主键仅为 InnoDB 必要）';

-- ---------------------------------------------------------------------------
-- ad_report 广告曝光/点击记录表（接口文档 #45，游客也统计、失败静默、异步写）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_ad_report
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录 ID',
    ad_id       BIGINT          NOT NULL COMMENT '广告素材 ID',
    slot        VARCHAR(30)     NOT NULL COMMENT '广告位 TRADE_BANNER/TASK_BANNER/FEED/DETAIL/INTERSTITIAL',
    action      VARCHAR(20)     NOT NULL COMMENT 'IMPRESSION 曝光 / CLICK 点击',
    user_id     BIGINT                   DEFAULT NULL COMMENT '用户 ID（游客为空）',
    create_time DATETIME        NOT NULL COMMENT '发生时间',
    KEY idx_slot_action (slot, action, create_time),
    KEY idx_ad (ad_id, create_time),
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='广告曝光/点击记录表';