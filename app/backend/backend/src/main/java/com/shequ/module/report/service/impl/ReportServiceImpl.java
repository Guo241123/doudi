package com.shequ.module.report.service.impl;

import com.shequ.module.report.dto.ReportSubmitDTO;
import com.shequ.module.report.entity.Report;
import com.shequ.module.report.mapper.ReportMapper;
import com.shequ.module.report.service.ReportService;
import com.shequ.module.report.vo.ReportMetaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 举报服务实现。举报处理（#54）在后台模块，这里只负责提交与元数据。
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;

    @Override
    public ReportMetaVO meta() {
        return new ReportMetaVO(
                List.of(
                        new ReportMetaVO.Option(Report.TARGET_ITEM, "二手物品"),
                        new ReportMetaVO.Option(Report.TARGET_POST, "帖子"),
                        new ReportMetaVO.Option(Report.TARGET_COMMENT, "评论"),
                        new ReportMetaVO.Option(Report.TARGET_USER, "用户")),
                List.of(
                        new ReportMetaVO.Option("SPAM", "垃圾广告"),
                        new ReportMetaVO.Option("PORN", "色情低俗"),
                        new ReportMetaVO.Option("FRAUD", "诈骗"),
                        new ReportMetaVO.Option("COPYRIGHT", "侵权"),
                        new ReportMetaVO.Option("OTHER", "其他")));
    }

    @Override
    public void submit(long reporterId, ReportSubmitDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(dto.getTargetType());
        report.setTargetId(dto.getTargetId());
        report.setReason(dto.getReason());
        report.setDescription(dto.getDescription());
        report.setStatus(Report.STATUS_PENDING);
        report.setCreateTime(now);
        report.setUpdateTime(now);
        reportMapper.insert(report);
    }

}
