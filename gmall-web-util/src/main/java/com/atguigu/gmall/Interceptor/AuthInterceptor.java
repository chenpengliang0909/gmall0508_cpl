package com.atguigu.gmall.Interceptor;

import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.constant.AppConstant;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Created by Administrator on 2018/9/18.
 */
//1、继承HandlerInterceptorAdapter类
//3、将拦截器交给Spring容器管理
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    //2、重写preHandle方法，方法返回true，表示放行，返回false，表示拦截
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        //说明：handler为拦截的方法的代理
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);

        if (methodAnnotation == null) { //说明放行，不需要拦截
            return true;

        } else { //说明需要拦截

            /**
             * 判断拦截的方法，是否必须登录
             * true：表示必须登录
             * false：表示可以不登录，去访问另一个分支
             */
            boolean boo = methodAnnotation.needSuccess();

            String token = "";
            //从cookie中获取token
            String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);

            String newToken = request.getParameter("token"); //获取请求参数中的token

            //如果 newToken空，oldToken空 从未登录，忽略不管

            //如果 newToken空，oldToken不空 说明之前已经登录了
            if (StringUtils.isNotBlank(oldToken) && StringUtils.isBlank(newToken)) {
                token = oldToken;
            }

            //如果 newToken不空，oldToken空  说明第一次登录
            if (StringUtils.isBlank(oldToken) && StringUtils.isNotBlank(newToken)) {
                token = newToken;
            }

            //如果 newToken不空，oldToken不空 说明token已经过期了
            if (StringUtils.isNotBlank(oldToken) && StringUtils.isNotBlank(newToken)) {
                token = newToken;
            }

            //验证
            if (StringUtils.isNotBlank(token)) { //有token，说明需要进行验证

                String ip=getMyIpFromRequest(request);//盐值为：客户端机器的IP地址

                //1、进行验证，调用远程认证中心的认证方法

                //String url = "http://192.168.0.42:8085/verify?token="+token+"&clientIp="+ip; //远程验证中心的方法
                String url = "http://localhost:8085/verify?token="+token+"&clientIp="+ip; //远程验证中心的方法

                String success = HttpClientUtil.doGet(url); //通过这个工具类来发送请求，请求远程的验证中心

                if ("success".equals(success)) { //说明验证通过了，放行

                    //2、每次验证通过后，将token保存到cookie中，刷新过期时间
                    CookieUtil.setCookie(request,response,AppConstant.COOKIE_NAME_TOKEN,token, AppConstant.EXPIRED_TIME,true);

                    Map userMap = JwtUtil.decode(AppConstant.TOKEN_KEY, token, ip);//将token进行解码

                    //3、将用户的id，放入请求中
                    request.setAttribute("userId",userMap.get("userId"));  //Map中的值来自于token
                    request.setAttribute("nickName",userMap.get("nickName"));

                    return true;
                }
            }

            if (boo) { //为true，说明必须登录

                //重定向到登录界面,并且携带上之前请求的界面，用于登录成功后，继续访问该界面，如点击去结算时的请求
                response.sendRedirect("http://passport.gmall.com:8085/index?returnUrl=" + request.getRequestURL());
                return false;

            } else { //说明可以不登录
                return true;
            }
        }
    }

    /***
     * 远程连接中获取客户端机器的IP地址:盐值
     * @param request
     * @return
     */
    public String getMyIpFromRequest(HttpServletRequest request) {

        String ip = request.getRemoteAddr(); //能获取到，只不过是获取了代理服务器的ip地址 //127.0.0.1

        //System.out.println("getRemoteAddr方法获取:"+ip ); //127.0.0.1

        if(StringUtils.isBlank(ip)){

            ip= request.getHeader("x-forwarded-for"); //这才是正确获取客户端的IP地址

            System.out.println("getHeader方法获取:"+ip );
        }
        return ip;
    }

}

