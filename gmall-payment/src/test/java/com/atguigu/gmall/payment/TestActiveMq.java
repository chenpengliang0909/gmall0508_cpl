package com.atguigu.gmall.payment;

import com.atguigu.gmall.util.ActiveMQUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jms.Connection;

/**
 * Created by Administrator on 2018/9/26.
 */
public class TestActiveMq {

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Test
    public void  test1(){

        Connection connection = activeMQUtil.getConnection();
        System.err.println(connection);
    }
}
