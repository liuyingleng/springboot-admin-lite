package com.admin.controller;

import com.admin.common.Result;
import com.admin.dto.RoleCreateDTO;
import com.admin.dto.RolePermissionDTO;
import com.admin.service.IRoleService;
import com.admin.vo.PermissionTreeVO;
import com.admin.vo.RoleVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "角色管理")
@RestController
@RequestMapping("/api/system")
public class RoleController {

    @Autowired
    private IRoleService roleService;

    @ApiOperation("查询所有角色")
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('role:view')")
    public Result<List<RoleVO>> getAll() {
        return Result.success(roleService.getAll());
    }

    @ApiOperation("创建角色")
    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('role:create')")
    public Result<Void> create(@Valid @RequestBody RoleCreateDTO dto) {
        roleService.create(dto);
        return Result.success(null, "创建成功");
    }

    @ApiOperation("更新角色")
    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody RoleCreateDTO dto) {
        roleService.update(id, dto);
        return Result.success(null, "更新成功");
    }

    @ApiOperation("删除角色")
    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public Result<Void> remove(@PathVariable Long id) {
        roleService.remove(id);
        return Result.success(null, "删除成功");
    }

    @ApiOperation("分配权限")
    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('role:assign')")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody RolePermissionDTO dto) {
        roleService.assignPermissions(id, dto);
        return Result.success(null, "权限分配成功");
    }

    @ApiOperation("获取权限树")
    @GetMapping("/permissions/tree")
    @PreAuthorize("hasAuthority('role:view')")
    public Result<List<PermissionTreeVO>> getPermissionTree() {
        return Result.success(roleService.getPermissionTree());
    }
}
