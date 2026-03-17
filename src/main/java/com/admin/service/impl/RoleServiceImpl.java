package com.admin.service.impl;

import com.admin.dto.RoleCreateDTO;
import com.admin.dto.RolePermissionDTO;
import com.admin.entity.Permission;
import com.admin.entity.Role;
import com.admin.entity.RolePermission;
import com.admin.entity.UserRole;
import com.admin.mapper.PermissionMapper;
import com.admin.mapper.RoleMapper;
import com.admin.mapper.RolePermissionMapper;
import com.admin.mapper.UserRoleMapper;
import com.admin.service.IPermissionService;
import com.admin.service.IRoleService;
import com.admin.vo.PermissionTreeVO;
import com.admin.vo.RoleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements IRoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private IPermissionService permissionService;

    @Override
    public List<RoleVO> getAll() {
        List<Role> roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>().orderByAsc(Role::getId)
        );
        return roles.stream().map(role -> {
            RoleVO vo = new RoleVO();
            BeanUtils.copyProperties(role, vo);
            // 查询角色拥有的权限 ID
            List<Long> permIds = rolePermissionMapper.selectList(
                    new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getId())
            ).stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
            vo.setPermissionIds(permIds);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void create(RoleCreateDTO dto) {
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, dto.getRoleCode())
        );
        if (count > 0) throw new RuntimeException("角色编码已存在");

        Role role = new Role();
        BeanUtils.copyProperties(dto, role);
        roleMapper.insert(role);

        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            assignPermissionIds(role.getId(), dto.getPermissionIds());
        }
    }

    @Override
    @Transactional
    public void update(Long id, RoleCreateDTO dto) {
        Role role = roleMapper.selectById(id);
        if (role == null) throw new RuntimeException("角色不存在");

        BeanUtils.copyProperties(dto, role, "permissionIds");
        roleMapper.updateById(role);

        if (dto.getPermissionIds() != null) {
            rolePermissionMapper.delete(
                    new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id)
            );
            if (!dto.getPermissionIds().isEmpty()) {
                assignPermissionIds(id, dto.getPermissionIds());
            }
        }

        // 清除该角色下所有用户的权限缓存
        clearRoleUsersCache(id);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id)
        );
        clearRoleUsersCache(id);
    }

    @Override
    @Transactional
    public void assignPermissions(Long id, RolePermissionDTO dto) {
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id)
        );
        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            assignPermissionIds(id, dto.getPermissionIds());
        }
        clearRoleUsersCache(id);
    }

    @Override
    public List<PermissionTreeVO> getPermissionTree() {
        List<Permission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSort)
        );
        List<PermissionTreeVO> list = all.stream().map(p -> {
            PermissionTreeVO vo = new PermissionTreeVO();
            BeanUtils.copyProperties(p, vo);
            return vo;
        }).collect(Collectors.toList());
        return buildTree(list, 0L);
    }

    private void assignPermissionIds(Long roleId, List<Long> permissionIds) {
        permissionIds.forEach(permId -> {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permId);
            rolePermissionMapper.insert(rp);
        });
    }

    private void clearRoleUsersCache(Long roleId) {
        userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId)
        ).forEach(ur -> permissionService.clearUserPermissionCache(ur.getUserId()));
    }

    private List<PermissionTreeVO> buildTree(List<PermissionTreeVO> list, Long parentId) {
        List<PermissionTreeVO> tree = new ArrayList<>();
        for (PermissionTreeVO node : list) {
            Long pid = node.getParentId() == null ? 0L : node.getParentId();
            if (pid.equals(parentId)) {
                List<PermissionTreeVO> children = buildTree(list, node.getId());
                node.setChildren(children.isEmpty() ? null : children);
                tree.add(node);
            }
        }
        return tree;
    }
}
