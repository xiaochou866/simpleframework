package org.simpleframework.aop;

import org.simpleframework.aop.annotation.Aspect;
import org.simpleframework.aop.annotation.Order;
import org.simpleframework.aop.aspect.AspectInfo;
import org.simpleframework.aop.aspect.DefaultAspect;
import org.simpleframework.core.BeanContainer;
import org.simpleframework.util.ValidationUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.*;

public class AspectWeaver {
    private BeanContainer beanContainer;

    public AspectWeaver(){
        this.beanContainer = BeanContainer.getInstance();
    }

    public void doAop(){
        // 1.获取所有的切面类 也就是获取所有的被@aspect注解标识的类对象
        Set<Class<?>> aspectSet = beanContainer.getClassesByAnnotation(Aspect.class);
        // 2.将切面类按照不同的织入目标进行切分 这里测试的时候分为针对controller的aspect和针对service的aspect
        Map<Class<? extends Annotation>, List<AspectInfo>> categorizedMap = new HashMap<>();
        if(ValidationUtil.isEmpty(aspectSet)){
            return;
        }
        for(Class<?> aspectClass: aspectSet){ // 获取到带有aspect注解的所有的类 遍历并进行操作
            if(verifyAspect(aspectClass)){
                categorizeAspect(categorizedMap, aspectClass);
            }else{
                throw new RuntimeException("@Aspect and @Order have not been added to the Aspect class, "+
                        "or Aspect class does not extend from DefaultAspect, or the value in Aspect Tag equals @Aspect");
            }
        }
        // 3.按照不同的织入目标分别去织入Aspect的逻辑
        if(ValidationUtil.isEmpty(categorizedMap)){return;}
        for(Class<? extends Annotation> category: categorizedMap.keySet()){
            waveByCategory(category, categorizedMap.get(category));
        }
    }

    private void waveByCategory(Class<? extends Annotation> category, List<AspectInfo> aspectInfoList) {
        //1.获取被代理类的集合
        Set<Class<?>>classSet = beanContainer.getClassesByAnnotation(category);
        if(ValidationUtil.isEmpty(classSet)){return;}
        //2.遍历被代理类，分别为每个被代理类生成动态代理实例
        for(Class<?> targetClass: classSet){
            // 创建动态代理对象
            AspectListExecutor aspectListExecutor = new AspectListExecutor(targetClass, aspectInfoList);
            Object proxyBean = ProxyCreator.createProxy(targetClass, aspectListExecutor);
            //3.将动态代理对象实例添加到容器中，取代未被代理前的类的实例
            beanContainer.addBean(targetClass, proxyBean);
        }
    }

    // 2.将切面类按照不同的织入目标进行切分
    private void categorizeAspect(Map<Class<? extends Annotation>, List<AspectInfo>> categorizedMap, Class<?> aspectClass) {
        Order orderTag = aspectClass.getAnnotation(Order.class); // 获取该切面类的优先级
        Aspect aspectTag = aspectClass.getAnnotation(Aspect.class); // 获取该切面类所针对的目标的类型
        DefaultAspect aspect = (DefaultAspect) beanContainer.getBean(aspectClass);
        AspectInfo aspectInfo = new AspectInfo(orderTag.value(), aspect);
        if(!categorizedMap.containsKey(aspectTag.value())){
            // 如果织入的joinpoint第一次出现，则以该joinpoint为key,以新创建的List<AspectInfo>为value
            List<AspectInfo> aspectInfoList = new ArrayList<>();
            aspectInfoList.add(aspectInfo);
            categorizedMap.put(aspectTag.value(), aspectInfoList); // 这里相当于是将针对于congroller的切面 和针对于service的切面 都 放到hashMap中
        }else{
            // 如果织入的joinpoint不是第一次出现，则往joinpoint对应的value里添加新的Aspect逻辑
            List<AspectInfo> aspectInfoList = categorizedMap.get(aspectTag.value());
            aspectInfoList.add(aspectInfo);
        }
    }

    // 框架中一定要遵守给Aspect类添加@Aspect和@Order标签的规范, 同时, 必须继承自DefaultAspect.class
    // 此外, @Aspect的属性值不能是它本身
    private boolean verifyAspect(Class<?> aspectClass){
        return aspectClass.isAnnotationPresent(Aspect.class) && // 这个声明切面的类必须有Aspect注解
                aspectClass.isAnnotationPresent(Order.class) && // 这个声明切面的类必须有Order注解
                DefaultAspect.class.isAssignableFrom(aspectClass) && // 这个声明的切面的类必须是DefaultAspect的子类
                aspectClass.getAnnotation(Aspect.class).value()!=Aspect.class; //
    }
}
