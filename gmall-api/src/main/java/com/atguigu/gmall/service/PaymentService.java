package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

/**
 * Created by Administrator on 2018/9/25.
 */
public interface PaymentService {

    void savePayment(PaymentInfo paymentInfo);

    void updateOrderId(PaymentInfo paymentInfo);

    void sendPaymentSuccessQueue(String outTradeNo,String alipayTradeNo);

    void sendDelayPaymentResult(String outTradeNo, int i);

    Map<String ,String> checkAlipayPayment(String outTradeNo);

    void updatePaymentSuccess(PaymentInfo paymentInfo);

    boolean chechStatus(String outTradeNo);
}
