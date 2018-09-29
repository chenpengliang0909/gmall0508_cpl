package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.util.RedisUtil;
import com.atguigu.gmall.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/9/16.
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    CartInfoMapper cartInfoMapper;

    @Autowired
    RedisUtil redisUtil;

    /**
     * 根据将要添加的商品，判断在数据库中是否存在
     *
     * @param cartInfo
     * @return
     */
    @Override
    public CartInfo ifCartExits(CartInfo cartInfo) {

        //根据将要添加的商品 的 userID  和 skuid 在数据库中进行查询
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId", cartInfo.getUserId()).andEqualTo("skuId", cartInfo.getSkuId());
        CartInfo cartInfoDB = cartInfoMapper.selectOneByExample(example); //结果最多只会有一条
        return cartInfoDB;
    }

    /**
     * 更新商品在购物车数量和商品总价格
     *
     * @param cartInfoDB
     */
    @Override
    public void updateCart(CartInfo cartInfoDB) {

        cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);

        //更新完成之后，同步到redis缓存
        flushCartCacheByUserId(cartInfoDB.getUserId());

    }

    /**
     * 往数据库的购物车中 插入商品
     *
     * @param cartInfo
     */
    @Override
    public void insertCart(CartInfo cartInfo) {

        cartInfoMapper.insertSelective(cartInfo);
        //更新完成之后，同步到redis缓存
        flushCartCacheByUserId(cartInfo.getUserId());
    }

    /**
     * 数据库操作完成后，同步到redis中，思路为：
     * 1、根据userId从数据库中取出购物车数据；
     * 2、将数据保存在redis中：以hash的数据结构
     */
    @Override
    public void flushCartCacheByUserId(String userId) {

        // 1、根据userId从数据库中取出购物车数据；
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId", userId);
        List<CartInfo> cartInfoList = cartInfoMapper.selectByExample(example);

        //存入到Redis缓存中的key
        String key = "cart:" + userId + ":list";

        if (cartInfoList != null && cartInfoList.size() > 0) { //说明查到了数据

            Map<String, String> map = new HashMap<>();

            //将购物车中的商品，保存在hashMap中，key为购物车商品id  value为购物车商品
            for (CartInfo cartInfo : cartInfoList) {
                map.put(cartInfo.getId(), JSON.toJSONString(cartInfo));
            }

            Jedis jedis = redisUtil.getJedis();

            /**
             2、将数据保存在redis中：以hash的数据结构
             总体结构为 key- value
             但是每一个value，又都是key-value结构的
             */
            jedis.del(key); //在进行更新之前，需要删除Redis中的数据，这种增量式的只针对hash(map)这种数据结构
            jedis.hmset(key, map); //保存后的结果为：一个userid在Redis中只有一条数据
            jedis.close(); //Redis连接用完要及时关闭
            

        } else { //说明该用户在购物车中无商品信息

            Jedis jedis = redisUtil.getJedis();
           // Map<String, String> map = new HashMap<>();

            //jedis.hmset(key, map); //将缓存中，该用户的购物车信息设置为空
            jedis.del(key);
            jedis.close();
        }
    }

    @Override
    public List<CartInfo> getCartInfosFromCacheByUserId(String userId) {

        // 声明一个处理后的购物车集合对象
        List<CartInfo> cartInfos = new ArrayList<>();
        Jedis jedis = redisUtil.getJedis();

        //存入到Redis缓存中的key
        String key = "cart:" + userId + ":list";
        List<String> cartList = jedis.hvals(key);//返回的为当时存入的Json字符串的集合

        if (cartList != null && cartList.size() > 0) {

            for (String s : cartList) {
                CartInfo cartInfo = JSON.parseObject(s, CartInfo.class); //将每个JSON字符串，转换为购物车商品对象
                cartInfos.add(cartInfo);
            }
        }
        jedis.close();
        return cartInfos;
    }

    //根据userID从数据中查询购物车数据
    @Override
    public List<CartInfo> getCartInfosFromDbByUserId(String userId) {

        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        return cartInfoMapper.select(cartInfo);
    }

    @Override
    public void updateCartIsChecked(CartInfo cartInfo) {

        Example example = new Example(CartInfo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("userId", cartInfo.getUserId()).andEqualTo("skuId", cartInfo.getSkuId());
        cartInfoMapper.updateByExampleSelective(cartInfo, example);

        //更新完成之后，同步到redis缓存
        flushCartCacheByUserId(cartInfo.getUserId());
    }

    /***
     * 合并购物车方法
     * @param userId
     * @param cartInfoListCookie
     */
    @Override
    public void combine(String userId, List<CartInfo> cartInfoListCookie) {


        CartInfo cartInfoParam = new CartInfo();
        cartInfoParam.setUserId(userId);
        List<CartInfo> cartInfoListDB = cartInfoMapper.select(cartInfoParam);

        lable:for (CartInfo cartInfoCookie : cartInfoListCookie) { //lable标签，用来表示跳过当次循环，进入下一次循环

            if (cartInfoListDB != null && cartInfoListDB.size() > 0) { //说明数据库中有购物车数据

                for (CartInfo infoDB : cartInfoListDB) {

                    if (infoDB.getSkuId().equals(cartInfoCookie.getSkuId())) { //skuId相等，说明存在相同的商品，更新

                        infoDB.setIsChecked(cartInfoCookie.getIsChecked()); //更新勾选请求
                        infoDB.setSkuNum(cartInfoCookie.getSkuNum());   //更新商品数量
                        infoDB.setCartPrice(infoDB.getSkuPrice().multiply(new BigDecimal(infoDB.getSkuNum()))); //更新该商品总价

                        cartInfoMapper.updateByPrimaryKeySelective(infoDB); //执行更新操作

                        continue lable; //更新完成之后，跳出当次循环
                    }
                }
            }
            /**执行到这一步，说明
             * 1、数据库中，没有购物车数据，则直接插入；
             * 2、数据库中购物车有数据，但是没有cookie中的该商品，则直接插入
             */
            cartInfoCookie.setUserId(userId); //特别注意：cookie中购物车的商品是没有userID的，在进行插入操作前，需要先设置userId
            cartInfoMapper.insert(cartInfoCookie);
        }

        //同步刷新Redis缓存中数据
        flushCartCacheByUserId(userId);
    }

    /***
     * 提交订单后，删除购物车中勾选的商品
     * @param cartInfoIdStrings
     */
    @Override
    public void deleteCartInfoAfterOrder(String cartInfoIdStrings,String userId) {

        cartInfoMapper.deleteByCartInfoIds(cartInfoIdStrings);

        //删除之后，同步到Redis中
        flushCartCacheByUserId(userId);

    }


}
