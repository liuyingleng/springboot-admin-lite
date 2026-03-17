package com.admin.service.impl;

import com.admin.common.RedisConstants;
import com.admin.entity.Permission;
import com.admin.entity.Role;
import com.admin.entity.RolePermission;
import com.admin.entity.UserRole;
import com.admin.mapper.PermissionMapper;
import com.admin.mapper.RoleMapper;
import com.admin.mapper.RolePermissionMapper;
import com.admin.mapper.UserRoleMapper;
import com.admin.service.IPermissionService;
import com.admin.utils.RedisUtil;
import com.admin.vo.MenuVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements IPermissionService {

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<String> getUserPermissions(Long userId) {
        String key = RedisConstants.USER_PERMISSIONS_PREFIX + userId;
        Object cache = redisUtil.get(key);
        if (cache != null) {
            return (List<String>) cache;
        }

        List<String> permissions = queryUserPermissionCodes(userId);
        redisUtil.set(key, permissions, RedisConstants.PERMISSION_EXPIRE_TIME);
        return permissions;
    }

    @Override
    public List<String> getUserRoles(Long userId) {
        String key = RedisConstants.USER_ROLES_PREFIX + userId;
        Object cache = redisUtil.get(key);
        if (cache != null) {
            return (List<String>) cache;
        }

        List<String> roles = queryUserRoleCodes(userId);
        redisUtil.set(key, roles, RedisConstants.PERMISSION_EXPIRE_TIME);
        return roles;
    }

    @Override
    public List<MenuVO> getUserMenuTree(Long userId) {
        String key = RedisConstants.USER_MENU_PREFIX + userId;
        Object cache = redisUtil.get(key);
        if (cache != null) {
            return (List<MenuVO>) cache;
        }

        // 获取用户有权限的 permissionId 集合
        Set<Long> permissionIds = getUserPermissionIds(userId);

        // 查询菜单类型权限，按 sort 排序
        List<Permission> allMenus = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .eq(Permission::getType, "menu")
                        .orderByAsc(Permission::getSort)
        );

        // 过滤出用户有权限的菜单
        List<MenuVO> menuList = allMenus.stream()
                .filter(p -> permissionIds.contains(p.getId()))
                .map(p -> {
                    MenuVO vo = new MenuVO();
                    BeanUtils.copyProperties(p, vo);
                    return vo;
                })
                .collect(Collectors.toList());

        List<MenuVO> menuTree = buildMenuTree(menuList, 0L);
        redisUtil.set(key, menuTree, RedisConstants.MENU_EXPIRE_TIME);
        return menuTree;
    }

    @Override
    public void clearUserPermissionCache(Long userId) {
        redisUtil.del(
                RedisConstants.USER_PERMISSIONS_PREFIX + userId,
                RedisConstants.USER_ROLES_PREFIX + userId,
                RedisConstants.USER_MENU_PREFIX + userId
        );
    }

    /**
     * 查询用户拥有的权限 code 列表（通过 用户->角色->权限 关联）
     */
    private List<String> queryUserPermissionCodes(Long userId) {
        Set<Long> permissionIds = getUserPermissionIds(userId);
        if (permissionIds.isEmpty()) return new ArrayList<>();

        return permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .in(Permission::getId, permissionIds)
        ).stream().map(Permission::getCode).collect(Collectors.toList());
    }

    /**
     * 查询用户拥有的角色 code 列表
     */
    private List<String> queryUserRoleCodes(Long userId) {
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) return new ArrayList<>();

        return roleMapper.selectList(
                new LambdaQueryWrapper<Role>().in(Role::getId, roleIds)
        ).stream().map(Role::getRoleCode).collect(Collectors.toList());
    }

    /**
     * 获取用户的角色 ID 列表
     */
    public List<Long> getUserRoleIds(Long userId) {
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        ).stream().map(UserRole::getRoleId).collect(Collectors.toList());
    }

    /**
     * 获取用户的权限 ID 集合（通过角色）
     */
    private Set<Long> getUserPermissionIds(Long userId) {
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) return Collections.emptySet();

        return rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds)
        ).stream().map(RolePermission::getPermissionId).collect(Collectors.toSet());
    }

    /**
     * 构建菜单树
     */
    private List<MenuVO> buildMenuTree(List<MenuVO> menuList, Long parentId) {
        List<MenuVO> tree = new ArrayList<>();
        for (MenuVO menu : menuList) {
            Long pid = menu.getParentId() == null ? 0L : menu.getParentId();
            if (pid.equals(parentId)) {
                List<MenuVO> children = buildMenuTree(menuList, menu.getId());
                menu.setChildren(children.isEmpty() ? null : children);
                tree.add(menu);
            }
        }
        return tree;
    }
}
