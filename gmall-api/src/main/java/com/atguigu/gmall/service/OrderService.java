package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;

/**
 * Created by Administrator on 2018/9/20.
 */
public interface OrderService {
    void saveOrder(OrderInfo orderInfo);

    String genTradeCode(String userId);

    boolean checkTradeCode(String userId, String tradeCode);

    OrderInfo getOrderInfoByOutTradeNo(String outTradeNo);

    void updateOrder(OrderInfo orderInfoParam);

    void sendOrderResultQueue(OrderInfo orderInfoParam);
}
