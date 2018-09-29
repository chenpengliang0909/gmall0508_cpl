package com.atguigu.gmall.payment.payMq;

import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

/**
 * Created by Administrator on 2018/9/26.
 */

/***
 * 延迟消息的消费端
 */
@Component
public class PaymentCheckQueueListener {


    @Autowired
    PaymentService paymentService;


    @JmsListener(destination = "PAYMENT_CHECK_QUEUE", containerFactory = "jmsQueueListener")
    public void consumeCheckResult(MapMessage mapMessage) throws JMSException {  //消息消费端

        String outTradeNo = mapMessage.getString("outTradeNo");
        int count = mapMessage.getInt("count");

        //获取用户的支付状态
        Map<String ,String> statusMap  =  paymentService.checkAlipayPayment(outTradeNo);

        /**
         * 根据用户的支付状态，执行不同业务
         * 如果为成功：则执行支付成功的业务
         * 如果为失败：则继续发送延迟消息，直至规定的次数结束
         */

        if("TRADE_SUCCESS".equals(statusMap.get("tradeStatus"))){ //说明用户已经支付成功了

            //验证幂等性原则
           boolean b =  paymentService.chechStatus(outTradeNo); //判断这个外部订单号对应支付状态是否为【已支付】

            //当paymentInfo的支付状态为【已支付】时，返回false

            if(!b){  //状态不为已支付

                //支付成功后，更新支付信息，以及向消息中间件发送消息  通知订单系统，更新订单信息
                PaymentInfo paymentInfo = new PaymentInfo();

                paymentInfo.setAlipayTradeNo(statusMap.get("alipayTradeNo"));
                paymentInfo.setPaymentStatus("已支付");
                paymentInfo.setCallbackTime(new Date());
                paymentInfo.setCallbackContent(statusMap.get("callback"));
                paymentInfo.setOutTradeNo(outTradeNo);

                System.err.println("进行第"+(6-count)+"检查订单的支付状态，已经支付，更新支付信息，以及向消息中间件发送消息");
                paymentService.updatePaymentSuccess(paymentInfo);

            }else{ //状态为已支付

                System.err.println("支付状态已经修改，延迟消息任务结束");

            }


        }else{ //说明用户没有支付成功

            //检测到用户还没有付款，
            if(count > 0 ){ //当循环次数大于0，继续发送延迟消息
                System.err.println("进行第"+(6-count)+"检查订单的支付状态,未支付，继续发送延迟消息");

                //消息生产端
                paymentService.sendDelayPaymentResult(outTradeNo,count-1); //每循环一次，循环次数减1

            }else{
                System.err.println("检查次数已达到上线，用户再规定时间内没有支付");
            }
        }
    }


}

