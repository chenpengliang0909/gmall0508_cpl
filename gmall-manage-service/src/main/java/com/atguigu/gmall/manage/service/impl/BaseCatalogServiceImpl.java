package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.BaseCatalog3;
import com.atguigu.gmall.manage.mapper.BaseCatalog1Mapper;
import com.atguigu.gmall.manage.mapper.BaseCatalog2Mapper;
import com.atguigu.gmall.manage.mapper.BaseCatalog3Mapper;
import com.atguigu.gmall.service.BaseCatalogService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 注意：@Service注解，必须为dubbo下的
 */
@Service
public class BaseCatalogServiceImpl implements BaseCatalogService {

    @Autowired
    BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    BaseCatalog3Mapper baseCatalog3Mapper;

    @Override
    public List<BaseCatalog1> getCatalog1() {

        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2ByCtg1(String id) {

        //根据传入的一级菜单的id，查询对应的二级菜单
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(id);
        List<BaseCatalog2> select = baseCatalog2Mapper.select(baseCatalog2);

        return select;
    }

    @Override
    public List<BaseCatalog3> getCatalog3ByCtg2(String id) {

        //根据传入的二级菜单的id，查询对应的三级菜单
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(id);
        List<BaseCatalog3> select = baseCatalog3Mapper.select(baseCatalog3);

        return select;
    }
}
