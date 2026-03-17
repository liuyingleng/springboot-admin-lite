package com.admin.service;

import com.admin.dto.RoleCreateDTO;
import com.admin.dto.RolePermissionDTO;
import com.admin.vo.PermissionTreeVO;
import com.admin.vo.RoleVO;

import java.util.List;

public interface IRoleService {

    List<RoleVO> getAll();

    void create(RoleCreateDTO dto);

    void update(Long id, RoleCreateDTO dto);

    void remove(Long id);

    void assignPermissions(Long id, RolePermissionDTO dto);

    List<PermissionTreeVO> getPermissionTree();
}
