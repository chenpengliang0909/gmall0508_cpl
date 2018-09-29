package com.atguigu.gmall.payment.testMQ;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

/**
 * Created by Administrator on 2018/9/25.
 */

//队列模式消息的生产者
public class BossQueueProducer {

    public static void main(String[] args) {

        //如下过程，有点类似Mybatis的使用  SQLSessionFactory  SQLSession Mapper

        ConnectionFactory connect = new ActiveMQConnectionFactory("tcp://192.168.0.42:61616");
        try {
            Connection connection = connect.createConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0 //并且是持久化
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);  //开启消息的事物模式

            //消息对象
            Queue testqueue = session.createQueue("HESHUI");  //必须给每个消息定义一个名称
            MessageProducer producer = session.createProducer(testqueue);

            //消息内容
            TextMessage textMessage=new ActiveMQTextMessage();
            textMessage.setText("我渴了，需要喝水！");
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

