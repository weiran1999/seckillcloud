package com.weiran.uaa.controller;

import com.weiran.common.obj.Result;
import com.weiran.uaa.param.LoginParam;
import com.weiran.uaa.param.RegisterParam;
import com.weiran.uaa.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 登陆控制器
 */
@Controller
@RequiredArgsConstructor
@Api("登陆控制器")
public class UserController {

    final UserService userService;

    @PostMapping("user/doLogin")
    @ResponseBody
    @ApiOperation("登陆，信息写进redis")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "登陆传递字段")
    })
    public Result doLogin(@RequestBody LoginParam loginParam) {
        return userService.doLogin(loginParam);
    }

    @PostMapping("user/doRegister")
    @ResponseBody
    @ApiOperation("注册")
    @ApiImplicitParam(value = "注册传递字段")
    public Result doRegister(RegisterParam registerParam) {
        return userService.doRegister(registerParam);
    }

    @RequestMapping("user/logout")
    @ResponseBody
    public Result doLogout(HttpServletRequest request) {
        return userService.doLogout(request);
    }

    @ApiOperation("后台查询本次成功参与活动的用户信息")
    @RequestMapping(value = "/user/inquiryUser")
    @ResponseBody
    public Result inquiryUser(@RequestParam("userId") long userId) {
        return userService.inquiryUser(userId);
    }
}