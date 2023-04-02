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

    // //1.0版本
    //public void doAop(){
    //    // 1.获取所有的切面类 也就是获取所有的被@aspect注解标识的类对象
    //    Set<Class<?>> aspectSet = beanContainer.getClassesByAnnotation(Aspect.class);
    //    // 2.将切面类按照不同的织入目标进行切分 这里测试的时候分为针对controller的aspect和针对service的aspect
    //    Map<Class<? extends Annotation>, List<AspectInfo>> categorizedMap = new HashMap<>();
    //    if(ValidationUtil.isEmpty(aspectSet)){
    //        return;
    //    }
    //    for(Class<?> aspectClass: aspectSet){ // 获取到带有aspect注解的所有的类 遍历并进行操作
    //        if(verifyAspect(aspectClass)){
    //            categorizeAspect(categorizedMap, aspectClass);
    //        }else{
    //            throw new RuntimeException("@Aspect and @Order have not been added to the Aspect class, "+
    //                    "or Aspect class does not extend from DefaultAspect, or the value in Aspect Tag equals @Aspect");
    //        }
    //    }
    //    // 3.按照不同的织入目标分别去织入Aspect的逻辑
    //    if(ValidationUtil.isEmpty(categorizedMap)){return;}
    //    for(Class<? extends Annotation> category: categorizedMap.keySet()){
    //        waveByCategory(category, categorizedMap.get(category));
    //    }
    //}

     //2.0版本 使用Aspectj解析表达式进行更加精确的匹配
    public void doAop(){
        // 1.获取所有的切面类 也就是获取所有的被@aspect注解标识的类对象
        Set<Class<?>> aspectSet = beanContainer.getClassesByAnnotation(Aspect.class);
        // 这里相比于之前的版本不需要进行分类了 所以直接将第二步和第三步进行移除
        if (ValidationUtil.isEmpty(aspectSet)){
            return;
        }
        // 2. 拼接AspectInfoList
        List<AspectInfo> aspectInfoList = packAspectInfoList(aspectSet);
        // 3. 遍历容器里的类
        Set<Class<?>> classSet = beanContainer.getClasses(); // 获取容器中装配的除了Aspect切面类以外的所有类
        for (Class<?> targetClasss: classSet){
            // 排除AspectClass自身
            if(targetClasss.isAnnotationPresent(Aspect.class)){ // 可以使用Class类对象实例的isAnnotationPersent来判断该对象是不是有某一个指定的注解
                continue;
            }
            // 4. 粗筛符合条件的Aspect 相当于获取与该类所匹配的所有aspect类 切面类
            List<AspectInfo> roughMatchedAspectList = collectRoughMatchedAspectListForSpecificClass(aspectInfoList, targetClasss); // targetClass相当于从容器中获取的
            // 5. 尝试进行Aspect的织入
            wrapIfNecessary(roughMatchedAspectList, targetClasss);
        }

    }

    private void wrapIfNecessary(List roughMatchedAspectList, Class<?> targetClass) {
        if(ValidationUtil.isEmpty(roughMatchedAspectList)){return;}
        //创建动态代理对象
        AspectListExecutor aspectListExecutor = new AspectListExecutor(targetClass, roughMatchedAspectList);
        Object proxyBean = ProxyCreator.createProxy(targetClass, aspectListExecutor);
        beanContainer.addBean(targetClass, proxyBean); // 将切面所要织入的类的代理实例对象 放入容器中
    }

    private List<AspectInfo> collectRoughMatchedAspectListForSpecificClass(List<AspectInfo> aspectInfoList, Class<?> targetClasss) {
        List<AspectInfo> roughMatchedAspectList = new ArrayList<>();
        for(AspectInfo aspectInfo: aspectInfoList){
            // 粗筛 这里可以看下AspectInfo类的成员变量的情况 相当于每一个aspect类来说都会有一个表达式 可以对其想要加入的对象进行筛选 这个筛选的过程是通过aspectJ这个包里的方法进行实现的
            if(aspectInfo.getPointcutLocator().roughMatches(targetClasss)){
                roughMatchedAspectList.add(aspectInfo);
            }
        }
        return roughMatchedAspectList;
    }



    private List<AspectInfo> packAspectInfoList(Set<Class<?>> aspectSet) {
        List<AspectInfo> aspectInfoList = new ArrayList<>();
        for(Class<?> aspectClass : aspectSet){
            if (verifyAspect(aspectClass)){
                Order orderTag = aspectClass.getAnnotation(Order.class);
                Aspect aspectTag = aspectClass.getAnnotation(Aspect.class);
                DefaultAspect defaultAspect = (DefaultAspect) beanContainer.getBean(aspectClass);
                //初始化表达式定位器
                PointcutLocator pointcutLocator = new PointcutLocator(aspectTag.pointcut());
                AspectInfo aspectInfo = new AspectInfo(orderTag.value(), defaultAspect, pointcutLocator);
                aspectInfoList.add(aspectInfo);
            } else {
                //不遵守规范则直接抛出异常
                throw new RuntimeException("@Aspect and @Order must be added to the Aspect class, and Aspect class must extend from DefaultAspect");
            }
        }
        return aspectInfoList;
    }

    //框架中一定要遵守给Aspect类添加@Aspect和@Order标签的规范，同时，必须继承自DefaultAspect.class
    //此外，@Aspect的属性值不能是它本身
    private boolean verifyAspect(Class<?> aspectClass) {
        return aspectClass.isAnnotationPresent(Aspect.class) &&
                aspectClass.isAnnotationPresent(Order.class) &&
                DefaultAspect.class.isAssignableFrom(aspectClass); // 确定此class对象表示的类或接口是否与指定class参数表示的类或接口相同，或者是否是类或接口的超类或超接口  这里指的是判断这个类是不是继承自DefaultAspect
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
    //private void categorizeAspect(Map<Class<? extends Annotation>, List<AspectInfo>> categorizedMap, Class<?> aspectClass) {
    //    Order orderTag = aspectClass.getAnnotation(Order.class); // 获取该切面类的优先级
    //    Aspect aspectTag = aspectClass.getAnnotation(Aspect.class); // 获取该切面类所针对的目标的类型
    //    DefaultAspect aspect = (DefaultAspect) beanContainer.getBean(aspectClass);
    //    AspectInfo aspectInfo = new AspectInfo(orderTag.value(), aspect);
    //    if(!categorizedMap.containsKey(aspectTag.value())){
    //        // 如果织入的joinpoint第一次出现，则以该joinpoint为key,以新创建的List<AspectInfo>为value
    //        List<AspectInfo> aspectInfoList = new ArrayList<>();
    //        aspectInfoList.add(aspectInfo);
    //        categorizedMap.put(aspectTag.value(), aspectInfoList); // 这里相当于是将针对于congroller的切面 和针对于service的切面 都 放到hashMap中
    //    }else{
    //        // 如果织入的joinpoint不是第一次出现，则往joinpoint对应的value里添加新的Aspect逻辑
    //        List<AspectInfo> aspectInfoList = categorizedMap.get(aspectTag.value());
    //        aspectInfoList.add(aspectInfo);
    //    }
    //}

    // 框架中一定要遵守给Aspect类添加@Aspect和@Order标签的规范, 同时, 必须继承自DefaultAspect.class
    // 此外, @Aspect的属性值不能是它本身
    //private boolean verifyAspect(Class<?> aspectClass){
    //    return aspectClass.isAnnotationPresent(Aspect.class) && // 这个声明切面的类必须有Aspect注解
    //            aspectClass.isAnnotationPresent(Order.class) && // 这个声明切面的类必须有Order注解
    //            DefaultAspect.class.isAssignableFrom(aspectClass) && // 这个声明的切面的类必须是DefaultAspect的子类
    //            aspectClass.getAnnotation(Aspect.class).value()!=Aspect.class; //
    //}
}
