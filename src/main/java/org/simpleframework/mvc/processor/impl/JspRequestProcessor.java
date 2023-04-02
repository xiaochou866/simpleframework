package org.simpleframework.mvc.processor.impl;

import org.simpleframework.mvc.processor.RequestProcessor;
import org.simpleframework.mvc.processor.RequestProcessorChain;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;

public class JspRequestProcessor implements RequestProcessor {
    //jsp请求的RequestDispatcher的名称
    private static final String JSP_SERVLET = "jsp";
    //Jsp请求资源路径前缀
    private static final String  JSP_RESOURCE_PREFIX = "/templates/"; // 这里存储着jsp文件 类似于图片 css等静态资源存储在static文件夹中

    /**
     * jsp的RequestDispatcher,处理jsp资源
     */
    private RequestDispatcher jspServlet;

    public JspRequestProcessor(ServletContext servletContext) {
        jspServlet = servletContext.getNamedDispatcher(JSP_SERVLET);
        if (null == jspServlet) {
            throw new RuntimeException("there is no jsp servlet");
        }
    }

    @Override
    public boolean process(RequestProcessorChain requestProcessorChain) throws Exception {
        if (isJspResource(requestProcessorChain.getRequestPath())) {
            jspServlet.forward(requestProcessorChain.getRequest(), requestProcessorChain.getResponse());
            return false;
        }
        return true;
    }

    /**
     * 是否请求的是jsp资源
     */
    private boolean isJspResource(String url) {
        return url.startsWith(JSP_RESOURCE_PREFIX);
    }
}
