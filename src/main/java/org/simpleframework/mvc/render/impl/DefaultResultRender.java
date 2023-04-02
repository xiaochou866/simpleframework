package org.simpleframework.mvc.render.impl;

import org.simpleframework.mvc.processor.RequestProcessorChain;
import org.simpleframework.mvc.render.ResultRender;

public class DefaultResultRender implements ResultRender {
    @Override
    public void render(RequestProcessorChain requestProcessorChain) throws Exception {
        // 从处理链中获取响应对象 并设置该响应对象的status 为该响应链中的ResponseCode
        requestProcessorChain.getResponse().setStatus(requestProcessorChain.getResponseCode());
    }
}
