package com.atguigu.gmall.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用来区分三类方法
 * 只有标记上本注解的，才进行拦截，否则不拦截
 */

@Retention(value = RetentionPolicy.RUNTIME) //用于定于注解的生命周期，RUNTIME：运行期间
@Target(value={ElementType.METHOD}) //定义用于描述注解可以用在类中的哪些结构上（如属性、方法、构造器等）
public @interface LoginRequire {

    boolean needSuccess() default true;  //返回true表示必须登录

}
