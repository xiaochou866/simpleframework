package org.simpleframework.aop.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Aspect {
    /**
     * 需要被织入横切逻辑的注解标签
     */
    //Class<? extends Annotation> value(); // 用于aspect指定的目标
    String pointcut();
    // “execution(* com.imooc.controller.frontend..*.*(..))” 以及 within(com.imooc.controller.frontend.*)
}
