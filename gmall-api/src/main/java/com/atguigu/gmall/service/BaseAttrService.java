package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;

import java.util.List;

/**
 * Created by Administrator on 2018/9/4.
 */
public interface BaseAttrService {

    //根据三级分类ID 获取所有的属性名
    List<BaseAttrInfo> getAttrList(String catalog3Id);

    //保存属性名称 和 属性值
    void saveAttr(BaseAttrInfo baseAttrInfo);

    //根据属性id，删除对应属性值
    void deleteAttr(String attrId);

    List<BaseAttrValue> getAttrValue(String attrId);

    List<BaseAttrInfo> getAttrListByValueId(String valueId);
}
