package com.admin.vo;

import lombok.Data;

import java.util.List;

/**
 * 菜单树 VO
 */
@Data
public class MenuVO {
    private Long id;
    private String name;
    private String code;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Long parentId;
    private List<MenuVO> children;
}
