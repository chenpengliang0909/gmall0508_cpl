package com.atguigu.gmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

/*
* 说明：MapperScan用于扫描mapper，所有扫描的为mapper接口所在的包
* 此外，该注解所在的包为【tk】
* */
@SpringBootApplication
@MapperScan(basePackages = "com.atguigu.gmall.manage.mapper")
public class GmallManageServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GmallManageServiceApplication.class, args);
	}
}
