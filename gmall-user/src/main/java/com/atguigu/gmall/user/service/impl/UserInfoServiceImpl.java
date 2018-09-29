package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.constant.AppConstant;
import com.atguigu.gmall.util.RedisUtil;
import com.atguigu.gmall.service.UserInfoService;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    UserInfoMapper userInfoMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    UserAddressMapper userAddressMapper;



    @Override
    public List<UserInfo> getAll() {
        return  userInfoMapper.selectAll();

    }

    @Override
    public UserInfo getUserById(String id) {

        UserInfo userInfo = new UserInfo();
        userInfo.setId(id);
        return  userInfoMapper.selectByPrimaryKey(userInfo);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {

        UserInfo userResult = null;

        if(userInfo!= null ){

            userResult =  userInfoMapper.selectOne(userInfo);

            if(userResult != null ){
                //将用户信息保存到Redis中
                Jedis jedis = redisUtil.getJedis();

                String key = AppConstant.USER_KEY_PREFIX_REDIS+userResult.getId()+AppConstant.USER_KEY_SUFFIX_REDIS;
                String userJsonString = JSON.toJSONString(userResult); //将Java对象，转换为JSON字符串
                jedis.setex(key,AppConstant.EXPIRED_TIME,userJsonString); //将用户信息保存到Redis中，同时设置过期时间为一天

                jedis.close();
            }
        }

        return userResult;
    }


    @Override
    public List<UserAddress> getUserAddressByUserId(String userId) {


        UserAddress userAddressParam = new UserAddress();
        userAddressParam.setUserId(userId);

        return userAddressMapper.select(userAddressParam);
    }

    @Override
    public UserAddress getUserAddressById(String addressId) {

        UserAddress userAddress = new UserAddress();
        if(StringUtils.isNotBlank(addressId)){
            userAddress = userAddressMapper.selectByPrimaryKey(addressId);
        }
        return userAddress;
    }

}
