package org.simpleframework.aop;

import lombok.Getter;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.simpleframework.aop.annotation.Aspect;
import org.simpleframework.aop.annotation.Order;
import org.simpleframework.aop.aspect.AspectInfo;
import org.simpleframework.util.ValidationUtil;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/*
    该类用于向一个目标类中 加入切面类 这里是通过cglib动态代理进行的实现
 */
public class AspectListExecutor implements MethodInterceptor {
    // 被代理的类
    private  Class<?> targetClass;

    // 排好序的Aspect列表
    @Getter
    private List<AspectInfo> sortedAspectInfoList;

    public AspectListExecutor(Class<?> targetClass, List<AspectInfo> aspectInfoList){
        this.targetClass = targetClass;
        this.sortedAspectInfoList = sortAspectInfoList(aspectInfoList); // 按照order从小到大进行排序
    }

    /**
     * 按照order的值进行升序排序，确保order值小的aspect先被织入
     * @param aspectInfoList
     * @return
     */
    private List<AspectInfo> sortAspectInfoList(List<AspectInfo> aspectInfoList) {
        Collections.sort(aspectInfoList, new Comparator<AspectInfo>() {
            @Override
            public int compare(AspectInfo o1, AspectInfo o2) {
                //return 0;
                return o1.getOrderIndex() - o2.getOrderIndex();
            }
        });
        return aspectInfoList;
    }

    @Override
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        Object returnValue = null;
        collectAccurateMatchedAspectList(method);
        if(ValidationUtil.isEmpty(sortedAspectInfoList)){
            returnValue = methodProxy.invokeSuper(proxy, args);
            return returnValue;
        }

        // 1.按照order的顺序执行完所有Aspect的before方法
        invokeBeforeAdvices(method, args);
        try{
            // 2.执行被代理类的方法
            returnValue = methodProxy.invokeSuper(proxy, args);
            // 3.如果代理方法正常返回，则按照order的顺序降序执行完所有Aspect的afterReturning方法
            returnValue = invokeAfterReturningAdvices(method, args, returnValue);
        }catch (Exception e){
            // 4.如果代理方法抛出异常，则按照order的顺序降序执行完所有Aspect的afterThrowing方法
            invokeAfterThrowingAdvices(method, args, e);
        }
        return returnValue;
    }

    private void collectAccurateMatchedAspectList(Method method) {
        if(ValidationUtil.isEmpty(sortedAspectInfoList)){return;}
        // java.util.ConcurrentModificationException for:mutex fail-fast sortedAspectInfoList.remove() Iterator.remove
        // 考虑使用迭代器的方式进行遍历
        Iterator<AspectInfo> it = sortedAspectInfoList.iterator();
        while(it.hasNext()){
            AspectInfo aspectInfo = it.next();
            if(!aspectInfo.getPointcutLocator().accurateMatches(method)){ // 如果当前切面类与当前方法不匹配 则将当前的切面类删除
                it.remove();
            }
        }
    }

    //4. 如果被代理方法抛出异常，则按照orer的顺序降序执行完所有Aspect的afterThrowing方法
    private void invokeAfterThrowingAdvices(Method method, Object[] args, Exception e) throws Throwable {
        for(int i=sortedAspectInfoList.size()-1; i>=0; i--){
            sortedAspectInfoList.get(i).getAspectObject().afterThrowing(targetClass, method, args, e);
        }
    }

    // 3.如果被代理方法正常返回, 则按照order的顺序执行完所有Aspect的afterReturning方法
    private Object invokeAfterReturningAdvices(Method method, Object[] args, Object returnValue){
        Object result = null;
        for(int i=sortedAspectInfoList.size()-1; i>=0; i--){
            result = sortedAspectInfoList.get(i).getAspectObject().afterReturning(targetClass, method, args, returnValue);
        }
        return result;
    }

    // 1.按照order的顺序升序执行完所有Aspect的before方法
    private void invokeBeforeAdvices(Method method, Object[] args) throws Throwable {
        for(AspectInfo aspectInfo: sortedAspectInfoList){
            aspectInfo.getAspectObject().before(targetClass, method, args);
        }
    }
}
