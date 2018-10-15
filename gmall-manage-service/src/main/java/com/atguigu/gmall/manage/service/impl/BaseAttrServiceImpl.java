package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.manage.mapper.BaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.BaseAttrValueMapper;
import com.atguigu.gmall.service.BaseAttrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * Created by Administrator on 2018/9/4.
 */
@Service
public class BaseAttrServiceImpl implements BaseAttrService {

    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    BaseAttrValueMapper baseAttrValueMapper;

    //根据三级分类ID 获取所有的属性  和 属性值
    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3Id);
        List<BaseAttrInfo> select = baseAttrInfoMapper.select(baseAttrInfo);
        return select;
    }

    //新增属性
    @Override
    public void saveAttr(BaseAttrInfo baseAttrInfo) {

        //判断 传入的baseAttrInfo 是否有属性info id
        String baseAttrInfoId = baseAttrInfo.getId();
        if (StringUtils.isEmpty(baseAttrInfoId)) {

            //当插入的属性值的主键id为空串("")时，需要设置为null，否则执行插入语句时，会一直报错
            baseAttrInfo.setId(null);

            //如果没有，说明是 新增属性值
            baseAttrInfoMapper.insertSelective(baseAttrInfo);

            //获取插入属性名称后，所对应的主键id
            String attrId = baseAttrInfo.getId();

            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

            //说明有添加属性值，才进行处理
            if (attrValueList != null) {
                //【iter】 生成增强for循环快捷键
                for (BaseAttrValue baseAttrValue : attrValueList) {

                    //当插入的属性值的主键id为空串时，需要设置为null，否则执行插入语句时，会一直报错
                    if (StringUtils.isEmpty(baseAttrValue.getId())) {
                        baseAttrValue.setId(null);
                    }

                    //插入属性对应的属性值，需要关联属性id
                    baseAttrValue.setAttrId(attrId);
                    // 这里有个疑问
                    baseAttrValueMapper.insert(baseAttrValue);
                }
            }

        } else {

            //如果有，说明是 修改属性值,调用 更新属性方法
            updateAttr(baseAttrInfo);
        }

    }

    //更新属性名称 和 属性值
    public void updateAttr(BaseAttrInfo baseAttrInfo) {

        /*
        * 传递过来的参数有：
        * 1、属性id、属性名称
        * 2、属性值
        * 3、三级分类id
        *
        * 要进行的操作
        * 1、更新base_attr_info表中 属性名称，根据属性id
        * 2、在base_attr_value表中，删除属性id对应所有属性值
        * 3、在base_attr_value表中，插入属性id对应的属性值
        * */
        BaseAttrInfo baseAttrInfo1 = new BaseAttrInfo();
        baseAttrInfo1.setId(baseAttrInfo.getId());
        baseAttrInfo1.setAttrName(baseAttrInfo.getAttrName());
        baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo1);


        //根据属性id，删除对应属性值
        deleteAttrValue(baseAttrInfo.getId());

        //根据属性id，插入对应的属性值
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        for (BaseAttrValue baseAttrValue : attrValueList) {

            if (StringUtils.isEmpty(baseAttrValue.getId())) {
                baseAttrValue.setId(null);
            }

            baseAttrValue.setAttrId(baseAttrInfo.getId());

            baseAttrValueMapper.insert(baseAttrValue);
        }
    }

    //根据属性id，删除属性，以及对应属性值
    @Override
    public void deleteAttr(String attrId) {

                /*
        * 2、根据属性id，在base_attr_value表中，删除对应的属性值
        * 3、根据属性id，在base_attr_info表中，删除对应的属性
        *
        * */
        deleteAttrValue(attrId);

        baseAttrInfoMapper.deleteByPrimaryKey(attrId);


    }

    //根据属性id，删除对应属性值
    public void deleteAttrValue(String attrId) {

        Example example = new Example(BaseAttrValue.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("attrId", attrId);
        baseAttrValueMapper.deleteByExample(example);

    }

    //根据属性id，获取对应的属性值
    @Override
    public List<BaseAttrValue> getAttrValue(String attrId) {

        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);

        List<BaseAttrValue> select = baseAttrValueMapper.select(baseAttrValue);

        return select;
    }

    //根据拿到的平台属性值id，查询对应的平台属性
    @Override
    public List<BaseAttrInfo> getAttrListByValueId(String valueId) {

        //结果为  1,23,45,2,18
        List<BaseAttrInfo> baseAttrInfos = baseAttrInfoMapper.selectAttrListByValueId(valueId);

        return baseAttrInfos;

    }
}


