package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseSaleAttr;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.manage.util.FileUploadUtils;
import com.atguigu.gmall.pojo.Massage;
import com.atguigu.gmall.service.SpuService;
import org.csource.common.MyException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/9/7.
 */
@RestController
public class SpuController {

    @Reference
    SpuService spuService;


    //spuImageList
    //根据商品id 获取 商品图片
    @RequestMapping("spuImageList")
    public List<SpuImage> spuImageList(@RequestParam Map<String,String>map){

        String spuId = map.get("spuId");

        return spuService.spuImageList(spuId);
    }

    //根据 商品id 获取销售属性 和对应的销售属性值
    @RequestMapping("spuSaleAttrList")
    public List<SpuSaleAttr> spuSaleAttrList(@RequestParam Map<String,String>map){

        String spuId = map.get("spuId");

        return spuService.spuSaleAttrList(spuId);
    }

    @RequestMapping("getSpuList")
    public List<SpuInfo> getSpuList(@RequestParam Map<String,String>map){
        String catalog3Id = map.get("catalog3Id");

        return  spuService.getSpuList(catalog3Id);
    }


    @RequestMapping("baseSaleAttrList")
    public List<BaseSaleAttr> baseSaleAttrList(){

        return spuService.baseSaleAttrList();

    }

    @RequestMapping("saveSpu")
    public Massage saveSpu(SpuInfo spuInfo){

        spuService.saveSpu(spuInfo);

        return Massage.success();

    }


    @RequestMapping("fileUpload")
    public String fileUpload(@RequestParam("file") MultipartFile file) throws IOException, MyException {

        String  imgName = FileUploadUtils.uploadImge(file);

        return imgName;

    }
}
