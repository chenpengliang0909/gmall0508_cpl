package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserAddressService;
import com.atguigu.gmall.service.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.dubbo.config.annotation.Reference;
import java.util.List;

@RestController
public class UserController {

    @Reference
    private UserInfoService userInfoService;

    Logger logger = LoggerFactory.getLogger(UserController.class);

    @Reference
    private UserAddressService userAddressService;

    //操作user_address表
    @RequestMapping("/userAddressList")
    public List<UserAddress> getUserAddressList(){

        List<UserAddress> list = userAddressService.getUserAddressList();

        return list;
    }

    //操作user_info 表
    @RequestMapping("index")
    public List<UserInfo> index(){

        logger.debug("测试一下");

        List<UserInfo> list = userInfoService.getAll();

        return list;
    }


    @RequestMapping("/get")
    public UserInfo getUserById(){

        logger.debug("测试一下");

        String id ="1";

       UserInfo userInfo =  userInfoService.getUserById(id);

        return userInfo;
    }
}
