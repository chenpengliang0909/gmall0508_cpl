package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

/**
 * Created by Administrator on 2018/9/16.
 */
public interface CartService {
    CartInfo ifCartExits(CartInfo cartInfo);

    void updateCart(CartInfo cartInfoDB);

    void insertCart(CartInfo cartInfo);

    void flushCartCacheByUserId(String userId);

    List<CartInfo> getCartInfosFromCacheByUserId(String userId);

    List<CartInfo> getCartInfosFromDbByUserId(String userId);

    void updateCartIsChecked(CartInfo cartInfo);

    void combine(String userId, List<CartInfo> cartInfos) ;

    void deleteCartInfoAfterOrder(String cartInfoIdStrings,String userId);
}
