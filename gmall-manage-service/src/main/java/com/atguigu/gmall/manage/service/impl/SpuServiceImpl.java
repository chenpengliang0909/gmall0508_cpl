package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by Administrator on 2018/9/7.
 */
@Service
public class SpuServiceImpl implements SpuService {

    @Autowired
    BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    SpuInfoMapper spuInfoMapper;

    @Autowired
    SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    SpuImageMapper spuImageMapper;

    @Override
    public List<BaseSaleAttr> baseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    public void saveSpu(SpuInfo spuInfo) {

        //==============将数据保存在 spu_info 表
        //需要设置的表字段：spu_name  description  catalog3_id
        spuInfoMapper.insertSelective(spuInfo);

        //插入之后，获取返回的主键 spuId
        String spuId = spuInfo.getId();


        //==============将数据保存在 spu_sale_attr 表
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();

        //当添加了销售属性时，才进行添加
        if(spuSaleAttrList != null &&spuSaleAttrList.size()!=0){

            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                //将spuId设置进去
                spuSaleAttr.setSpuId(spuId);

                //需要设置的表字段：spu_id  sale_attr_id  sale_attr_name
                spuSaleAttrMapper.insertSelective(spuSaleAttr);

                //===============将数据保存在 spu_sale_attr_value 表
                //保存 对应 销售属性值
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                    //将spuId 设置进来
                    spuSaleAttrValue.setSpuId(spuId);
                    //需要设置的表字段：  spu_id  sale_attr_id  sale_attr_value_name
                    spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                }
            }

        }

        //=======================将数据保存在 spu_image 表
        // 需要设置的表字段：   spu_id  img_name  img_url
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();

        //当添加了上传了图片时，才进行添加
        if(spuImageList != null && spuImageList.size() != 0){

            for (SpuImage spuImage : spuImageList) {
                //将spuId 设置进来
                spuImage.setSpuId(spuId);
                spuImageMapper.insertSelective(spuImage);
            }
        }


    }

    @Override
    public List<SpuInfo> getSpuList(String catalog3Id) {

        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);

        return spuInfoMapper.select(spuInfo);
    }

    //根据 商品id 获取销售属性 和 对应的销售属性值
    @Override
    public List<SpuSaleAttr> spuSaleAttrList(String spuId) {

        SpuSaleAttr spuSaleAttr = new SpuSaleAttr();
        spuSaleAttr.setSpuId(spuId);

        //根据 商品id 获取销售属性
        List<SpuSaleAttr> spuSaleAttrs = spuSaleAttrMapper.select(spuSaleAttr);

        if(spuSaleAttrs!=null && spuSaleAttrs.size()>0){
           //遍历销售属性
            for (SpuSaleAttr saleAttr : spuSaleAttrs) {

                SpuSaleAttrValue spuSaleAttrValue = new SpuSaleAttrValue();

                spuSaleAttrValue.setSpuId(spuId);
                spuSaleAttrValue.setSaleAttrId(saleAttr.getSaleAttrId());

                //根据销售属性id  和 商品id 获取对应的销售属性的值
                List<SpuSaleAttrValue> spuSaleAttrValues = spuSaleAttrValueMapper.select(spuSaleAttrValue);

                //封装销售属性的值
                saleAttr.setSpuSaleAttrValueList(spuSaleAttrValues);

            }
        }

        //将封装 销售属性  和 销售属性值 的集合返回
        return spuSaleAttrs;
    }

    //根据商品id 获取 商品图片
    @Override
    public List<SpuImage> spuImageList(String spuId) {

        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);
    }
}
