package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.SkuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Administrator on 2018/9/10.
 */
@Controller
public class ItemController {

    @Reference
    SkuService skuService;

    @RequestMapping("{skuId}.html")
    public String index(@PathVariable("skuId") String skuId, ModelMap modelMap){

        //根据skuid，获取对应skuinfo信息：当前SKU

        SkuInfo skuInfo = skuService.getSkuById(skuId);
        modelMap.addAttribute("skuInfo",skuInfo);


        //查询SPU 销售属性列表 包括属性 和 属性值
        /**
         * 1、根据skuid，获取对应的spuid
         *
         */
        String spuId = skuInfo.getSpuId();

        //这里面同时封装 销售属性 和销售属性值
        List<SpuSaleAttr> spuSaleAttrs =  skuService.getSpuSaleAttrListCheckBySku(spuId,skuId);

        //将数据放在Request域中
        modelMap.addAttribute("spuSaleAttrListCheckBySku",spuSaleAttrs);


        //查询spuid下sku的兄弟姐妹的所有销售属性
        List<SkuInfo> skuInfos = skuService.getSkuSaleAttrValueListBySpu(spuId);

        //将  skuInfos 信息 封装到HashMap中
        HashMap<String, String> hashMap = new HashMap<>();
        /*
            销售属性值的id 为key
            skuid  为value
         */

        if(skuInfos!=null && skuInfos.size()>0){
            //遍历销售属性
            for (SkuInfo info : skuInfos) {

                //获得SKU 的 id
                String id = info.getId();

                List<SkuSaleAttrValue> skuSaleAttrValueList = info.getSkuSaleAttrValueList();

                String key="";
                for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {

                    //获得销售属性值的id
                    String saleAttrValueId = skuSaleAttrValue.getSaleAttrValueId();

                    //进行拼接：如 |219|222
                    key = key + "|" + saleAttrValueId;

                }
                //将销售属性值的id 和   SKU 的 id 放到HashMap中
                hashMap.put(key,id);

            }
        }
        //封装后的hashMap：{|227|229=103, |228|230=102}
        //System.out.println( hashMap );

        //将HashMap转换为Json字符串
        String valuesSkuJson = JSON.toJSONString(hashMap);

        //将转换后的数据，放到request域中
        modelMap.addAttribute("valuesSkuJson",valuesSkuJson);


        return "item";
    }





}
