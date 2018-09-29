package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.BaseCatalog3;

import java.util.List;


public interface BaseCatalogService  {

    List<BaseCatalog1> getCatalog1();

    List<BaseCatalog2> getCatalog2ByCtg1(String id);

    List<BaseCatalog3> getCatalog3ByCtg2(String id);
}
