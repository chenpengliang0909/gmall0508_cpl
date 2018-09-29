package com.atguigu.gmall.list;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.util.ElasticSearchUtils;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

	@Autowired
	JestClient jestClient;

	//特别注意：这里需要使用Reference来进行注入
	@Reference
	SkuService skuService;


	@Test
	public void testBigDecimal(){

		BigDecimal b1 = new BigDecimal(0.01d);
		BigDecimal b2 = new BigDecimal(0.01f);

		System.out.println( "dubole:"+b1);
		System.out.println( "float:"+b2);
	}


	//StringUtils.join()方法的测试
	@Test
	public void test2(){

		//用来保存销售属性值id
		Set<String> set =  new HashSet<>();
		set.add("1");
		set.add("2");
		set.add("23");
		set.add("45");
		set.add("18");
		String join = StringUtils.join(set, ">>>");
		System.out.println( join );
		// 1,23,45,2,18
	}



	//查询所有一级分类id下的所有二级分类id的所有三级分类的数据,并输出到本地
	@Test
	public void queryAllCatalog(){

		//查询每一个一级分类下的所有二级分类和 三级分类
		//创建map，用于方法数据
		HashMap<String, List<BaseCatalog2>> hashMap = new HashMap<>();

		//获取所有的一级分类
		List<BaseCatalog1> baseCatalog1List = skuService.getAllCatalog1();

		for (BaseCatalog1 baseCatalog1 : baseCatalog1List) {

			//获得每一个一级分类
			String catalog1Id = baseCatalog1.getId();

			//根据一级分类id，获取对应所有二级分类和三级分类
			List<BaseCatalog2> baseCatalog2List  =  skuService.queryAllCatalog(catalog1Id);

			hashMap.put(catalog1Id,baseCatalog2List);

		}

		//将查询到的数据，转换为Json
		String AllCatalogJson = JSON.toJSONString(hashMap);

		FileOutputStream fos = null;
		try {
			//创建一个输出流
			fos = new FileOutputStream(new File("E:\\atguigu_Java_install\\workspace\\workspace_idea\\gmall0508_cpl\\gmall-list-web\\src\\main\\resources\\static\\index\\json\\cpl_catalog.json"));

			//将Json字符串，转换为字节数组
			byte[] bytes = AllCatalogJson.getBytes();

			//将字符换输出到本地指定的位置
			fos.write( bytes);

		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			//关；流
			if( fos!= null){
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}



		System.out.println( AllCatalogJson);

	}




	//从ElasticSearch中查询数据
	@Test
	public void search() throws IOException {

		List<SkuLsInfo> list = new ArrayList<>();

	/*	String quesyStr = "{\n" +
				"  \"query\": {\n" +
				"    \"match_all\": {}\n" +
				"  }\n" +
				"  , \"from\": 0\n" +
				"  , \"size\": 100\n" +
				"}";*/
		//设置查询语句（查询条件）
		//Search search = new Search.Builder(quesyStr).addIndex("gmall0508").addType("SkuLsInfo").build();

		System.out.println( getMyDsl2() );

		Search search = new Search.Builder(getMyDsl2()).addIndex("gmall0508").addType("SkuLsInfo").build();

		//执行查询语句，返回查询结果
		SearchResult searchResult = jestClient.execute(search);

		//取出查询结果命中记录
		List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);

		//遍历命中数据，从每一个命中数据的【source】中取出具体的数据
		for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
			SkuLsInfo skuLsInfo = hit.source;
			list.add(skuLsInfo);
		}

		System.out.println( list.size() );

	}

	//通过ES的查询工具，自动生成查询语句
	public String getMyDsl2(){

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

		TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", 61);

		//过滤
		boolQueryBuilder.filter(termQueryBuilder);

		MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", "华为");

		//查询
		boolQueryBuilder.must(matchQueryBuilder);

		searchSourceBuilder.query(boolQueryBuilder);
		searchSourceBuilder.from(0);
		searchSourceBuilder.size(100);

		return searchSourceBuilder.toString();
	}

	//通过ES的查询工具，自动生成查询语句
	public String getMyDsl(){

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		//bool查询
		BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

		TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", 61);

		//过滤
		boolQueryBuilder.filter(termQueryBuilder);

		//过滤之后，再指定，按照那个字段进行查询
		MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", "小米");
		boolQueryBuilder.must(matchQueryBuilder);

		//将设置好的参数，放入查询中
		searchSourceBuilder.query( boolQueryBuilder);
		//分页
		searchSourceBuilder.from(0);
		searchSourceBuilder.size(100);


		//将查询语句以字符串的形式返回
		return searchSourceBuilder.toString();
	}

	@Test
	public void test1(){

		ElasticSearchUtils.insertToElasticSearch(skuService,jestClient,"1");
	}


	//将MySQL中数据插入到ElasticSearch中
	@Test
	public void contextLoads() throws InvocationTargetException, IllegalAccessException, IOException {

		//根据三级分类id，查询所有的skuInfo信息,需要封装平台属性集合、图片集合

		String catalog3Id = "178";

		List<SkuInfo> skuInfos = skuService.getSkuInfoBycatalog3Id(catalog3Id);


		//将skuInfo 转换为skuLsInfo

		List<SkuLsInfo> skuLsInfos =  new ArrayList<>();

		for (SkuInfo skuInfo : skuInfos) {

            SkuLsInfo skuLsInfo = new SkuLsInfo();

            //使用BeanUtils的工具类，将一种类型对象，复制为另一种类型对象
			//目标对象  源对象
            BeanUtils.copyProperties(skuLsInfo,skuInfo);

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

			jestClient.execute(build);
		}

	}

}
