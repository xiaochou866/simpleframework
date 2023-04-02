package org.simpleframework.mvc.render.impl;

import org.simpleframework.mvc.processor.RequestProcessorChain;
import org.simpleframework.mvc.render.ResultRender;

import javax.servlet.http.HttpServletResponse;

public class InternalErrorResultRender implements ResultRender {
    private String errorMsg;
    public InternalErrorResultRender(String  errorMsg){
        this.errorMsg = errorMsg;
    }
    @Override
    public void render(RequestProcessorChain requestProcessorChain) throws Exception {
        requestProcessorChain.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMsg); // 为响应设置服务器内部错误 并设置对应的错误信息
    }
}
