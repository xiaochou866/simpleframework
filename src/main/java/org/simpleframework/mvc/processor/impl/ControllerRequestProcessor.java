package org.simpleframework.mvc.processor.impl;

import lombok.extern.slf4j.Slf4j;
import org.simpleframework.core.BeanContainer;
import org.simpleframework.mvc.annotation.RequestMapping;
import org.simpleframework.mvc.annotation.RequestParam;
import org.simpleframework.mvc.annotation.ResponseBody;
import org.simpleframework.mvc.processor.RequestProcessor;
import org.simpleframework.mvc.processor.RequestProcessorChain;
import org.simpleframework.mvc.render.impl.JsonResultRender;
import org.simpleframework.mvc.render.impl.ResourceNotFoundResultRender;
import org.simpleframework.mvc.render.ResultRender;
import org.simpleframework.mvc.render.impl.ViewResultRender;
import org.simpleframework.mvc.type.ControllerMethod;
import org.simpleframework.mvc.type.RequestPathInfo;
import org.simpleframework.util.ConverterUtil;
import org.simpleframework.util.ValidationUtil;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ControllerRequestProcessor implements RequestProcessor {
    //IOC容器
    private BeanContainer beanContainer;

    //请求和controller方法的映射集合
    // 对于ControllerMethod 类中有三个属性 一个是对应的Controller类 一个是对应的方法 一个是该方法的所有参数情况
    private Map<RequestPathInfo, ControllerMethod> pathControllerMethodMap = new ConcurrentHashMap<>(); // RequestPathInfo: 方法请求类型 url地址 value为对应的处理请求的Controller类

    /**
     * 依靠容器的能力，建立起请求路径、请求方法与Controller方法实例的映射
     */
    public ControllerRequestProcessor() {
        this.beanContainer = BeanContainer.getInstance();
        Set<Class<?>> requestMappingSet = beanContainer.getClassesByAnnotation(RequestMapping.class); // 这里获取所有被RequestMapping所标注的类
        initPathControllerMethodMap(requestMappingSet);
    }

    private void initPathControllerMethodMap(Set<Class<?>> requestMappingSet) {
        if (ValidationUtil.isEmpty(requestMappingSet)) {
            return;
        }

        //1. 遍历所有被@RequestMapping标记的类，获取类上面该注解的属性值作为一级路径
        for (Class<?> requestMappingClass : requestMappingSet) {
            RequestMapping requestMapping = requestMappingClass.getAnnotation(RequestMapping.class); // 获取标注在该类上的RequestMapping的注解
            String basePath = requestMapping.value();
            if (!basePath.startsWith("/")) { // 对requestMapping中的路径进行修正 比如 add -> /add
                basePath = "/" + basePath;
            }

            //2.遍历类里所有被@RequestMapping标记的方法，获取方法上面该注解的属性值，作为二级路径
            Method[] methods = requestMappingClass.getDeclaredMethods(); // 通过反射的方式获取这个被注解类的所有方法
            if (ValidationUtil.isEmpty(methods)) { // 如果该类没有任何方法 则开始处理下一个类
                continue;
            }

            for (Method method : methods) { // 遍历该类中的所有方法
                if (method.isAnnotationPresent(RequestMapping.class)) { // 如果该类的这个某个方法上有requestMapping这个注解的话 进行获取形成二级路径
                    RequestMapping methodRequest = method.getAnnotation(RequestMapping.class);
                    String methodPath = methodRequest.value(); // 访问路径的二级名
                    if (!methodPath.startsWith("/")) {
                        methodPath = "/" + basePath;
                    }
                    String url = basePath + methodPath; // 这个url就是最终的访问路径

                    //3.解析方法里被@RequestParam标记的参数，
                    // 获取该注解的属性值，作为参数名，
                    // 获取被标记的参数的数据类型，建立参数名和参数类型的映射
                    Map<String, Class<?>> methodParams = new HashMap<>();
                    Parameter[] parameters = method.getParameters();
                    if (!ValidationUtil.isEmpty(parameters)) {
                        for (Parameter parameter : parameters) {
                            RequestParam param = parameter.getAnnotation(RequestParam.class);
                            //目前暂定为Controller方法里面所有的参数都需要@RequestParam注解 这里简化了实现
                            if (param == null) {
                                throw new RuntimeException("The parameter must have @RequestParam");
                            }
                            methodParams.put(param.value(), parameter.getType()); // key:为注解上标注的参数的名称 value:为从真实的parameter上获取到的参数的类型
                        }
                    }
                    // 以上的处理流程 就是先从类开始处理 再处理类的方法 然后再处理类中方法的参数

                    //4.将获取到的信息封装成RequestPathInfo实例和ControllerMethod实例，放置到映射表里
                    String httpMethod = String.valueOf(methodRequest.method()); // 从方法的requestMapping的注解中 获取请求的方式
                    RequestPathInfo requestPathInfo = new RequestPathInfo(httpMethod, url); // 将请求方法 映射 到指定的二级路径 并创建ResultPathInfo 这里相当于是 httpMethod: "POST" url:"/headline/add"

                    if (this.pathControllerMethodMap.containsKey(requestPathInfo)) {
                        log.warn("duplicate url:{} registration，current class {} method{} will override the former one",
                                requestPathInfo.getHttpPath(), requestMappingClass.getName(), method.getName());
                    }

                    ControllerMethod controllerMethod = new ControllerMethod(requestMappingClass, method, methodParams); // 可以看上面那几个循环 就大概清楚controllerMethod是什么意思了
                    this.pathControllerMethodMap.put(requestPathInfo, controllerMethod); // 后面那个相当于做饭用的材料一样 用来处理前面那个发过来的请求
                }
            }
        }
    }

    @Override
    public boolean process(RequestProcessorChain requestProcessorChain) throws Exception {
        //1.解析HttpSevletRequest的请求方法,请求路径，获取对应的ControllerMethod实例
        String method = requestProcessorChain.getRequestMethod();
        String path = requestProcessorChain.getRequestPath();

        ControllerMethod controllerMethod = this.pathControllerMethodMap.get(new RequestPathInfo(method, path));
        if (controllerMethod == null) { // 如果该请求方法以及路径 没有对应的Controller类进行处理的话 就将Render设置为ResourceNotFoundResultRender
            requestProcessorChain.setResultRender(new ResourceNotFoundResultRender(method, path));
            return false;
        }

        //2.解析请求参数，并传递给获取到的ControllerMethod实例去执行
        Object result = invokeControllerMethod(controllerMethod, requestProcessorChain.getRequest()); // controllerMethod中存储有将要处理请求方法的Controller类 方法 以及 方法所需要的参数情况 第二个参数是发送过来的请求情况
        //3.根据处理的结果，选择对应的render进行渲染
        setResultRender(result, controllerMethod, requestProcessorChain); // 根据请求的方法上是不是有@ResponseBody注解 去选择合适的Render对结果进行处理
        return true;
    }


    /**
     * 根据不同情况设置不同的渲染器
     */
    private void setResultRender(Object result, ControllerMethod controllerMethod, RequestProcessorChain requestProcessorChain) {
        if (result == null) {
            return;
        }
        ResultRender resultRender;
        boolean isJson = controllerMethod.getInvokeMethod().isAnnotationPresent(ResponseBody.class); // 如果controller类中的处理方法上有ResponseBody注解的话 就将Render赋值为JsonResultRender
        if (isJson) {
            resultRender = new JsonResultRender(result);
        } else {
            resultRender = new ViewResultRender(result);
        }
        requestProcessorChain.setResultRender(resultRender);
    }

    private Object invokeControllerMethod(ControllerMethod controllerMethod, HttpServletRequest request) {
        //1.从请求里获取GET或者POST的参数名及其对应的值
        Map<String, String> requestParamMap = new HashMap<>();

        //GET，POST方法的请求参数获取方式 由parameterMap去构造requestParamMap
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> parameter : parameterMap.entrySet()) {
            if (!ValidationUtil.isEmpty(parameter.getValue())) {
                //只支持一个参数对应一个值的形式
                requestParamMap.put(parameter.getKey(), parameter.getValue()[0]);
            }
        }

        //2.根据获取到的请求参数名及其对应的值，以及controllerMethod里面的参数和类型的映射关系，去实例化出方法对应的参数
        List<Object> methodParams = new ArrayList<>();
        Map<String, Class<?>> methodParamMap = controllerMethod.getMethodParameters();

        for (String paramName : methodParamMap.keySet()) {
            Class<?> type = methodParamMap.get(paramName);
            String requestValue = requestParamMap.get(paramName);
            Object value;
            //只支持String 以及基础类型char,int,short,byte,double,long,float,boolean,及它们的包装类型
            if (requestValue == null) {
                //将请求里的参数值转成适配于参数类型的空值
                value = ConverterUtil.primitiveNull(type);
            } else {
                value = ConverterUtil.convert(type, requestValue);
            }
            methodParams.add(value);
        }

        //3.执行Controller里面对应的方法并返回结果
        Object controller = beanContainer.getBean(controllerMethod.getControllerClass());
        Method invokeMethod = controllerMethod.getInvokeMethod();
        invokeMethod.setAccessible(true);
        Object result;
        try {
            if (methodParams.size() == 0) {
                result = invokeMethod.invoke(controller);
            } else {
                result = invokeMethod.invoke(controller, methodParams.toArray());
            }
        } catch (InvocationTargetException e) {
            //如果是调用异常的话，需要通过e.getTargetException()
            // 去获取执行方法抛出的异常
            throw new RuntimeException(e.getTargetException());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}
