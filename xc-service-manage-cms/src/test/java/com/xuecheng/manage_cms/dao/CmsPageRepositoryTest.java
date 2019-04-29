package com.xuecheng.manage_cms.dao;


import com.xuecheng.framework.domain.cms.CmsPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CmsPageRepositoryTest {


    @Autowired
    CmsPageRepository cmsPageRepository;

    /**
     * 测试查询所有方法
     */
    @Test
    public void testFindAll(){
        List<CmsPage> cmsPageList = cmsPageRepository.findAll();
        for (CmsPage cmsPage : cmsPageList) {
            System.out.println(cmsPage);
        }
    }

}
