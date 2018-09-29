package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.conf.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Map;

/**
 * Created by Administrator on 2018/9/24.
 */
@Controller
public class PaymentController {


    @Autowired
    AlipayClient alipayClient;

    @Reference
    OrderService orderService;

    @Autowired
    PaymentService paymentService;

    @LoginRequire(needSuccess = true)
    @RequestMapping("index")
    public String index(HttpServletRequest request, String outTradeNo, String totalAmount, ModelMap map) {

        map.put("outTradeNo", outTradeNo);
        map.put("totalAmount", totalAmount);
        return "index";
    }

    /***
     * 选择支付方式之后，点击立即支付
     * @param outTradeNo
     * @return
     */
    @LoginRequire(needSuccess = true)
    @RequestMapping("alipay/submit")
    @ResponseBody
    public String alipay(String outTradeNo) {

        //根据outTradeNo来获取对应的orderInfo信息

        OrderInfo orderInfo = orderService.getOrderInfoByOutTradeNo(outTradeNo);

        //设置 支付宝page.pay的请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request

        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址

        Map<String, Object> map = new HashedMap();
        map.put("out_trade_no", outTradeNo);
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");
        //map.put("total_amount",orderInfo.getTotalAmount());
        map.put("total_amount", 0.01);
        map.put("subject", orderInfo.getOrderDetailList().get(0).getSkuName()); //第一个商品的名称
        map.put("body", "硅谷支付测试");

        String s = JSON.toJSONString(map);

        alipayRequest.setBizContent(s);//填充业务参数
        String form = "";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
//        httpResponse.setContentType("text/html;charset=" + CHARSET);
//        httpResponse.getWriter().write(form);//直接将完整的表单html输出到页面
//        httpResponse.getWriter().flush();
//        httpResponse.getWriter().close();
        System.out.println(form);

        //保存交易信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        paymentInfo.setPaymentStatus("未支付");
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setSubject(orderInfo.getOrderDetailList().get(0).getSkuName());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setOrderId(orderInfo.getId());

        paymentService.savePayment(paymentInfo);


        //用户选择支付方式之后，点击立即支付，同时发送延迟消息
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),5); //5代表最多会循环5次

        return form;
    }

    /***
     * 支付成功之后，的回调方法
     * @param request
     * @return
     */
    @RequestMapping("alipay/callback/return")
    public String alipayReturn(HttpServletRequest request) {

        //回调首先需要验证ali的签名
        boolean signVerified = false; //调用SDK验证签名

        try {
            signVerified = AlipaySignature.rsaCheckV1(null, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);

            if (signVerified) {
                // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            } else {
                // TODO 验签失败则记录异常日志，并在response中返回failure.
            }

        } catch (Exception e) {
            System.out.println("支付系统的验签");
        }

        //签名验证通过后，继续执行支付成功的业务
        String alipayTradeNo = (String) request.getParameter("trade_no");
        String callback = request.getQueryString();
        String outTradeNo = (String) request.getParameter("out_trade_no");

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setAlipayTradeNo(alipayTradeNo);
        paymentInfo.setPaymentStatus("已支付");
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(callback);
        paymentInfo.setOutTradeNo(outTradeNo);

        //支付成功后，更新支付信息，以及向消息中间件发送消息  通知订单系统，更新订单信息
        paymentService.updatePaymentSuccess(paymentInfo);

        return "finish";

    }

}
