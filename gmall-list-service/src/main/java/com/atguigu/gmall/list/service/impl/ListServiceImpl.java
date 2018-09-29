package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParam;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/9/13.
 */
@Service
public class ListServiceImpl implements ListService {


    @Autowired
    JestClient jestClient;


    //根据前台传入的条件进行查询
    @Override
    public List<SkuLsInfo> search(SkuLsParam skuLsParam) {

        //String catalog3Id = skuLsParam.getCatalog3Id();

        List<SkuLsInfo> skuLsInfos =  new ArrayList<>();


       //根据三级分类id，从ElasticSearch中查询数据
        Search build = new Search.Builder(myDSL(skuLsParam)).addIndex("gmall0508").addType("SkuLsInfo").build();

        SearchResult searchResult = null;

        try {

            //执行查询语句
             searchResult = jestClient.execute(build);

            //获得返回结果
            List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);

            //遍历结果
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {

                //从source中获取命中具体数据
                SkuLsInfo skuLsInfo = hit.source;

                //高亮字段结果，也放在hit中
                //获得 需要 高亮的字段
                Map<String, List<String>> highlight = hit.highlight;

                //如果存在需要高亮的字段
                if(highlight!= null && highlight.size()>0){

                    List<String> skuName = highlight.get("skuName");

                    //如果需要高亮的字段不为空
                    if( StringUtils.isNotBlank( skuName.get(0)) ){
                        //则高亮字段替换 查询出的数据中的字段
                        skuLsInfo.setSkuName( skuName.get(0)  );
                    }
                }

                skuLsInfos.add(skuLsInfo);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println( myDSL(skuLsParam) );

        return skuLsInfos;
    }


    //构建ElasticSearch中的查询语句
    public String myDSL(SkuLsParam skuLsParam){

        //取出查询条件
        //根据三级分类进行过滤
        String catalog3Id = skuLsParam.getCatalog3Id();


        //根据关键字进行查询
        //说明：当根据关键字进行查询时，不会再拼接其他条件了
        String keyword = skuLsParam.getKeyword();

        //根据平台属性值查询
        String[] valueId = skuLsParam.getValueId();

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //bool查询
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        //过滤 先过滤再进行搜索
        //当三级分类id不为空时,才到ELasticSearch中进行查询
        if(StringUtils.isNoneEmpty( catalog3Id )){

            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", catalog3Id);
            boolQueryBuilder.filter(termQueryBuilder);
        }


        //搜索，根据关键字进行搜索
        if(StringUtils.isNoneEmpty( keyword )){
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", keyword);
            boolQueryBuilder.must(matchQueryBuilder);
        }

        if(valueId!=null && valueId.length>0){

            for (int i = 0; i < valueId.length; i++) {

                //将每个valueId进行拼接查询
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId[i]);
                boolQueryBuilder.filter(termQueryBuilder);

            }
        }

        //构建DSL查询语句
        searchSourceBuilder.query(boolQueryBuilder);


        //设置高亮查询：高亮查询与query查询是平级关系
        HighlightBuilder highlightBuilder = new HighlightBuilder();

        //设置需要高亮显示的属性
        highlightBuilder.field("skuName");

        //设置高亮字段的样式  preTags  postTags
        highlightBuilder.preTags("<span style='color:red;font-weight: bolder'>");
        highlightBuilder.postTags("</span>");

        searchSourceBuilder.highlight(highlightBuilder);

        //将构建的语句，转换为字符串
        return searchSourceBuilder.toString();
    }
}
