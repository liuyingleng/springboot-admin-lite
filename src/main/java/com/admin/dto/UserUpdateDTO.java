package com.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserUpdateDTO {

    private String email;

    private String phone;

    private String nickname;

    private String avatar;

    private List<Long> roleIds;
}
