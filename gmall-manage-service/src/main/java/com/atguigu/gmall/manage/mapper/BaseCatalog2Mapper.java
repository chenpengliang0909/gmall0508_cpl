package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.BaseCatalog2;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * Created by Administrator on 2018/9/4.
 */
public interface BaseCatalog2Mapper extends Mapper<BaseCatalog2> {

    List<BaseCatalog2> queryAllCatalog(String catalog1Id);
}
