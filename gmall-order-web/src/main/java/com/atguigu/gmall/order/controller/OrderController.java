package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.bean.enums.PaymentWay;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.UserInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2018/9/20.
 */
@Controller
public class OrderController {

    @Reference
    OrderService orderService;

    @Reference
    CartService cartService;

    @Reference
    UserInfoService userInfoService;

    @Reference
    SkuService skuService;

    /***
     * 提交订单
     * @param request
     * @param addressId
     * @param modelMap
     * @return
     */
    @LoginRequire(needSuccess = true) //说明访问本方法，必须登录
    @RequestMapping("submitOrder")
    public String submitOrder(HttpServletRequest request,
                              String tradeCode,
                              String addressId, ModelMap modelMap) {

        String userId = (String) request.getAttribute("userId"); //到这一步，userId肯定时是存在的

        boolean boo = orderService.checkTradeCode(userId,tradeCode); //校验交易码，判断是否重复提交订单

        if(boo){ //只有返回true，才能进行之后的操作

            List<CartInfo> cartInfoList = cartService.getCartInfosFromCacheByUserId(userId); //从缓存中或购物车数据

            BigDecimal totalAmount = getTotalPrice(cartInfoList);

            OrderInfo orderInfo = new OrderInfo();
            List<OrderDetail> orderDetailList = new ArrayList<>();

            //点击提交订单时，需要进行的操作
            //1、生成order订单、orderDetail

            UserAddress userAddress = userInfoService.getUserAddressById(addressId); //根据addressId获取收货地址
            if (userAddress!= null) {
                //封装orderInfo信息
                orderInfo.setConsignee(userAddress.getConsignee());
                orderInfo.setConsigneeTel(userAddress.getPhoneNum());
                orderInfo.setTotalAmount(totalAmount);
                orderInfo.setOrderStatus("未支付");
                orderInfo.setProcessStatus("订单提交");
                orderInfo.setUserId(userId);
                orderInfo.setPaymentWay(PaymentWay.ONLINE);

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE,1); //当前时间 加1天
                orderInfo.setExpireTime(calendar.getTime()); //失效时间，为当前时间加1天

                SimpleDateFormat sdf = new SimpleDateFormat("yyyymmddHHmmss");
                String format = sdf.format(new Date()); //将当前时间，按照指定格式，转换为字符串，如20180920

                String outTradeNo = "atguigugmall0508"+format+System.currentTimeMillis(); //包装订单交易编号的唯一性
                orderInfo.setOutTradeNo(outTradeNo);  //订单交易编号（第三方支付用)

                orderInfo.setDeliveryAddress(userAddress.getUserAddress()); //送货地址
                orderInfo.setOrderComment("硅谷快递");
                orderInfo.setCreateTime(new Date()); //创建时间

            }


            List<String> cartInfoIds = new ArrayList<>(); //用于存放需要删除购物车商品的id

            //封装orderDetail信息
            if (cartInfoList != null && cartInfoList.size() > 0) {
                for (CartInfo cartInfo : cartInfoList) {

                    if ("1".equals(cartInfo.getIsChecked())) {

                        OrderDetail orderDetail = new OrderDetail();
                        orderDetail.setSkuId(cartInfo.getSkuId());
                        orderDetail.setSkuNum(cartInfo.getSkuNum());
                        orderDetail.setSkuName(cartInfo.getSkuName());
                        orderDetail.setImgUrl(cartInfo.getImgUrl());

                        SkuInfo skuInfo = skuService.getSkuById(cartInfo.getSkuId());
                        //验价格
                        if(skuInfo.getPrice().compareTo(cartInfo.getSkuPrice()) == 0){ //价格相等才进行设置
                            orderDetail.setOrderPrice(cartInfo.getCartPrice());
                        }else{//价格不相等，则抛出异常

                            return "OrderErr";

                        }
                        //验库存
                        orderDetail.setHasStock("1");

                        orderDetailList.add(orderDetail);

                        String id = cartInfo.getId(); //获取购物车商品的id，用于删除

                        cartInfoIds.add(id);
                    }
                }
            }

            orderInfo.setOrderDetailList(orderDetailList);

            // 保存订单
            orderService.saveOrder(orderInfo);

            String cartInfoIdStrings = StringUtils.join(cartInfoIds, ","); //id集合，按照逗号，转换为字符串
            //2、删除购物车数据
            cartService.deleteCartInfoAfterOrder(cartInfoIdStrings,userId);


            //提交订单后，重定向到支付系统的收银台界面
            return "redirect:http://payment.gmall.com:8087/index?outTradeNo="+orderInfo.getOutTradeNo()+"&totalAmount="+totalAmount.toString();


        }else{
            return "OrderErr";  //当交易码校验失败时，返回错误界面
        }
    }

    /***
     * 去结算 方法
     * @param request
     * @param modelMap
     * @return
     */
    @LoginRequire(needSuccess = true) //说明访问本方法，必须登录
    @RequestMapping("toTrade")
    public String toTrade(HttpServletRequest request, ModelMap modelMap) {

        String userId = (String) request.getAttribute("userId"); //到这一步，userId肯定时是存在的

        //1、根据userId获取收货人信息
        List<UserAddress> userAddressList = new ArrayList<>();
        userAddressList = userInfoService.getUserAddressByUserId(userId);


        //2、获取用户勾选的商品，生成oderdetail
        List<OrderDetail> orderDetailList = new ArrayList<>();

        List<CartInfo> cartInfoList = cartService.getCartInfosFromCacheByUserId(userId); //从缓存中，获取用户购物车数据

        if (cartInfoList != null && cartInfoList.size() > 0) {

            for (CartInfo cartInfo : cartInfoList) {
                String isChecked = cartInfo.getIsChecked();
                if ("1".equals(isChecked)) {  //只去勾选上的
                    OrderDetail orderDetail = new OrderDetail();
                    orderDetail.setSkuId(cartInfo.getSkuId());
                    orderDetail.setSkuName(cartInfo.getSkuName());
                    orderDetail.setImgUrl(cartInfo.getImgUrl());
                    orderDetail.setOrderPrice(cartInfo.getCartPrice()); //为购买价格(下单时sku价格）
                    orderDetail.setSkuNum(cartInfo.getSkuNum());
                    orderDetailList.add(orderDetail);
                }
            }
        }

        //3、计算总金额
        BigDecimal totalAmount = getTotalPrice(cartInfoList);
        modelMap.put("userAddressList", userAddressList);
        modelMap.put("orderDetailList", orderDetailList);
        modelMap.put("totalAmount", totalAmount);


        //生成tradeCode方法
        String tradeCode = orderService.genTradeCode(userId);

        modelMap.put("tradeCode", tradeCode); //将交易码保存到订单界面

        return "trade";
    }

    /**
     * 计算勾选的商品的总价
     *
     * @param cartInfos
     * @return
     */
    private BigDecimal getTotalPrice(List<CartInfo> cartInfos) {

        BigDecimal totalAmount = new BigDecimal("0");

        if (cartInfos != null && cartInfos.size() > 0) {
            for (CartInfo cartInfo : cartInfos) {
                if ("1".equals(cartInfo.getIsChecked())) {
                    totalAmount = totalAmount.add(cartInfo.getCartPrice());
                }
            }
        }
        return totalAmount;
    }

}
