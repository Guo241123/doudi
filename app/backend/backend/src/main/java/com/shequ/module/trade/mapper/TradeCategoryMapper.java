package com.shequ.module.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shequ.module.trade.entity.TradeCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 二手交易分类数据访问。
 */
@Mapper
public interface TradeCategoryMapper extends BaseMapper<TradeCategory> {

}
