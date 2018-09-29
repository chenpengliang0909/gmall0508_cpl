package com.atguigu.gmall.list.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by Administrator on 2018/9/13.
 */
@Controller
public class IndexController {

    @RequestMapping("index")
    public String index(){

        return "index";
    }


}
