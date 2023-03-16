package demo.pattern.proxy;

import demo.pattern.proxy.cglib.AlipayMethodInterceptor;
import demo.pattern.proxy.cglib.CglibUtil;
import demo.pattern.proxy.impl.*;
import demo.pattern.proxy.jdkproxy.AlipayInvocationHandler;
import demo.pattern.proxy.jdkproxy.JdkDynamicProxyUtil;
import net.sf.cglib.proxy.MethodInterceptor;

import java.lang.reflect.InvocationHandler;

public class ProxyDemo {
    public static void main(String[] args) {
        //ToCPayment toCProxy = new AlipayToC(new ToCPaymentImpl());
        //toCProxy.pay();
        //ToBPayment toBProxy = new AlipayToB(new ToBPaymentImpl());
        //toBProxy.pay();

        //ToCPaymentImpl toCPayment = new ToCPaymentImpl();
        //InvocationHandler handler = new AlipayInvocationHandler(toCPayment);// 创建出对应的切面类实例来
        //ToCPayment toCProxy = JdkDynamicProxyUtil.newProxyInstance(toCPayment, handler);
        //toCProxy.pay();
        //
        //ToBPaymentImpl toBPayment = new ToBPaymentImpl();
        //handler = new AlipayInvocationHandler(toBPayment);// 创建出对应的切面类实例来
        //ToBPayment toBProxy = JdkDynamicProxyUtil.newProxyInstance(toBPayment, handler);
        //toBProxy.pay();

        //CommonPayment commonPayment = new CommonPayment();
        //AlipayInvocationHandler invocationHandler = new AlipayInvocationHandler(commonPayment);
        //CommonPayment commonPaymentProxy = JdkDynamicProxyUtil.newProxyInstance(commonPayment, invocationHandler);
        //commonPaymentProxy.pay();

        CommonPayment commonPayment = new CommonPayment();
        MethodInterceptor methodInterceptor = new AlipayMethodInterceptor();
        CommonPayment commonPaymentProxy = CglibUtil.createProxy(commonPayment, methodInterceptor);
        commonPaymentProxy.pay();

        //ToCPaymentImpl toCPayment = new ToCPaymentImpl();
        //MethodInterceptor methodInterceptor = new AlipayMethodInterceptor();
        //ToCPaymentImpl toCProxy = CglibUtil.createProxy(toCPayment, methodInterceptor);
        //toCProxy.pay();
    }
}
