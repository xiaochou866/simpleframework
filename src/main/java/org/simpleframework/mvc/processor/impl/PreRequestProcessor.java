package org.simpleframework.mvc.processor.impl;

import lombok.extern.slf4j.Slf4j;
import org.simpleframework.mvc.processor.RequestProcessor;
import org.simpleframework.mvc.processor.RequestProcessorChain;

/**
 * 请求预处理，包括编码以及路径处理
 */
@Slf4j
public class PreRequestProcessor implements RequestProcessor {
    @Override
    public boolean process(RequestProcessorChain requestProcessorChain) throws Exception {
        // 1.设置请求编码，将其统一设置成UTF-8
        requestProcessorChain.getRequest().setCharacterEncoding("UTF-8");

        // 2.将请求路径末尾的/剔除，为后续匹配Controller请求路径做准备
        // （一般Controller的处理路径是/aaa/bbb，所以如果传入的路径结尾是/aaa/bbb/，
        // 就需要处理成/aaa/bbb）
        String requestPath = requestProcessorChain.getRequestPath();

        //http://localhost:8080/simpleframework requestPath="/"
        if(requestPath.length() > 1 && requestPath.endsWith("/")){
            requestProcessorChain.setRequestPath(requestPath.substring(0, requestPath.length() - 1)); // 这里substring相当于是左闭右开区间就是为了去掉最右侧的/
        }

        /**
         * 这里记录的日志信息举例
         * org.simpleframework.mvc.processor.impl.PreRequestProcessor 20:41:14 -- INFO -- preprocess request GET /headline/remove
         */
        log.info("preprocess request {} {}", requestProcessorChain.getRequestMethod(), requestProcessorChain.getRequestPath());
        return true; // 这里标识着肯定会走到下一个RequestProgressor去进行处理
    }
}
