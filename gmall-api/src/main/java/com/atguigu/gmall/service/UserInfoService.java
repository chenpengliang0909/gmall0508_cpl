package com.atguigu.gmall.service;


import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserInfoService {

    public List<UserInfo> getAll();

    UserInfo getUserById(String id);

    UserInfo login(UserInfo userInfo);

    List<UserAddress> getUserAddressByUserId(String userId);

    UserAddress getUserAddressById(String addressId);
}
