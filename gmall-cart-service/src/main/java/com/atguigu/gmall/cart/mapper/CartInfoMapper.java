package com.atguigu.gmall.cart.mapper;

import com.atguigu.gmall.bean.CartInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

/**
 * Created by Administrator on 2018/9/16.
 */
public interface CartInfoMapper extends Mapper<CartInfo> {


    void deleteByCartInfoIds(@Param("cartInfoIdStrings") String cartInfoIdStrings);
}
