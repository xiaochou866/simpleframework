package org.simpleframework.mvc.processor.impl;

import lombok.extern.slf4j.Slf4j;
import org.simpleframework.mvc.processor.RequestProcessor;
import org.simpleframework.mvc.processor.RequestProcessorChain;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;

/**
 * 静态资源请求处理，包括但不限于图片、css以及js文件等
 */
@Slf4j
public class StaticResourceRequestProcessor implements RequestProcessor {
    public static final String DEFAULT_TOMCAT_SERVLET = "default";
    public static final String STATIC_RESOURCE_PREFIX = "/static/";

    // tomcat默认请求派发器RequestDispatcher的名称
    RequestDispatcher defaultDisptcher;

    public StaticResourceRequestProcessor(ServletContext servletContext){
        this.defaultDisptcher = servletContext.getNamedDispatcher(DEFAULT_TOMCAT_SERVLET);
        if(this.defaultDisptcher==null){
            throw new RuntimeException("There is no default tomcat servlet");
        }
        log.info("The default servlet for static resource is {}", DEFAULT_TOMCAT_SERVLET);
    }

    @Override
    public boolean process(RequestProcessorChain requestProcessorChain) throws Exception {
        // 1.通过请求路径判断是否是请求的静态资源 webapp/static
        if(isStaticResource(requestProcessorChain.getRequestPath())){
            // 2.如果是静态资源，则将请求转发给default servlet处理
            defaultDisptcher.forward(requestProcessorChain.getRequest(), requestProcessorChain.getResponse());
            return false; // 这里说明该请求已经处理完毕了 不需要接着给下一个requestProcessor去进行处理
        }
        return true;
    }

    // 通过请求路径前缀(目录)是否为静态资源 /static/
    private boolean isStaticResource(String path){
        return path.startsWith(STATIC_RESOURCE_PREFIX);
    }
}
