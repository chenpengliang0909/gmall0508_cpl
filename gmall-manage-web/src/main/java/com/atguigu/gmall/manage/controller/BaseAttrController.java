package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.pojo.Massage;
import com.atguigu.gmall.service.BaseAttrService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 操作三级分类对应的属性
 */
@RestController
public class BaseAttrController {

    @Reference
    BaseAttrService baseAttrService;

    /**
     * 根据属性id  获取对应的属性值
     * @param map
     * @return
     */
    @RequestMapping("getAttrValue")
    public Massage getAttrValue(@RequestParam Map<String,String> map){

        String attrId = map.get("attrId");

        List<BaseAttrValue> attrValueList = baseAttrService.getAttrValue(attrId);

        return  Massage.success(attrValueList);

    }

    //获取三级分类对应所有的属性  获取对应的平台属性
    @RequestMapping("getAttrList")
    public List<BaseAttrInfo> getAttrList(@RequestParam Map<String,String> map){

        String catalog3Id = map.get("catalog3Id");

        List<BaseAttrInfo> list = baseAttrService.getAttrList(catalog3Id);

        return list;
    }

    //新增SKU时，根据三级分类id，获取对应的平台属性 和属性值
    @RequestMapping("attrInfoList")
    public List<BaseAttrInfo> attrInfoList(@RequestParam Map<String,String> map){

        String catalog3Id = map.get("catalog3Id");

        //获取平台属性
        List<BaseAttrInfo> attrInfoList = baseAttrService.getAttrList(catalog3Id);
        if(attrInfoList != null && attrInfoList.size() > 0 ){
            //然后获取平台属性 对应的 属性值
            //遍历 平台属性
            for (BaseAttrInfo baseAttrInfo : attrInfoList) {

                //根据平台 属性id  查询 平台属性值
                String attrId = baseAttrInfo.getId();

                List<BaseAttrValue> attrValueList = baseAttrService.getAttrValue(attrId);

                //将查询到的 平台属性值 添加到对应的平台属性中
                baseAttrInfo.setAttrValueList(attrValueList);
            }

        }
        //将封装好的 平台属性 和平台属性值 返回
        return attrInfoList;

    }

    //添加/修改属性
    @RequestMapping("saveAttr")
    public Massage saveAttr(BaseAttrInfo baseAttrInfo){

        baseAttrService.saveAttr(baseAttrInfo);

        return Massage.success();
    }

    //删除属性
    @DeleteMapping("deleteAttr")
    public Massage deleteAttr(@RequestParam Map<String,String> map){

        String attrId = map.get("attrId");

        baseAttrService.deleteAttr(attrId);

        return Massage.success();
    }

}
