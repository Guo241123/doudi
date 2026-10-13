package com.shequ.common.sensitive;

/**
 * 敏感词过滤服务：发帖 / 评论"先发后审"的发布前拦截（接口文档 #28 / #33，需求 4.7.2）。
 * <p>
 * MVP 采用最简本地词表实现（见 {@link SensitiveWordServiceImpl}），
 * 后续可平滑替换为 DB 词表（sensitive_word 表）或第三方内容安全服务。
 */
public interface SensitiveWordService {

    /** 命中敏感词抛 {@code BusinessException(40002)}；未命中直接返回。 */
    void check(String text);

    /** 文本是否命中敏感词。 */
    boolean contains(String text);

}
