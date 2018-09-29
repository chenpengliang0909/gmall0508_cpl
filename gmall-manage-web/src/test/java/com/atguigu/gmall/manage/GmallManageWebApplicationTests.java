package com.atguigu.gmall.manage;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallManageWebApplicationTests {

	@Test
	public void contextLoads() throws IOException, MyException {

		//读取tracker.cof配置文件

        //通过类加载器，可以得到一个绝对的物理地址
        //如：【E:\atguigu_Java_install\workspace\workspace_scw\Proj9_FastDFS\src\test\java】
        String path = GmallManageWebApplicationTests.class.getClassLoader().getResource("tracker.conf").getPath();

        // 进行初始化
		ClientGlobal.init(path);

		//创建fafs的客户端
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer = trackerClient.getConnection();
        StorageClient storageClient = new StorageClient(trackerServer, null);

        //图片上传后，会返回图片的访问地址
        String[] files = storageClient.upload_file("d:/1.jpg", "jpg", null);
        //linux的地址
        String ImgURL="http://192.168.74.200";

        //拼接上传图片的地址
        for (String file : files) {
            ImgURL= ImgURL+"/"+file;
        }
        System.out.println( ImgURL );

    }

}
