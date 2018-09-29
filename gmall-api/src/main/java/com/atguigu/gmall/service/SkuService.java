package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SpuSaleAttr;

import java.util.List;

/**
 * Created by Administrator on 2018/9/8.
 */
public interface SkuService {

    List<SkuInfo> skuInfoListBySpu(String supId);

    void saveSku(SkuInfo skuInfo);

    SkuInfo getSkuById(String skuId);

    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String spuId,String skuId);

    List<SkuInfo> getSkuSaleAttrValueListBySpu(String spuId);

    List<SkuInfo> getSkuInfoBycatalog3Id(String catalog3Id);

    List<BaseCatalog2> queryAllCatalog(String catalog1Id);

    List<BaseCatalog1> getAllCatalog1();
}
