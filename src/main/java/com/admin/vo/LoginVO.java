package com.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应VO
 */
@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private UserInfoVO userInfo;
}
