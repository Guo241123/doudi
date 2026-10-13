package com.shequ.module.task.service;

import com.shequ.common.result.CursorResult;
import com.shequ.module.task.dto.AdminTaskSaveDTO;
import com.shequ.module.task.vo.TaskDetailVO;
import com.shequ.module.task.vo.TaskItemVO;

/**
 * 任务服务（App 端只读 #15/#16；后台管理 #49）。
 */
public interface TaskService {

    /**
     * #15 任务信息流：状态过滤 + id 游标倒序，仅返回发布中的任务。
     *
     * @param cursor 游标，首页传 0
     * @param size   每页条数，非法值回落默认 20、超上限截断为 50
     */
    CursorResult<TaskItemVO> feed(int cursor, int size);

    /**
     * #16 任务详情；任务不存在、已软删或已下架均返回 40401。
     */
    TaskDetailVO detail(long id);

    /**
     * #49 后台发布任务；附件为 fileKey 数组，落 t_media（bizType=TASK）。
     */
    long publish(long adminId, AdminTaskSaveDTO dto);

    /**
     * #49 后台编辑任务；附件整体替换，任务回发布状态（原为下架时重新可见）。
     */
    void update(long adminId, long id, AdminTaskSaveDTO dto);

    /**
     * #49 后台下架任务；已下架重复调用幂等成功。
     */
    void offShelf(long adminId, long id);

    /**
     * #49 后台删除任务（软删，MyBatis-Plus 逻辑删除）。
     */
    void delete(long adminId, long id);

    /**
     * 任务是否存在（供消息模块校验任务咨询关联，跨模块只调 Service 接口）。
     */
    boolean existsById(long id);

}
