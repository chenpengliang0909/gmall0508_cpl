package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.constant.AppConstant;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/9/15.
 */
@Controller
public class CartController {

    @Reference
    CartService cartService;

    @Reference
    SkuService skuService;

    /**
     * 购物车列表界面，每次勾选和取消勾选商品时，会发送的请求
     * @return
     */
    @LoginRequire(needSuccess=false) //说明访问本方法，可以不登录
    @RequestMapping("checkCart")
    public String checkCart(CartInfo cartInfo,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            ModelMap map){
        // 声明一个处理后的购物车集合对象
        List<CartInfo> cartInfos =  new ArrayList<>();
        String skuId = cartInfo.getSkuId();
        String isChecked = cartInfo.getIsChecked();

        String userId = (String) request.getAttribute("userId");//从请求中获取用户的id，这个id是在拦截器中放入的 "1"

        //判断用户是否登录
        if(StringUtils.isNotBlank(userId)){ //登录了，操作数据库

            cartInfo.setUserId(userId); // 将用户id设置进来

            cartService.updateCartIsChecked(cartInfo); //更新勾选信息

            cartInfos = cartService.getCartInfosFromCacheByUserId(userId); //根据Userid获取所有的购物车商品

        }else{ //没有登录，操作cookies

            String cookieName = "listCartCookie";  //cookies的名称
            String cookieValue = CookieUtil.getCookieValue(request, cookieName, true);

            if(StringUtils.isNotBlank(cookieValue)){
                cartInfos = JSON.parseArray(cookieValue, CartInfo.class);

                for (CartInfo info : cartInfos) {
                    if(info.getSkuId().equals(skuId)){
                        info.setIsChecked(isChecked);
                    }
                }
            }

            //更新Cookies
            CookieUtil.setCookie(request,response,cookieName,JSON.toJSONString(cartInfos),1000*60*60*24,true);
        }

        //计算勾选的商品的总价
        BigDecimal totalPrice =  getTotalPrice(cartInfos);

        map.put("totalPrice",totalPrice);
        map.put("cartList",cartInfos);
        return "cartListInner";
    }

    /**
     * 去购物车列表界面
     * @return
     */
    @LoginRequire(needSuccess=false) //说明访问本方法，可以不登录
    @RequestMapping("cartList")
    public String cartList(HttpServletRequest request, ModelMap map){

        // 声明一个处理后的购物车集合对象
        List<CartInfo> cartInfos =  new ArrayList<>();

        //判断用户是否登录
        String userId = (String) request.getAttribute("userId");//从请求中获取用户的id，这个id是在拦截器中放入的 "1"

        if(StringUtils.isNotBlank(userId)){ //说明登录了

            //从Redis中取出数据
            cartInfos =  cartService.getCartInfosFromCacheByUserId(userId);
            if(!(cartInfos==null && cartInfos.size() > 0)){ //说明从Redis中没有获取到
                //于是从数据库中取
                cartInfos =  cartService.getCartInfosFromDbByUserId(userId);
            }

        }else{ //说明没有登录

            String cookieName = "listCartCookie";  //cookies的名称
            //从cookies中取出数据
            String cookieValue = CookieUtil.getCookieValue(request, cookieName, true);
           if(StringUtils.isNotBlank(cookieValue)){
               cartInfos = JSON.parseArray(cookieValue, CartInfo.class);//将cookies中购物车数据，转换为购物车集合
           }
        }

        //将获取到的购物车数据，放在request域中
        map.put("cartList",cartInfos);

        //计算勾选的商品的总价
       BigDecimal totalPrice =  getTotalPrice(cartInfos);


        map.put("totalPrice",totalPrice);
        return "cartList";
    }

    /**
     * 计算勾选的商品的总价
     * @param cartInfos
     * @return
     */

    private BigDecimal getTotalPrice(List<CartInfo> cartInfos) {
        BigDecimal totalPrice = new BigDecimal("0");
        if(cartInfos!= null && cartInfos.size()>0){
            for (CartInfo cartInfo : cartInfos) {
                //当购物车商品为勾选时，才计算商品的总价
                if("1".equals(cartInfo.getIsChecked())){
                    totalPrice =  totalPrice.add(cartInfo.getCartPrice());
                }
            }
        }
        return  totalPrice;
    }

    @LoginRequire(needSuccess=false) //说明访问本方法，可以不登录
    @RequestMapping("addToCart")
    public String addToCart(HttpServletRequest request,
                            HttpServletResponse response,
                            @RequestParam Map<String,String> map){


        // 声明一个处理后的购物车集合对象，用于放入cookie中
        List<CartInfo> cartInfos =  new ArrayList<>();

        //当点击加入购物车时，传递的参数为【num】【skuId】
        Integer num =Integer.parseInt(map.get("num"));
        String skuId = map.get("skuId");


        //根据skuId，查询对应商品skuInfo 信息
        SkuInfo skuInfo = skuService.getSkuById(skuId);

        //将skuInfo详细信息，封装购物车对象中
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuInfo.getId());
        cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
        cartInfo.setSkuName(skuInfo.getSkuName());
        cartInfo.setSkuNum(num);
        cartInfo.setIsChecked("1");
        cartInfo.setSkuPrice(skuInfo.getPrice());

        //设置放入购物车时的价格 = sku商品的价格 * sku商品的数量
        cartInfo.setCartPrice(cartInfo.getSkuPrice().multiply(new BigDecimal(cartInfo.getSkuNum())));

        //判断用户是否登录
        String userId = (String) request.getAttribute("userId");//从请求中获取用户的id，这个id是在拦截器中放入的 "1"

        //说明用户没有登录  操作cookies
        if(StringUtils.isBlank(userId)){
            cartInfo.setUserId("");  //cookie没有用户id


            //根据cookie的名称，从cookie中取出购物车数据
            String cartFromCookie = CookieUtil.getCookieValue(request, AppConstant.COOKIE_NAME_CART, true);

            //判断是否有购物车数据

            if(StringUtils.isBlank( cartFromCookie )){ //没有

                cartInfos.add(cartInfo);  // 将商品，添加到空集合中，用于放入cookie中
            }else{
                // 有数据，将cookie中的数据转换为购物车集合
                cartInfos = JSON.parseArray(cartFromCookie, CartInfo.class);

                //判断更新还是插入购物车数据
                boolean isNewCartInfo = if_new_cartInfo(cartInfos,cartInfo);
                if( isNewCartInfo ){ //说明 新增
                    cartInfos.add( cartInfo ); // 将商品，添加到集合中
                }else{  //说明更新
                    for (CartInfo info : cartInfos) {
                        if(info.getSkuId().equals(cartInfo.getSkuId()) ){  //在cookie中找到该商品
                            //修改商品数量，和商品总价格
                            info.setSkuNum( info.getSkuNum() + cartInfo.getSkuNum());
                            info.setCartPrice( info.getSkuPrice().multiply(new BigDecimal(info.getSkuNum())));
                        }
                    }
                }
            }

            //将cookie保存至浏览器：一JSON字符串的形式
            // 同时设置cookies的超时时间为 一天
            CookieUtil.setCookie(request,response,AppConstant.COOKIE_NAME_CART,JSON.toJSONString(cartInfos),AppConstant.EXPIRED_TIME,true);


        }else{
            //说明用户登录了  操作数据库
            cartInfo.setUserId(userId);  //设置用户id

            //根据将要添加的商品，判断在数据库中是否存在
            CartInfo cartInfoDB = cartService.ifCartExits(cartInfo);
            if(cartInfoDB!= null){ //说明已经存在,则进行更新操作

                cartInfoDB.setSkuNum(cartInfoDB.getSkuNum()+cartInfo.getSkuNum()); //更新商品数量  和 商品总价
                cartInfoDB.setCartPrice(cartInfoDB.getSkuPrice().multiply(new BigDecimal(cartInfoDB.getSkuNum())));

                cartService.updateCart(cartInfoDB);

            }else{ //说明不存在，则进行插入操作

                cartService.insertCart(cartInfo);
            }

            /**
             数据库操作完成后，同步到redis中，思路为：
             1、根据userId从数据库中取出购物车数据；
             2、将数据保存在redis中：以hash的数据结构
             */
            cartService.flushCartCacheByUserId(userId);
        }
        return "redirect:/cartSuccess";
    }

    //判断 更新还是插入 购物车数据
    private boolean if_new_cartInfo(List<CartInfo> cartInfos, CartInfo cartInfo) {

        for (CartInfo info : cartInfos) {

            //如果购物车中有商品的skuId  与 将要添加的商品的skuId一致，则返回false 说明是更新
            if( info.getSkuId().equals( cartInfo.getSkuId() )){
                return false;
            }
        }
        return true; //说明是新增

    }

    @RequestMapping("cartSuccess")
    public String cartSuccess(){

        return "success";
    }


}
