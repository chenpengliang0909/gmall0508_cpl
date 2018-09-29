package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.constant.AppConstant;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.UserInfoService;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2018/9/18.
 */
@Controller
public class PassportController {

    @Reference
    UserInfoService userInfoService;

    @Reference
    CartService cartService;

    /***
     *
     进入登录界面
     */
    @RequestMapping("index")
    public String index(String returnUrl, ModelMap map) {

        map.put("originUrl", returnUrl);

        return "index";
    }

    /***
     * 用户登录方法
     * @param model
     * @return
     */
    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo, HttpServletRequest request,
                        HttpServletResponse response, ModelMap model) {

        //验证用户名和密码，
        UserInfo userResult = userInfoService.login(userInfo);
        if (userResult != null) { //说明根据用户输入的用户名和密码，查到了用户，登录成功

            Map<String, String> map = new HashMap<>();
            map.put("userId", userResult.getId());
            map.put("nickName", userResult.getNickName());

            String ip = getMyIpFromRequest(request); //盐值，使用为：客户端机器的IP地址

            //1、验证通过后，生成token
            String token = JwtUtil.encode(AppConstant.TOKEN_KEY, map, ip);

            //2、将用户的信息，保存在redis中，并设置过期时间，在UserInfoServiceImpl中实现

            //合并购物车
            String cartJson = CookieUtil.getCookieValue(request, AppConstant.COOKIE_NAME_CART, true);

            if(StringUtils.isNotBlank(cartJson)){ //只有当cookie中有购物车数据时，才合并
                //合并购物车方法
                cartService.combine(userResult.getId(), JSON.parseArray(cartJson, CartInfo.class));

                CookieUtil.deleteCookie(request,response,AppConstant.COOKIE_NAME_CART); //合并之后，删除Cookie
            }

            return token; //返回生成的token给界面，认证中心不负责界面的跳转

        } else {  //当没有查到用户时，返回一个错误信息

            return "err";

        }
    }

    /***
     * 验证token是否正确，这个request请求来自拦截器，拦截器在web-util，并不是客户端发送的请求
     * @param request
     * @param model
     * @return
     */
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request,String token,String clientIp,ModelMap model) {

        //1、验证token的真伪

        //解码token，能解出来，说明token是正确的
        try {
            Map userMap = JwtUtil.decode(AppConstant.TOKEN_KEY, token, clientIp);

            if(userMap != null){
                return "success";
            }else{
                return "fail";
            }

        } catch (Exception e) { //如果抛出异常，说明解码失败了

            return "fail";
        }

        //2、验证token对应的用户信息的过期时间，增加严谨性



        /**
         * 关于使用【clientIp】的理解
         * ip(盐值)，是用来解码token，既然是解码，那么是需要与编码token时，是一致的；
         但是编码token时，使用客户端的ip，那么这里最终也是需要客户端的ip，
         而客户端的ip是需要通过用户发送的request来进行获取；
         但是【verify】中的request并不是用户发送的request，
         因此没有办法使用【getMyIpFromRequest】来获取客户机的ip，
         需要在拦截器中，调用本方法时，直接将客户端的ip，直接传递过来；

         */
    }



    /***
     * 远程连接中获取客户端机器的IP地址
     * @param request
     * @return
     */
    public String getMyIpFromRequest(HttpServletRequest request) {

        String ip = request.getRemoteAddr(); //能获取到，只不过是获取了代理服务器的ip地址 //127.0.0.1

        //System.out.println("getRemoteAddr方法获取:"+ip ); //127.0.0.1

        if (StringUtils.isBlank(ip)) {

            ip = request.getHeader("x-forwarded-for"); //这才是正确获取客户端的IP地址

            System.out.println("getHeader方法获取:" + ip);
        }
        return ip;
    }

    //上下这两种方式都可以，只要弄清楚这两个方法的意思就行，实际中，用下面这种更好些
    public String getMyIpFromRequest1(HttpServletRequest request) {

        String ip = request.getHeader("x-forwarded-for");

        System.out.println("getHeader方法获取:" + ip);

        if (StringUtils.isBlank(ip)) {

            ip = request.getRemoteAddr();

            System.out.println("getRemoteAddr方法获取:" + ip);
        }

        return ip;
    }
}
