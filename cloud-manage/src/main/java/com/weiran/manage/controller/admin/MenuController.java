package com.weiran.manage.controller.admin;

import com.weiran.manage.entity.admin.PermissionMenuDTO;
import com.weiran.manage.response.ResultVO;
import com.weiran.manage.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/menu")
public class MenuController {


    private final AdminUserService adminUserService;

    /**
     * 获取管理员权限菜单
     */
    @GetMapping
    public ResultVO findByMenus(Principal principal) {
        List<PermissionMenuDTO> menus = adminUserService.findByMenus(principal.getName());
        return ResultVO.success(menus);
    }


}