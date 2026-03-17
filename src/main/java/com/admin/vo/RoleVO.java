package com.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleVO {

    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    private LocalDateTime createTime;

    /** 角色拥有的权限 ID 列表（用于回显） */
    private List<Long> permissionIds;
}
