package org.simpleframework.mvc;

import com.imooc.controller.frontend.MainPageController;
import com.imooc.controller.superadmin.HeadLineOperationController;
import org.simpleframework.aop.AspectWeaver;
import org.simpleframework.core.BeanContainer;
import org.simpleframework.inject.DependencyInjector;
import org.simpleframework.mvc.processor.RequestProcessor;
import org.simpleframework.mvc.processor.RequestProcessorChain;
import org.simpleframework.mvc.processor.impl.ControllerRequestProcessor;
import org.simpleframework.mvc.processor.impl.JspRequestProcessor;
import org.simpleframework.mvc.processor.impl.PreRequestProcessor;
import org.simpleframework.mvc.processor.impl.StaticResourceRequestProcessor;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

//@WebServlet("/")
//public class DispatcherServlet extends HttpServlet {
//    @Override
//    public void init(){
//        System.out.println("我是首次请求被处理前执行的, 后续不会再执行");
//    }
//    @Override
//    protected void service(HttpServletRequest req, HttpServletResponse resp) {
//        System.out.println("request path is : " + req.getServletPath());
//        System.out.println("request method is : " + req.getMethod());
//        if (req.getServletPath() == "/frontend/getmainpageinfo" && req.getMethod() == "GET"){
//            new MainPageController().getMainPageInfo(req, resp);
//        } else if(req.getServletPath() == "/superadmin/addheadline" && req.getMethod() == "POST"){
//            new HeadLineOperationController().addHeadLine(req, resp);
//        }
//    }
//}


@WebServlet("/*")
public class DispatcherServlet extends HttpServlet {
    List<RequestProcessor> PROCESSOR = new ArrayList<>();

    @Override
    public void init(){
        // 1. 初始化容器
        BeanContainer beanContainer = BeanContainer.getInstance();
        beanContainer.loadBeans("com.imooc");

        new AspectWeaver().doAop();
        new DependencyInjector().doIoc();

        // 2. 初始化请求处理器责任链
        PROCESSOR.add(new PreRequestProcessor());
        PROCESSOR.add(new StaticResourceRequestProcessor(getServletContext()));
        PROCESSOR.add(new JspRequestProcessor(getServletContext()));
        PROCESSOR.add(new ControllerRequestProcessor());
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) {
        // 1.创建责任链对象实例
        RequestProcessorChain requestProcessorChain = new RequestProcessorChain(PROCESSOR.iterator(), req, resp);

        // 2.通过责任链模式来依次调用请求处理器对请求进行处理
        requestProcessorChain.doRequestProcessorChain();

        // 3.对处理结果进行渲染
        requestProcessorChain.doRender();
    }
}
