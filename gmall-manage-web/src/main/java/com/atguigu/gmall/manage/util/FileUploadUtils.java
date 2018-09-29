package com.atguigu.gmall.manage.util;

import com.atguigu.gmall.constant.AppConstant;
import com.atguigu.gmall.manage.controller.SpuController;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Created by Administrator on 2018/9/7.
 */
@Component
public class FileUploadUtils {

    //利用@Value 标签可以引用application.properties中的值
    //@Value("${fileServer.url}")
    public static String uploadImge(MultipartFile file) {

         /*
        * 1、初始化，获取FastDFS服务器的地址
        * 2、获取StorageClient对象
        * 3、调用upload_file上传文件
        * 4、返回图片地址
        * */
        String path = SpuController.class.getClassLoader().getResource("tracker.conf").getPath();
        try {
            ClientGlobal.init(path);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }

        //2、获取StorageClient对象
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer = null;
        try {
            trackerServer = trackerClient.getConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StorageClient storageClient = new StorageClient(trackerServer, null);

        //获取文件的扩展名
        String originalFilename = file.getOriginalFilename();
        String extName = originalFilename.substring(originalFilename.indexOf(".") + 1);

        String[] files = new String[0];
        try {
            files = storageClient.upload_file(file.getBytes(), extName, null);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }

        String imgName = AppConstant.FILE_SERVER_URL;
//        String imgName = http://192.168.74.200;
        //拼接图片地址

        String groupName = files[0];  //group1
        String lastFileName = files[1];  //M00/00/00/wKhDo1qjU2qAWKQmAAATla901AQ534.jpg

        imgName = imgName+"/"+groupName+"/"+lastFileName;
        System.out.println( imgName );

        return imgName;
    }
}
