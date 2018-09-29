package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 2018/9/20.
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    OrderInfoMapper orderInfoMapper;

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public void saveOrder(OrderInfo orderInfo) {

        orderInfoMapper.insertSelective(orderInfo);

        String orderInfoId = orderInfo.getId(); //插入后，返回orderInfo的主键

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        if (orderDetailList != null && orderDetailList.size() > 0) {
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setOrderId(orderInfoId);  //将orderInfoId设置进来
                orderDetailMapper.insertSelective(orderDetail);
            }
        }
    }

    @Override
    public String genTradeCode(String userId) {

        String tradeCode = UUID.randomUUID().toString(); //生成交易码

        Jedis jedis = redisUtil.getJedis();
        String key = "user:" + userId + ":tradeCode";  //保存交易码的 key
        jedis.setex(key, 1000 * 60 * 15, tradeCode); //将交易码保存在Redis缓存中，过期时间为15分钟

        jedis.close(); //使用jedis时，需要及时关闭
        return tradeCode;
    }

    /***
     * 校验交易码
     * @param userId
     * @param tradeCodePage
     * @return
     */
    @Override
    public boolean checkTradeCode(String userId, String tradeCodePage) {

        boolean boo = false;

        //从Redis中取出交易码
        Jedis jedis = redisUtil.getJedis();
        String key = "user:" + userId + ":tradeCode";  //交易码的 key
        String tradeCodeCache = jedis.get(key);

        if (StringUtils.isNotBlank(tradeCodeCache) && tradeCodeCache.equals(tradeCodePage)) { //比较界面和Redis中的交易码
            boo =  true;    //相同，则返回true

            jedis.del(key); //交易码验证完成之后，需要将Redis中的删除，这样就能保证，同一时间，用户只能提交一个订单了
        }
        jedis.close();

        return boo;
    }

    @Override
    public OrderInfo getOrderInfoByOutTradeNo(String outTradeNo) {

        OrderInfo orderInfoParam = new OrderInfo();
        orderInfoParam.setOutTradeNo(outTradeNo);

        OrderInfo orderInfo = orderInfoMapper.selectOne(orderInfoParam);


        //根据orderId获取对应的orderDetail信息d
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderInfo.getId());
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);

        orderInfo.setOrderDetailList(orderDetailList);

        return orderInfo;
    }


    /***
     * 支付成功之后，更新订单信息
     * @param orderInfoParam
     */
    @Override
    public void updateOrder(OrderInfo orderInfoParam) {

        Example example = new Example(OrderInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",orderInfoParam.getOutTradeNo());

        orderInfoMapper.updateByExampleSelective(orderInfoParam,example);

    }

    /***
     * 更新订单信息之后，向消息中间件发送消息，给仓库系统使用
     * @param orderInfoParam
     */
    @Override
    public void sendOrderResultQueue(OrderInfo orderInfoParam) {


        Connection connection = activeMQUtil.getConnection(); //获取连接

        try {
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0 //并且是持久化
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);  //开启消息的事物模式

            //消息对象
            Queue testqueue = session.createQueue("ORDER_RESULT_QUEUE");  //更新订单信息之后，向消息中间件发送消息，给仓库系统使用
            MessageProducer producer = session.createProducer(testqueue);

            //消息内容
            TextMessage textMessage=new ActiveMQTextMessage(); //使用MapMessage就可以发送key-value的键值对 TextMessage只能发送字符串
            textMessage.setText(JSON.toJSONString(orderInfoParam)); //消息中携带订单信息
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);  //持久化模式

            //发出消息
            producer.send(textMessage);
            session.commit();     //消息也是有事物的 ：服务器上的消息，一个消费执行时出现了异常，消息的消费会回滚；在消息模式下，才有分布式事物；不是在db层面的
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
