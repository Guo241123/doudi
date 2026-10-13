package com.shequ.module.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 举报页元数据（接口文档 #41：带中文名，前端不写死）。
 */
@Data
@AllArgsConstructor
public class ReportMetaVO {

    private List<Option> targetTypes;

    private List<Option> reasons;

    @Data
    @AllArgsConstructor
    public static class Option {

        private String value;

        private String label;

    }

}
