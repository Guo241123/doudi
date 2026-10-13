package com.shequ.module.report.service;

import com.shequ.module.report.dto.ReportSubmitDTO;
import com.shequ.module.report.vo.ReportMetaVO;

/**
 * 举报服务（接口文档 #41/#42）。
 */
public interface ReportService {

    /** #41 举报页元数据：目标类型与原因枚举（带中文名，前端不写死） */
    ReportMetaVO meta();

    /** #42 提交举报：幂等不限制重复举报同一目标（后台去重处理） */
    void submit(long reporterId, ReportSubmitDTO dto);

}
