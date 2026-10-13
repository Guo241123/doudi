package com.shequ.common.media;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 通用媒体服务实现。
 */
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaMapper mediaMapper;

    @Override
    public void deleteByBiz(String bizType, long bizId) {
        mediaMapper.delete(Wrappers.<Media>lambdaQuery()
                .eq(Media::getBizType, bizType)
                .eq(Media::getBizId, bizId));
    }

    @Override
    public void saveAll(List<Media> medias) {
        if (medias == null || medias.isEmpty()) {
            return;
        }
        for (Media media : medias) {
            mediaMapper.insert(media);
        }
    }

    @Override
    public List<Media> listByBiz(String bizType, Collection<Long> bizIds) {
        if (bizIds == null || bizIds.isEmpty()) {
            return List.of();
        }
        return mediaMapper.selectList(Wrappers.<Media>lambdaQuery()
                .eq(Media::getBizType, bizType)
                .in(Media::getBizId, bizIds)
                .orderByAsc(Media::getSort)
                .orderByAsc(Media::getId));
    }

}
