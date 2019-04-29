package com.xuecheng.manage_course.service;


import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.response.AddCourseResult;
import com.xuecheng.framework.domain.course.response.CourseCode;
import com.xuecheng.framework.domain.course.response.CoursePublishResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.client.CmsPageClient;
import com.xuecheng.manage_course.dao.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    TeachplanMapper teachplanMapper;
    @Autowired
    CourseBaseRepository courseBaseRepository;
    @Autowired
    TeachplanRepository teachplanRepository;
    @Autowired
    CourseMarketRepository courseMarketRepository;
    @Autowired
    CoursePicRepository coursePicRepository;
    @Autowired
    CmsPageClient cmsPageClient;
    CoursePubRepository coursePubRepository;


    @Value("${course-publish.dataUrlPre}")
    private String publish_dataUrlPre;
    @Value("${course-publish.pagePhysicalPath}")
    private String publish_page_physicalpath;
    @Value("${course-publish.pageWebPath}")
    private String publish_page_webpath;
    @Value("${course-publish.siteId}")
    private String publish_siteId;
    @Value("${course-publish.templateId}")
    private String publish_templateId;
    @Value("${course-publish.previewUrl}")
    private String previewUrl;

    /**
     * 查询课程计划
     * @param courseId
     * @return
     */
    public TeachplanNode findTeachplanList(String courseId){
       TeachplanNode teachplanNode = teachplanMapper.selectList(courseId);
        return teachplanNode;
    }


    /**
     * 获取课程根节点，如果没有则添加根节点
     * @param courseId
     * @return
     */
    public String getTeachplanRoot(String courseId){
//        校验课程id
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if (!optional.isPresent()){
            return  null;
        }
        CourseBase courseBase = optional.get();
//        取出课程计划根节点（一级结点）
        List<Teachplan> teachplanList = teachplanRepository.findByCourseidAndParentid(courseId, "0");
        if (teachplanList==null || teachplanList.size()<=0){
//            新增一个根节点
            Teachplan teachplanRoot = new Teachplan();
            teachplanRoot.setCourseid(courseId);
            teachplanRoot.setPname(courseBase.getName());
            teachplanRoot.setParentid("0");
//            1级
            teachplanRoot.setGrade("1");
//            未发布
            teachplanRoot.setStatus("0");
            teachplanRepository.save(teachplanRoot);
            return teachplanRoot.getId();
        }
        return teachplanList.get(0).getId();
    }


    /**
     * 添加课程计划
     * @param teachplan
     * @return
     */
    @Transactional
    public ResponseResult addTeachplan(Teachplan teachplan){
//        检验课程id和课程计划名称
        if (teachplan==null || StringUtils.isEmpty(teachplan.getCourseid()) || StringUtils.isEmpty(teachplan.getPname())){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
//        取出课程id
        String courseid = teachplan.getCourseid();
//        取出父节点id
        String parentid = teachplan.getParentid();
        if (StringUtils.isEmpty(parentid)){
//            如果父节点id为空则获取根节点
             parentid = getTeachplanRoot(courseid);
        }
//        取出父节点信息
        Optional<Teachplan> optional = teachplanRepository.findById(parentid);
        if (!optional.isPresent()){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
//        父节点
        Teachplan teachplanParent = optional.get();
//        父节点级别
        String parentGrade = teachplanParent.getGrade();
//        创建一个新节点准备添加
        Teachplan teachplan1 = new Teachplan();
//        将teachplan的属性拷贝到teachplan1中
        BeanUtils.copyProperties(teachplan,teachplan1);
//        设置父节点
        teachplan1.setParentid(parentid);
//        未发布
        teachplan1.setStatus("0");
//      子节点的级别,根据父节点来判断
        if (parentGrade.equals("1")){
            teachplan1.setGrade("2");
        }else {
            teachplan1.setGrade("3");
        }
//        设置课程id
        teachplan1.setCourseid(teachplanParent.getCourseid());
        teachplanRepository.save(teachplan1);
        return new ResponseResult(CommonCode.SUCCESS);
    }


    /**
     * 添加课程提交
     * @param courseBase
     * @return
     */
    @Transactional
    public AddCourseResult addCourseBase(CourseBase courseBase){
//        课程状态默认认为未发布
        courseBase.setStatus("202001");
        courseBaseRepository.save(courseBase);
        return new AddCourseResult(CommonCode.SUCCESS,courseBase.getId());
    }


    public CourseBase getCoursebaseById(String courseId){
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if (optional.isPresent()){
            return optional.get();
        }
        return null;
    }


    @Transactional
    public ResponseResult updateCoursebase(String courseId, CourseBase courseBase){
        CourseBase one = this.getCoursebaseById(courseId);
        if (one == null){
//            抛出异常
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
//        修改课程信息
        one.setName(courseBase.getName());
        one.setMt(courseBase.getMt());
        one.setSt(courseBase.getSt());
        one.setGrade(courseBase.getGrade());
        one.setStudymodel(courseBase.getStudymodel());
        one.setUsers(courseBase.getUsers());
        one.setDescription(courseBase.getDescription());
        CourseBase save = courseBaseRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);

    }


    /**
     * 根据id查询课程营销信息
     * @param courseId
     * @return
     */
    public CourseMarket  getCourseMarketById(String courseId){
        Optional<CourseMarket> optional = courseMarketRepository.findById(courseId);
        if (optional.isPresent()){
            return optional.get();
        }
        return null;
    }


    /**
     * 更新课程营销信息
     * @param courseId
     * @param courseMarket
     * @return
     */
    @Transactional
    public CourseMarket updateCourseMarket(String courseId, CourseMarket courseMarket){
        CourseMarket one = this.getCourseMarketById(courseId);
        if (one!=null){
            one.setCharge(courseMarket.getCharge());
            //课程有效期，开始时间
        one.setStartTime(courseMarket.getStartTime());
            //课程有效期，结束时间
        one.setEndTime(courseMarket.getEndTime());
        one.setPrice(courseMarket.getPrice());
        one.setQq(courseMarket.getQq());
        one.setValid(courseMarket.getValid());
        courseMarketRepository.save(one);
        }else {
//            添加课程营销信息
            one = new CourseMarket();
//            设置课程id
            one.setId(courseId);
            courseMarketRepository.save(one);
        }
        return one;
    }


    @Transactional
     public ResponseResult saveCoursePic(String courseId,String pic){
//        查询课程图片
        Optional<CoursePic> picOptional = coursePicRepository.findById(courseId);
        CoursePic coursePic = null;
        if (picOptional.isPresent()){
            coursePic = picOptional.get();
        }
//        没有课程图片则新建对象
        if (coursePic == null){
            coursePic = new CoursePic();
        }
        coursePic.setCourseid(courseId);
        coursePic.setPic(pic);
//        保存课程图片
        coursePicRepository.save(coursePic);
        return new ResponseResult(CommonCode.SUCCESS);
    }


    /**
     * 根据课程id查询课程图片
     * @param courseId
     * @return
     */
    public CoursePic findCoursepic(String courseId){
//        查询课程图片
        Optional<CoursePic> picOptional = coursePicRepository.findById(courseId);
        if (picOptional.isPresent()){
            CoursePic coursePic = picOptional.get();
            return coursePic;
        }
        return null;
    }


    @Transactional
    public ResponseResult deleteCoursePic(String courseId){
//        执行删除,返回1表示删除成功，返回0表示删除失败
        long result = coursePicRepository.deleteByCourseid(courseId);
        if (result>0){
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }


    /**
     * 根据id查询课程视图
     * @param id
     * @return
     */
    public CourseView getCourseView(String id){
        CourseView courseView = new CourseView();
//        查询课程基本信息
        Optional<CourseBase> optional = courseBaseRepository.findById(id);
        if (optional.isPresent()){
            CourseBase courseBase = optional.get();
            courseView.setCourseBase(courseBase);
        }
//        查询课程营销信息
        Optional<CourseMarket> courseMarketOptional = courseMarketRepository.findById(id);
        if (courseMarketOptional.isPresent()){
            CourseMarket courseMarket = courseMarketOptional.get();
            courseView.setCourseMarket(courseMarket);
        }
//        查询课程图片信息
        Optional<CoursePic> coursePicOptional = coursePicRepository.findById(id);
        if (coursePicOptional.isPresent()){
            CoursePic coursePic = coursePicOptional.get();
            courseView.setCoursePic(coursePic);
        }
//        查询课程计划信息
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        courseView.setTeachplanNode(teachplanNode);
        return courseView;
    }

    /**
     * 课程预览
     * @param id
     * @return
     */
    public CoursePublishResult preview(String id){
//        查询课程
        CourseBase courseBaseById = this.findCourseBaseById(id);
//        请求cms添加页面
//        准备cmsPage信息
        CmsPage cmsPage = new CmsPage();
//        站点id
        cmsPage.setSiteId(publish_siteId);
//        数据模型url
        cmsPage.setDataUrl(publish_dataUrlPre);
//        页面名称
        cmsPage.setPageName(id+".html");
//        页面别名，就是课程名称
        cmsPage.setPageAliase(courseBaseById.getName());
//        页面物理路径
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
//        页面webpath
        cmsPage.setPageWebPath(publish_page_webpath);
//        页面模板id
        cmsPage.setTemplateId(publish_templateId);
//        远程调用cms
        CmsPageResult cmsPageResult = cmsPageClient.save(cmsPage);
        if (!cmsPageResult.isSuccess()){
            return  new CoursePublishResult(CommonCode.FAIL,null);
        }
        CmsPage cmsPage1 = cmsPageResult.getCmsPage();
        String pageId = cmsPage1.getPageId();
//        拼装页面预览的url
        String url = previewUrl + pageId;
//        返回CoursePublishResult对象(当中包含页面预览的url)
        return new CoursePublishResult(CommonCode.SUCCESS,url);

    }


    /**
     * 根据id查询课程基本信息
     * @param courseId
     * @return
     */
    public CourseBase findCourseBaseById(String courseId){
        Optional<CourseBase> baseOptional = courseBaseRepository.findById(courseId);
        if(baseOptional.isPresent()){
            CourseBase courseBase = baseOptional.get();
            return courseBase;
        }
        ExceptionCast.cast(CourseCode.COURSE_DENIED_DELETE);
        return null;
    }


    /**
     * 课程发布
     * @param courseId
     * @return
     */
    @Transactional
    public CoursePublishResult publish(String courseId){

        //查询课程
        CourseBase courseBaseById = this.findCourseBaseById(courseId);

        //准备页面信息
        CmsPage cmsPage = new CmsPage();
        //站点id
        cmsPage.setSiteId(publish_siteId);
        //数据模型url
        cmsPage.setDataUrl(publish_dataUrlPre+courseId);
        //页面名称
        cmsPage.setPageName(courseId+".html");
        //页面别名，就是课程名称
        cmsPage.setPageAliase(courseBaseById.getName());
        //页面物理路径
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        //页面webpath
        cmsPage.setPageWebPath(publish_page_webpath);
        //页面模板id
        cmsPage.setTemplateId(publish_templateId);
//        调用cms一键发布接口将课程详情页面发布到服务器
        CmsPostPageResult cmsPostPageResult = cmsPageClient.postPageQuick(cmsPage);
        if (!cmsPostPageResult.isSuccess()){
                return new CoursePublishResult(CommonCode.FAIL,null);
        }
//        保存课程的发布状态为“已发布”
        CourseBase courseBase = this.saveCoursePubState(courseId);
        if (courseBase == null){
            return new CoursePublishResult(CommonCode.FAIL,null);
        }

        //保存课程索引信息
//        先创建一个coursePub对象
        CoursePub coursePub = createCoursePub(courseId);
//        将coursePub对象保存到数据库
        saveCoursePub(courseId,coursePub);

        //缓存课程的信息
        //...
        //得到页面的url
        String pageUrl = cmsPostPageResult.getPageUrl();
        return new CoursePublishResult(CommonCode.SUCCESS,pageUrl);
    }

    /**
     * 更新课程状态为已发布 202002
     * @param courseId
     * @return
     */
    private CourseBase saveCoursePubState(String courseId) {
        CourseBase courseBaseById = this.findCourseBaseById(courseId);
        courseBaseById.setStatus("202002");
        courseBaseRepository.save(courseBaseById);
        return courseBaseById;
    }

    public CoursePub saveCoursePub(String id,CoursePub coursePub){
        if (StringUtils.isNotEmpty(id)){
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_COURSEIDISNULL);
        }
        CoursePub coursePubNew = null;
//        根据id查询查询coursePub
        Optional<CoursePub> coursePubOptional = coursePubRepository.findById(id);
        if (coursePubOptional.isPresent()){
            coursePubNew = coursePubOptional.get();
        }
        if (coursePubNew == null){
             coursePubNew = new CoursePub();
        }
//        将coursePub对象中的信息保存到coursePubNew中
        BeanUtils.copyProperties(coursePub,coursePubNew);
//        设置主键
        coursePubNew.setId(id);
//        更新时间戳为最新事件
        coursePubNew.setTimestamp(new Date());
//        发布时间
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String date = simpleDateFormat.format(new Date());
        coursePubNew.setPubTime(date);
        coursePubRepository.save(coursePubNew);
        return coursePubNew;
    }

    /**
     * 创建coursePub对象
     * @param id
     * @return
     */
    private CoursePub createCoursePub(String id){
        CoursePub coursePub = new CoursePub();
        //根据课程id查询course_base
        Optional<CourseBase> baseOptional = courseBaseRepository.findById(id);
        if(baseOptional.isPresent()){
            CourseBase courseBase = baseOptional.get();
            //将courseBase属性拷贝到CoursePub中
            BeanUtils.copyProperties(courseBase,coursePub);
        }

        //查询课程图片
        Optional<CoursePic> picOptional = coursePicRepository.findById(id);
        if(picOptional.isPresent()){
            CoursePic coursePic = picOptional.get();
            BeanUtils.copyProperties(coursePic, coursePub);
        }

        //课程营销信息
        Optional<CourseMarket> marketOptional = courseMarketRepository.findById(id);
        if(marketOptional.isPresent()){
            CourseMarket courseMarket = marketOptional.get();
            BeanUtils.copyProperties(courseMarket, coursePub);
        }

        //课程计划信息
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        String jsonString = JSON.toJSONString(teachplanNode);
        //将课程计划信息json串保存到 course_pub中
        coursePub.setTeachplan(jsonString);
        return coursePub;

    }

    }


