package com.admin.controller;

import com.admin.common.Result;
import com.admin.dto.UserCreateDTO;
import com.admin.dto.UserPageDTO;
import com.admin.dto.UserUpdateDTO;
import com.admin.service.IUserService;
import com.admin.vo.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Api(tags = "用户管理")
@RestController
@RequestMapping("/api/system/users")
public class UserController {

    @Autowired
    private IUserService userService;

    @ApiOperation("分页查询用户")
    @GetMapping
    @PreAuthorize("hasAuthority('user:view')")
    public Result<Page<UserVO>> getPage(UserPageDTO dto) {
        return Result.success(userService.getPage(dto));
    }

    @ApiOperation("创建用户")
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public Result<Void> create(@Valid @RequestBody UserCreateDTO dto) {
        userService.create(dto);
        return Result.success(null, "创建成功");
    }

    @ApiOperation("更新用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        userService.update(id, dto);
        return Result.success(null, "更新成功");
    }

    @ApiOperation("删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public Result<Void> remove(@PathVariable Long id) {
        userService.remove(id);
        return Result.success(null, "删除成功");
    }

    @ApiOperation("修改用户状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user:edit')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.updateStatus(id, body.get("status"));
        return Result.success(null, "状态更新成功");
    }

    @ApiOperation("重置密码")
    @PutMapping("/{id}/password/reset")
    @PreAuthorize("hasAuthority('user:edit')")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success(null, "密码已重置为 Admin@123");
    }
}
