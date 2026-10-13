package com.shequ.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shequ.module.user.dto.MobileChangeDTO;
import com.shequ.module.user.dto.PasswordChangeDTO;
import com.shequ.module.user.dto.ProfileUpdateDTO;
import com.shequ.module.user.entity.User;
import com.shequ.module.user.vo.UserProfileVO;
import com.shequ.module.user.vo.UserSummaryVO;
import com.shequ.module.user.vo.UserVO;

/**
 * 用户基础服务 + 个人资料与账号安全（接口文档 2.1 #10～#14）。
 */
public interface UserService extends IService<User> {

    /**
     * 按手机号查询（自动过滤软删除）。
     *
     * @return 不存在返回 null
     */
    User getByMobile(String mobile);

    /** #10 我的资料 */
    UserProfileVO getProfile(long userId);

    /** #11 编辑资料 */
    UserVO updateProfile(long userId, ProfileUpdateDTO dto);

    /** #12 更换手机号（成功后清除全部设备 token） */
    void changeMobile(long userId, MobileChangeDTO dto);

    /** #13 修改密码（成功后清除全部设备 token） */
    void changePassword(long userId, PasswordChangeDTO dto);

    /** #14 我的信息概览 */
    UserSummaryVO getSummary(long userId);

}
