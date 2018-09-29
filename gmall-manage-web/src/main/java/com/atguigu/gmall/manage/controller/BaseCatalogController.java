package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.BaseCatalog3;
import com.atguigu.gmall.service.BaseCatalogService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


/**
 * 用于显示一级、二级、三级分类
 */
@RestController
public class BaseCatalogController {

    @Reference
    BaseCatalogService baseCatalogService;

    @RequestMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1(){

        //获取所有的一级菜单
        List<BaseCatalog1> list =  baseCatalogService.getCatalog1();
        return  list;
    }

    @RequestMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2ByCtg1(@RequestParam Map<String,String>map){

        String id = map.get("catalog1Id");

        //获取所有的二级菜单
        List<BaseCatalog2> list =  baseCatalogService.getCatalog2ByCtg1(id);
        return  list;
    }


    @RequestMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3ByCtg2(@RequestParam Map<String,String>map){

        String id = map.get("catalog2Id");

        //获取所有的三级菜单
        List<BaseCatalog3> list =  baseCatalogService.getCatalog3ByCtg2(id);
        return  list;
    }

}
