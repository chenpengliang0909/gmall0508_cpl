package com.atguigu.gmall.order.orderMq;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Calendar;

/**
 * Created by Administrator on 2018/9/26.
 */

/***
 订单模块消费消息
 */
@Component
public class OrderPaymentSuccessQueueListener {


    @Autowired
    OrderService orderService;

    //其实可以看做一个监听器，监听消息中间件中的消息，当监听到与自己相关的时，就会进入如下的方法
    //然后就可以从监听的消息中获取内容，内容为生产消息时，添加的内容

    //特别注意需要添加如下注解  destination表示监听目标   containerFactory为容器中的一个JMS监听Bean，使用消息队列配置中的监听
    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE",containerFactory = "jmsQueueListener") //
    public void consumePaymentResult(MapMessage mapMessage) throws JMSException {

        String outTradeNo = mapMessage.getString("outTradeNo");
        String trackingNo = mapMessage.getString("trackingNo");


        //订单系统执行消息；即监听到支付成功后，开始执行订单系统的业务，并不是由支付系统直接调用订单系统
        OrderInfo orderInfoParam = new OrderInfo();
        orderInfoParam.setOutTradeNo(outTradeNo);
        orderInfoParam.setTrackingNo(trackingNo);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,3); //在当前时间上增加三天

        orderInfoParam.setExpectDeliveryTime(calendar.getTime()) ; //预计送达时间
        orderInfoParam.setOrderStatus("已支付");
        orderInfoParam.setProcessStatus("订单已支付");

        //订单状态、支付方式、预计送达时间、整体状态、支付宝交易码
        orderService.updateOrder(orderInfoParam);  //更新完成之后，Mybatis会将信息返回到orderInfoParam中：貌似不会


        //向消息中间件发送消息，给仓库系统使用
        orderService.sendOrderResultQueue(orderInfoParam);  //即 这个OrderInfo中时完整的信息

    }
}
