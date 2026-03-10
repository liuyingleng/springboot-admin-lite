package com.admin.service.impl;

import com.admin.common.RedisConstants;
import com.admin.entity.Permission;
import com.admin.entity.Role;
import com.admin.mapper.PermissionMapper;
import com.admin.mapper.RoleMapper;
import com.admin.service.IPermissionService;
import com.admin.utils.RedisUtil;
import com.admin.vo.MenuVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 */
@Service
public class PermissionServiceImpl implements IPermissionService {

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<String> getUserPermissions(Long userId) {
        // 先从 Redis 获取
        String key = RedisConstants.USER_PERMISSIONS_PREFIX + userId;
        Object cache = redisUtil.get(key);
        if (cache != null) {
            return (List<String>) cache;
        }

        // TODO: 从数据库查询用户权限
        // 这里暂时返回空列表，后续实现用户-角色-权限关联查询
        List<String> permissions = new ArrayList<>();
        permissions.add("system:user:view");
        permissions.add("system:user:add");

        // 存入 Redis
        redisUtil.set(key, permissions, RedisConstants.PERMISSION_EXPIRE_TIME);

        return permissions;
    }

    @Override
    public List<String> getUserRoles(Long userId) {
        // 先从 Redis 获取
        String key = RedisConstants.USER_ROLES_PREFIX + userId;
        Object cache = redisUtil.get(key);
        if (cache != null) {
            return (List<String>) cache;
        }

        // TODO: 从数据库查询用户角色
        // 这里暂时返回空列表，后续实现用户-角色关联查询
        List<String> roles = new ArrayList<>();
        roles.add("ROLE_ADMIN");

        // 存入 Redis
        redisUtil.set(key, roles, RedisConstants.PERMISSION_EXPIRE_TIME);

        return roles;
    }

    @Override
    public List<MenuVO> getUserMenuTree(Long userId) {
        // 先从 Redis 获取
        String key = RedisConstants.USER_MENU_PREFIX + userId;
        Object cache = redisUtil.get(key);
        if (cache != null) {
            return (List<MenuVO>) cache;
        }

        // TODO: 根据用户权限查询菜单
        // 这里暂时查询所有菜单，后续实现权限过滤
        List<Permission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .eq(Permission::getType, "menu")
                        .orderByAsc(Permission::getSort)
        );

        // 转换为 MenuVO
        List<MenuVO> menuList = permissions.stream().map(permission -> {
            MenuVO menuVO = new MenuVO();
            BeanUtils.copyProperties(permission, menuVO);
            return menuVO;
        }).collect(Collectors.toList());

        // 构建树形结构
        List<MenuVO> menuTree = buildMenuTree(menuList, 0L);

        // 存入 Redis
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
     * 构建菜单树
     */
    private List<MenuVO> buildMenuTree(List<MenuVO> menuList, Long parentId) {
        List<MenuVO> tree = new ArrayList<>();
        for (MenuVO menu : menuList) {
            if (menu.getParentId().equals(parentId)) {
                List<MenuVO> children = buildMenuTree(menuList, menu.getId());
                menu.setChildren(children);
                tree.add(menu);
            }
        }
        return tree;
    }
}
