package com.xuecheng.manage_course.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * @author Administrator
 * @version 1.0
 **/
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestRibbon {

    @Autowired
    RestTemplate restTemplate;
    @Autowired
    CourseBaseRepository courseBaseRepository;
    @Test
    public void testRibbon(){
//      确定要获取的服务名
        String serviceId = "XC-SERVICE-MANAGE-CMS";
        for (int i = 0; i < 10; i++) {
            //      ribbon客户端从eurekaServer中获取服务列表
            ResponseEntity<Map> forEntity = restTemplate.getForEntity("http://"+serviceId+"/cms/page/get/5ad94b9168db5243ec846e8e", Map.class);
            Map map = forEntity.getBody();
            System.out.println(map);
        }

    }



}
