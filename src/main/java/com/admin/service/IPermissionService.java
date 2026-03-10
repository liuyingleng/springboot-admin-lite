package com.admin.service;

import com.admin.vo.MenuVO;

import java.util.List;

/**
 * 权限服务接口
 */
public interface IPermissionService {
    
    /**
     * 获取用户权限列表
     */
    List<String> getUserPermissions(Long userId);
    
    /**
     * 获取用户角色列表
     */
    List<String> getUserRoles(Long userId);
    
    /**
     * 获取用户菜单树
     */
    List<MenuVO> getUserMenuTree(Long userId);
    
    /**
     * 清除用户权限缓存
     */
    void clearUserPermissionCache(Long userId);
}
