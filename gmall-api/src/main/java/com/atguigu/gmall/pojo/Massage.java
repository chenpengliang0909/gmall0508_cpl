package com.atguigu.gmall.pojo;

import com.atguigu.gmall.constant.AppConstant;

/**
 * Created by Administrator on 2018/9/5.
 * 用于给浏览器返回数据信息
 */

public class Massage {

    private Integer code;  //0表示成功，1表示失败
    private String msg;    //给浏览器返回的信息
    private Object data;  //给浏览器返回的数据

    //提供若干操作成功的方法
    public static Massage success(){

        return new Massage(0, AppConstant.SUC_MSG,null);
    }

    public static Massage success(String msg){

        return new Massage(0,msg,null);
    }

    public static Massage success(Object data){

        return new Massage(0,AppConstant.SUC_MSG,data);
    }

    public static Massage success(String msg,Object data){

        return new Massage(0,msg,data);
    }

    //提供若干操作失败的方法
    public static Massage fail(){

        return new Massage(1, AppConstant.FAIL_MSG,null);
    }

    public static Massage fail(String msg){

        return new Massage(1,msg,null);
    }

    public static Massage fail(Object data){

        return new Massage(1,AppConstant.FAIL_MSG,data);
    }

    public static Massage fail(String msg,Object data){

        return new Massage(1,msg,data);
    }




    public Massage(Integer code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public Massage() {
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Massage{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
