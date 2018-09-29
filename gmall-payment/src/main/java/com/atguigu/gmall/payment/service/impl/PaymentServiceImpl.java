package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2018/9/25.
 */
@Service
public class PaymentServiceImpl implements PaymentService {


    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;


    @Override
    public void savePayment(PaymentInfo paymentInfo) {

        paymentInfoMapper.insertSelective(paymentInfo);

    }

    @Override
    public void updateOrderId(PaymentInfo paymentInfo) {

        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo", paymentInfo.getOutTradeNo());
        paymentInfoMapper.updateByExampleSelective(paymentInfo, example);
    }


    /***
     * 支付成功之后，向消息队列发送消息，给订单系统进行消费
     * @param outTradeNo
     */
    @Override
    public void sendPaymentSuccessQueue(String outTradeNo, String alipayTradeNo) {

        Connection connection = activeMQUtil.getConnection(); //获取连接

        // ConnectionFactory connect = new ActiveMQConnectionFactory("tcp://192.168.0.42:61616");
        try {
            //Connection connection = connect.createConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0 //并且是持久化
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);  //开启消息的事物模式

            //消息对象
            Queue testqueue = session.createQueue("PAYMENT_SUCCESS_QUEUE");  //支付成功后，给订单系统使用的消息
            MessageProducer producer = session.createProducer(testqueue);

            //消息内容
            MapMessage mapMessage = new ActiveMQMapMessage(); //使用MapMessage就可以发送key-value的键值对 TextMessage只能发送字符串
            mapMessage.setString("outTradeNo", outTradeNo);  //消息中携带外部订单号
            mapMessage.setString("trackingNo", alipayTradeNo);  //消息中携带支付宝交易号
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);  //持久化模式

            //发出消息
            producer.send(mapMessage);
            session.commit();     //消息也是有事物的 ：服务器上的消息，一个消费执行时出现了异常，消息的消费会回滚；在消息模式下，才有分布式事物；不是在db层面的
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    /***
     * 条用支付宝的 【alipay.trade.query(统一收单线下交易查询)】接口，查询支付宝的支付状态
     * @param outTradeNo
     * @return
     */
    public Map<String, String> checkAlipayPayment(String outTradeNo) {

        Map<String, String> returnMap = new HashMap<>();

        //检查支付宝支付状态
        System.out.println("开始检查支付宝的支付状态....，返回支付结果");

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

        Map<String, String> map = new HashMap<>(); //用于封装请求参数

        map.put("out_trade_no", outTradeNo);
        request.setBizContent(JSON.toJSONString(map));

        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);  //执行查询请求，得到返回结果
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if (response.isSuccess()) { //如果response有值，说明支付成功了

            System.out.println("调用成功");
            String tradeStatus = response.getTradeStatus();  //获取支付宝支付状态

            if (StringUtils.isNotBlank(tradeStatus)) { //如果交易状态有值，则返回成功的状态

                returnMap.put("tradeStatus", tradeStatus);
                returnMap.put("alipayTradeNo", response.getTradeNo());
                returnMap.put("callback", response.getMsg());

                return returnMap;

            } else {

                returnMap.put("tradeStatus", "fail");
                return returnMap; //如果交易状态为空，返回失败
            }

        } else { //说明用户还没有支付，返回失败

            System.out.println("用户未创建交易");
            returnMap.put("tradeStatus", "fail");
            return returnMap; //如果交易状态为空，返回失败
        }
    }

    @Override
    public void updatePaymentSuccess(PaymentInfo paymentInfo) {

        //更新支付信息
        updateOrderId(paymentInfo);

        //向消息中间件发送消息  通知订单系统，更新订单信息
        sendPaymentSuccessQueue(paymentInfo.getOutTradeNo(), paymentInfo.getAlipayTradeNo());

    }

    /***
     * 根据外部订单号，查询对应的paymentInfo的支付状态是否为【已支付】
     * @param outTradeNo
     * @return
     */
    @Override
    public boolean chechStatus(String outTradeNo) {

        boolean b = false;
        PaymentInfo paymentInfoParam = new PaymentInfo();
        paymentInfoParam.setOutTradeNo(outTradeNo);

        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoParam);
        if(paymentInfo!= null){

            String paymentStatus = paymentInfo.getPaymentStatus();

            if("已支付".equals(paymentStatus)){ //说明状态为已支付，返回true
                b = true;
            }
        }
        return b;
    }

    /***
     * 选择支付方式，点击立即支付时，会发送延迟队列，检查用户的支付结果
     * @param outTradeNo
     * @param count
     */
    @Override
    public void sendDelayPaymentResult(String outTradeNo, int count) {


        System.err.println("开始向消息中间件发送检查支付状态的 延迟消息");

        Connection connection = activeMQUtil.getConnection(); //获取连接
        try {
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0 //并且是持久化
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);  //开启消息的事物模式

            //消息对象
            Queue testqueue = session.createQueue("PAYMENT_CHECK_QUEUE");  //支付成功后，给订单系统使用的消息
            MessageProducer producer = session.createProducer(testqueue);

            //消息内容
            MapMessage mapMessage = new ActiveMQMapMessage(); //使用MapMessage就可以发送key-value的键值对 TextMessage只能发送字符串
            mapMessage.setString("outTradeNo", outTradeNo);  //消息中携带外部订单号
            mapMessage.setInt("count", count);    //消息中携带循环发送延迟消息的次数
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);  //持久化模式

            //设置发送的为延迟消息
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, 1000 * 10); //延迟时间为10S


            //发出消息
            producer.send(mapMessage);
            session.commit();     //消息也是有事物的 ：服务器上的消息，一个消费执行时出现了异常，消息的消费会回滚；在消息模式下，才有分布式事物；不是在db层面的
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }


    }
}
