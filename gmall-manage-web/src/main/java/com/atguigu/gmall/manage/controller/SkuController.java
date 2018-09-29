package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.pojo.Massage;
import com.atguigu.gmall.service.SkuService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/9/8.
 */
@RestController
public class SkuController {

    @Reference
    SkuService skuService;

    //保存SKU信息
    @RequestMapping("saveSku")
    public Massage saveSku(SkuInfo skuInfo){

        skuService.saveSku(skuInfo);

        return Massage.success();
    }


    //根据 spuId 获取对应 sku信息
    @RequestMapping("skuInfoListBySpu")
    public List<SkuInfo> skuInfoListBySpu(@RequestParam Map<String,String> map){

       String supId =  map.get("supId");

        return skuService.skuInfoListBySpu(supId);
    }
}
