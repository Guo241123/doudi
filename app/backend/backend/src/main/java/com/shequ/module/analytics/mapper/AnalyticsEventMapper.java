package com.shequ.module.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shequ.module.analytics.entity.AnalyticsEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 埋点事件表 Mapper。
 */
@Mapper
public interface AnalyticsEventMapper extends BaseMapper<AnalyticsEvent> {

}
