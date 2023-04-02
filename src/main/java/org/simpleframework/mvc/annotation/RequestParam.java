package org.simpleframework.mvc.annotation;

import org.junit.jupiter.api.condition.EnabledForJreRange;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {
    // 方法参数名称
    String value() default "";
    boolean required() default true;
}
