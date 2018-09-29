package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParam;

import java.util.List;

/**
 * Created by Administrator on 2018/9/13.
 */
public interface ListService {

    List<SkuLsInfo> search(SkuLsParam skuLsParam);

}
