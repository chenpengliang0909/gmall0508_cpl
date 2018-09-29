package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * Created by Administrator on 2018/9/8.
 */
@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    SkuInfoMapper skuInfoMapper;

    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;

    //sku_sale_attr_value
    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    SkuImageMapper skuImageMapper;

    @Autowired
    BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    RedisUtil redisUtil;


    @Override
    public List<SkuInfo> skuInfoListBySpu(String supId) {

        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSpuId(supId);
        return skuInfoMapper.select(skuInfo);
    }

    //保存SKU信息
    @Transactional
    @Override
    public void saveSku(SkuInfo skuInfo) {

        //======保存SKU信息 sku_info 表
        skuInfoMapper.insertSelective(skuInfo);

        //插入后，获取返回的主键值
        String skuId = skuInfo.getId();

        //======保存平台属性信息 sku_attr_value 表
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();

        if (skuAttrValueList != null && skuAttrValueList.size() > 0) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                //设置 skuid
                skuAttrValue.setSkuId(skuId);
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }

        }

        //======保存到销售属性信息 sku_sale_attr_value 表
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                //设置 skuid
                skuSaleAttrValue.setSkuId(skuId);
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }

        //======保存到SKU图片信息 sku_image 表
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList != null && skuImageList.size() > 0) {
            for (SkuImage skuImage : skuImageList) {
                //设置 skuid
                skuImage.setSkuId(skuId);
                skuImageMapper.insertSelective(skuImage);
            }
        }
    }

    /**
     * 根据skuId 获取 SKU信息
     *
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo getSkuById(String skuId) {

        SkuInfo skuInfo = null;

        //使用redis缓存
        try {
            //获得jedis对象，来操作Redis数据中数据
            Jedis jedis = redisUtil.getJedis();
            //缓存数据的 key
            String skuKey = "sku:" + skuId + ":info";

            //根据key从Redis中获取数据
            String skuInfoFromRedis = jedis.get(skuKey);

            //当在缓存中没有获取到时：可能是Redis宕机了，那么就要限制用户访问了，需要加锁了
            if (skuInfoFromRedis == null || skuInfoFromRedis.length() == 0) {
                System.err.println(Thread.currentThread().getName() + "：未命中");

                //准备 分布式锁 的key
                String skuLockKey = "sku:" + skuId + ":lock";

                //在 Redis缓存中，设置一把锁（其实就是一个键值对） ，锁的【key】如【sku:102:lock】： 锁的值为【OK】
                //NX参数表示当前命令中指定的KEY不存在才行,即只有当key不存在时，才能设置这个值
                //如果设置成功了：会返回一个【OK】
                //如果设置失败(说明已经存在该键值对，也就是已经存在该锁，就不能再设置了)，返回【空】
                String lock = jedis.set(skuLockKey, "OK", "nx", "px", 20);

                //如果设置成功了，说明就获得了“锁”
                if ("OK".equals(lock)) {

                    System.err.println(Thread.currentThread().getName() + "获得分布式锁！");

                    //那么就可以访问数据库了
                    skuInfo = getSkuInfoDB(skuId);

                    //如果从数据库中没有获取到值
                    if (skuInfo == null) {

                        //那么将Redis中缓存的数据设置为【empty】
                        jedis.setex(skuKey, 20, "empty");

                        //返回null
                        return null;
                    }

                    //如果，从数据库中获取到了
                    System.err.println( Thread.currentThread().getName()+"： 查询到数据##################### ##" );
                    //将查询到的Java对象，转换为JSON字符串
                    String skuInfoJsonNew = JSON.toJSONString(skuInfo);

                    //更新到Redis缓存中
                    jedis.setex(skuKey, 20, skuInfoJsonNew);

                    System.err.println( Thread.currentThread().getName()+"：数据库更新至Redis完毕############### #####" );

                    //关闭Redis连接
                    jedis.close();

                    return skuInfo;

                } else {
                    //如果在Redis服务器中，设置锁（键值对）时，没有成功，说明此时已经有其他用户拿到锁，在访问页面了，你需要等待
                    System.err.println(Thread.currentThread().getName() + "未获得分布式锁，开始自旋！");

                    //关闭Redis连接
                    jedis.close();

                    //开始自旋
                    return getSkuById(skuId);
                }

                //如果在缓存中获取到的数据为【empty】，说明数据中都已经没有数据了，则直接返回null
                //因为这个【empty】,是{当从数据库中没有获取到数据时，将Redis中缓存的数据设置为【empty】的}
            } else if (skuInfoFromRedis.equals("empty")) {

                return null;

            } else {
                //如果从缓存中获取数据了，那么将数据（Json字符串），转换为Java对象返回
                System.err.println(Thread.currentThread().getName() + "：命中缓存");

                //将获取到Json字符串，转换为Java对象
                skuInfo = JSON.parseObject(skuInfoFromRedis, SkuInfo.class);

                //关闭Redis连接
                jedis.close();

                System.err.println(Thread.currentThread().getName() + "直接返回命中的数据");
                //将skuInfo对象数据返回
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getSkuInfoDB(skuId);
    }


    public SkuInfo getSkuInfoDB(String skuId) {

        //使用db查询
        //根据skuid  获取对应skuinfo
        SkuInfo skuInfoParam = new SkuInfo();
        skuInfoParam.setId(skuId);

        //获取skuinfo 信息
        SkuInfo skuInfo = skuInfoMapper.selectOne(skuInfoParam);
        if(skuInfo!=null){
            //获得skuimage信息
            SkuImage skuImageParam = new SkuImage();
            skuImageParam.setSkuId(skuId);
            List<SkuImage> skuImages = skuImageMapper.select(skuImageParam);
            skuInfo.setSkuImageList(skuImages);
        }

        return skuInfo;

    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String spuId, String skuId) {

        return skuSaleAttrValueMapper.selectSpuSaleAttrListCheckBySku(Integer.parseInt(spuId), Integer.parseInt(skuId));
    }

    @Override
    public List<SkuInfo> getSkuSaleAttrValueListBySpu(String spuId) {

        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(Integer.parseInt(spuId));
    }

    /**
     * 根据三级分类id，查询所有的skuInfo信息
     * @param catalog3Id
     * @return
     */
    @Override
    public List<SkuInfo> getSkuInfoBycatalog3Id(String catalog3Id) {

        SkuInfo skuInfoParam = new SkuInfo();
        skuInfoParam.setCatalog3Id(catalog3Id);

        //根据三级分类id查询对应的skuInfo信息
        List<SkuInfo> skuInfos = skuInfoMapper.select(skuInfoParam);

        for (SkuInfo skuInfo : skuInfos) {
            //查询对应平台属性信息
            //获得skuinfo的主键id
            String id = skuInfo.getId();

            //根据skuid 获得对应的平台属性信息
            SkuAttrValue skuAttrValue = new SkuAttrValue();
            skuAttrValue.setSkuId(id);
            List<SkuAttrValue> skuAttrValues = skuAttrValueMapper.select(skuAttrValue);

            skuInfo.setSkuAttrValueList(skuAttrValues);

            //查询对应图片信息
            SkuImage skuImage = new SkuImage();
            skuImage.setSkuId(id);
            List<SkuImage> skuImages = skuImageMapper.select(skuImage);

            skuInfo.setSkuImageList(skuImages);

        }


        return skuInfos;
    }

    @Override
    public List<BaseCatalog2> queryAllCatalog(String catalog1Id) {

        return baseCatalog2Mapper.queryAllCatalog(catalog1Id);
    }

    //查询所有的一级分类
    @Override
    public List<BaseCatalog1> getAllCatalog1() {

        return baseCatalog1Mapper.selectAll();
    }
}
