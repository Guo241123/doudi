package com.shequ.common.media;

import java.util.Collection;
import java.util.List;

/**
 * 通用媒体服务：跨模块取媒体时只调本接口，禁止各模块直查 t_media。
 */
public interface MediaService {

    /**
     * 按业务批量取媒体，按 sort 升序（轮播顺序）；一次查询解决列表页的 N+1。
     *
     * @param bizType 业务类型，如 {@link Media#BIZ_TASK}
     * @param bizIds  业务 ID 集合，为空时直接返回空列表
     */
    List<Media> listByBiz(String bizType, Collection<Long> bizIds);

    /**
     * 软删除某业务下的全部媒体（编辑物品时先清后插，保留历史行便于 OSS 清理与机审追溯）。
     */
    void deleteByBiz(String bizType, long bizId);

    /**
     * 批量写入媒体；sort 由调用方按顺序给定。
     */
    void saveAll(List<Media> medias);

}
