package com.shequ.module.user.service;

import com.shequ.module.user.dto.DeactivateDTO;
import com.shequ.module.user.dto.LoginPasswordDTO;
import com.shequ.module.user.dto.LoginSmsDTO;
import com.shequ.module.user.dto.PasswordResetDTO;
import com.shequ.module.user.dto.RegisterCheckMobileDTO;
import com.shequ.module.user.dto.RegisterNicknameDTO;
import com.shequ.module.user.dto.RegisterPasswordDTO;
import com.shequ.module.user.dto.SmsSendDTO;
import com.shequ.module.user.vo.LoginVO;
import com.shequ.module.user.vo.RegisterTokenVO;
import com.shequ.module.user.vo.SmsSendVO;
import com.shequ.module.user.vo.TokenVO;

/**
 * 注册登录与账号（接口文档 2.1 认证与账号 #0～#9）。
 */
public interface AuthService {

    /** #1 发送短信验证码 */
    SmsSendVO sendSmsCode(SmsSendDTO dto, String ip);

    /** #2 注册①：校验手机号与验证码 */
    RegisterTokenVO registerCheckMobile(RegisterCheckMobileDTO dto);

    /** #3 注册②：设置密码（中间态存 Redis） */
    void registerSetPassword(RegisterPasswordDTO dto);

    /** #4 注册③：设置昵称，完成注册并登录 */
    LoginVO registerSetNickname(RegisterNicknameDTO dto);

    /** #5 密码登录 */
    LoginVO loginByPassword(LoginPasswordDTO dto, String ip);

    /** #6 验证码登录 */
    LoginVO loginBySms(LoginSmsDTO dto);

    /** #7 忘记密码-重置（重置后自动登录） */
    TokenVO resetPassword(PasswordResetDTO dto);

    /** #8 退出登录：只作废当前 token */
    void logout(long userId, String jti);

    /** #9 注销账号：进入 7 天处理期并立即失效全部 token */
    void deactivate(long userId, DeactivateDTO dto);

}