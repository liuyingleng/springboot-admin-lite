package com.admin.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户信息VO
 */
@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String avatar;
    private List<String> roles;
    private List<String> permissions;
}
