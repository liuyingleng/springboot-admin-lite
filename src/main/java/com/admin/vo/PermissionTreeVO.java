package com.admin.vo;

import lombok.Data;

import java.util.List;

@Data
public class PermissionTreeVO {

    private Long id;

    private String name;

    private String code;

    private String type;

    private String icon;

    private Integer sort;

    private Long parentId;

    private List<PermissionTreeVO> children;
}
