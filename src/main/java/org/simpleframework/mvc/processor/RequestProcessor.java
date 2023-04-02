package org.simpleframework.mvc.processor;

public interface RequestProcessor {
    boolean process(RequestProcessorChain requestProcessorChain) throws Exception;
}

// 手写spring MVC相关代码
//https://www.cnblogs.com/tc971121/p/13491586.html