package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.BaseAttrService;
import com.atguigu.gmall.service.ListService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;


/**
 * Created by Administrator on 2018/9/13.
 */
@Controller
public class ListController {

    @Reference
    ListService listService;

    @Reference
    BaseAttrService baseAttrService;

    //根据前台传入的查询条件，进行查询操作  urlParam
    @RequestMapping("list.html")
    public String list(SkuLsParam skuLsParam, ModelMap modelMap){


        //用来保存销售属性值id
        Set<String> attrValueSet =  new HashSet<>();

        //根据查询条件，获取到的数据
        List<SkuLsInfo> skuLsInfoList =  listService.search(skuLsParam);

        for (SkuLsInfo skuLsInfo : skuLsInfoList) {

            //获取到每一个商品 的所有平台属性值
            List<SkuLsAttrValue> skuAttrValueList = skuLsInfo.getSkuAttrValueList();

            //遍历所有的平台属性值
            for (SkuLsAttrValue skuLsAttrValue : skuAttrValueList) {

                //将平台属性值对应 的id，添加到Set集合中，进行去重
                attrValueSet.add(skuLsAttrValue.getValueId() );

            }
        }

        //制作当前请求的面包屑
        List<Crumb> crumbs = new ArrayList<>();

        //将Set集合按照设置的符号，转换为字符串
        String valueId = StringUtils.join(attrValueSet, ",");


        //根据拿到的平台属性值id，查询对应的平台属性
        List<BaseAttrInfo> attrList =   baseAttrService.getAttrListByValueId(valueId);


        String[] skuLsParamValueId = skuLsParam.getValueId();

        //遍历所有选择的平台属性值id（被用户选择的）
        if(skuLsParamValueId!= null && skuLsParamValueId.length>0){


            //这个sid，是当前正在遍历的平台属性值 id
            for (String sid : skuLsParamValueId) {

                //每一个用户选择的平台属性值都会对应一个面包屑
                //那么这个sid，可以认为是 当前面包屑的id
                //即当前正在遍历的平台属性值，即为面包屑
                Crumb crumb = new Crumb();


                //删除当前请求中所包含的属性
                //遍历  查询到的平台属性
                Iterator<BaseAttrInfo> iterator = attrList.iterator();

                while (iterator.hasNext()){
                    BaseAttrInfo baseAttrInfo = iterator.next();

                    List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

                    //遍历每一个平台属性的 属性值
                    for (BaseAttrValue baseAttrValue : attrValueList) {

                        String id = baseAttrValue.getId();

                        //如果正在遍历的平台属性id（即已经被用户选择了的平台属性）
                        //  等于 某一个平台属性的平台属性值的id，则将这个平台属性移除（即不再显示了）
                        if(id.equals(sid)){

                            //删除对应的属性
                            iterator.remove();
                            crumb.setValueName(baseAttrValue.getValueName());
                        }
                    }
                }

                //获取删除面包屑的地址
                //那么删除 当前这个面包屑时，
                // 浏览器的地址为：当前浏览器中的地址 -(不包含) 当前正在遍历的这个平台属性值id，即当前面包屑的id
                String crumbUrlParam = getCrumbUrlParam(skuLsParam, sid);

                //设置面包屑地址
                crumb.setUrlParam(crumbUrlParam);
                crumbs.add( crumb );
            }
        }



        //将查询的平台属性，放在域对象中 skuLsInfoList
        modelMap.addAttribute("attrList",attrList);

        //将查询的数据，放在域对象中 skuLsInfoList
        modelMap.addAttribute("skuLsInfoList",skuLsInfoList);

        //拼接当前请求
        String urlParam =  getUrlParam(skuLsParam);

        //将当前请求参数，放在域对象中 skuLsInfoList
        modelMap.addAttribute("urlParam",urlParam);

        //将用户选择平台属性值，添加到面包屑
        modelMap.addAttribute("attrValueSelectedList",crumbs);

        return "list";

    }

    /**
     * 面包屑的请求地址
     */
    public String getCrumbUrlParam(SkuLsParam skuLsParam,String sid){
        String catalog3Id = skuLsParam.getCatalog3Id();
        String keyword = skuLsParam.getKeyword();
        String[] valueId = skuLsParam.getValueId();

        String urlParam="";

        //当三级分类不为空时，才进行拼接
        if(StringUtils.isNotBlank(catalog3Id)){

            //当请求参数不为空时，则需要使用 & 开头进行凭借
            if(StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam+"&";
            }

            //当请求参数为空，说明是第一个参数，不需要增加&
            urlParam = urlParam+"catalog3Id="+catalog3Id;
        }



        if(StringUtils.isNotBlank(keyword)){

            //当请求参数不为空时，则需要使用 & 开头进行凭借
            if(StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam+"&";
            }

            //当请求参数为空，说明是第一个参数，不需要增加&
            urlParam = urlParam+"keyword="+keyword;
        }

        if(valueId!=null && valueId.length >0){

            //用户选择的所有平台属性值
            for (String id : valueId) {

                //当用户选择多个平台属性值时：参数为 三级分类id & 关键词 & 每个选择的平台属性值id
                //现在要删除其中某一个平台属性值：那么参数中，就不能包括这个平台属性值id

                //当用户选择的平台属性值id，不等于 面包屑需要删除的id
                //sid 即为当前面包屑的id
                //每次只会排除一个
                if(!id.equals(sid)){

                    urlParam = urlParam+"&valueId="+id;
                }
            }
        }

        return  urlParam;
    }

    /**
     * 地址栏中的参数
     * @param skuLsParam
     * @return
     */
    private String getUrlParam(SkuLsParam skuLsParam) {

        String catalog3Id = skuLsParam.getCatalog3Id();
        String keyword = skuLsParam.getKeyword();
        String[] valueId = skuLsParam.getValueId();

        String urlParam="";

        //当三级分类不为空时，才进行拼接
        if(StringUtils.isNotBlank(catalog3Id)){

            //当请求参数不为空时，则需要使用 & 开头进行凭借
            if(StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam+"&";
            }

            //当请求参数为空，说明是第一个参数，不需要增加&
            urlParam = urlParam+"catalog3Id="+catalog3Id;
        }



        if(StringUtils.isNotBlank(keyword)){

            //当请求参数不为空时，则需要使用 & 开头进行凭借
            if(StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam+"&";
            }

            //当请求参数为空，说明是第一个参数，不需要增加&
            urlParam = urlParam+"keyword="+keyword;
        }

        if(valueId!=null && valueId.length >0){

            for (String id : valueId) {
                urlParam = urlParam+"&valueId="+id;
            }
        }

        return  urlParam;
    }

}
