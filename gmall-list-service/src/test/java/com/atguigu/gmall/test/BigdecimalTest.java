package com.atguigu.gmall.test;

import org.junit.Test;

import java.math.BigDecimal;

/**
 * Created by Administrator on 2018/9/17.
 */
public class BigdecimalTest {

    @Test
    public void testBigDecimal(){

        BigDecimal b1 = new BigDecimal(0.01d);
        BigDecimal b2 = new BigDecimal(0.01f);

        System.out.println( "dubole:"+b1); //dubole:0.01000000000000000020816681711721685132943093776702880859375
        System.out.println( "float:"+b2); //float:0.00999999977648258209228515625

        int i = b1.compareTo(b2);
        b1.divide(b1,3,BigDecimal.ROUND_HALF_DOWN);

        b1.setScale(3,BigDecimal.ROUND_HALF_DOWN);

    }

}
