package com.atguigu.gmall.util;

import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import org.apache.commons.beanutils.BeanUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/9/13.
 */
public class ElasticSearchUtils {


    //向ElasticSearch中添加数据
    public static void insertToElasticSearch(SkuService skuService, JestClient jestClient,String catalog3Id){

        //根据三级分类id，查询所有的skuInfo信息,需要封装平台属性集合、图片集合


        List<SkuInfo> skuInfos = skuService.getSkuInfoBycatalog3Id(catalog3Id);


        //将skuInfo 转换为skuLsInfo

        List<SkuLsInfo> skuLsInfos =  new ArrayList<>();

        for (SkuInfo skuInfo : skuInfos) {

            SkuLsInfo skuLsInfo = new SkuLsInfo();

            //使用BeanUtils的工具类，将一种类型对象，复制为另一种类型对象
            //目标对象  源对象
            try {
                BeanUtils.copyProperties(skuLsInfo,skuInfo);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            skuLsInfos.add(skuLsInfo);
        }

        //System.out.println( skuLsInfos.size() );

        //将转换为skuLsInfo数据，添加到ElasticSearch中

        for (SkuLsInfo skuLsInfo : skuLsInfos) {
            //Builder为一个静态内部类，在构造器中需要传入数据源，即需要上传到ElasticSearch中的数据
            // 如下相当于 造了一个Index的静态内部类Builder的对象  new Index.Builder(skuLsInfo)
            //创建静态内部类的对象，可以直接通过 外部类 调用 静态内部类的构造器 进行创建

            //new Index.Builder(skuLsInfo).index("gmall0508").type("SkuLsInfo").id(skuLsInfo.getId()).build();
            Index build = new Index.Builder(skuLsInfo).index("gmall0508").type("SkuLsInfo").id(skuLsInfo.getId()).build();

            try {
                jestClient.execute(build);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
