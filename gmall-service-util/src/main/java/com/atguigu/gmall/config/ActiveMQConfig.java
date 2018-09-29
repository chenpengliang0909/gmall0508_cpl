package com.atguigu.gmall.config;

import com.atguigu.gmall.util.ActiveMQUtil;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

import javax.jms.Session;

/**
 * @param
 * @return
 */

@Configuration
public class ActiveMQConfig {


    @Value("${spring.activemq.broker-url:disabled}")
    String brokerURL;

    @Value("${activemq.listener.enable:disabled}")
    String listenerEnable;



    @Bean
    public ActiveMQUtil getActiveMQUtil() {
       if("disabled".equals(brokerURL)){
            return null;
        }
        ActiveMQUtil activeMQUtil = new ActiveMQUtil();
        activeMQUtil.init(brokerURL);
        return activeMQUtil;
    }


    /***
     * 如下方法功能
     * ：将ActiveMq组件，转换为Spring的JMS的listener
     * @param activeMQConnectionFactory
     * @return
     */
    @Bean(name = "jmsQueueListener")  //将如下生成的factory的bean放入容器中，并定义了一个名称为jmsQueueListener
    public DefaultJmsListenerContainerFactory jmsQueueListenerContainerFactory(ActiveMQConnectionFactory activeMQConnectionFactory) {

        if("disabled".equals(listenerEnable)){
            return null;
        }

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(activeMQConnectionFactory);

        factory.setSessionTransacted(false);

        factory.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
        factory.setConcurrency("5");
        factory.setRecoveryInterval(5000L);

        return factory;

    }

    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory ( ){

        ActiveMQConnectionFactory activeMQConnectionFactory =
                new ActiveMQConnectionFactory(  brokerURL);
        return activeMQConnectionFactory;
    }

}