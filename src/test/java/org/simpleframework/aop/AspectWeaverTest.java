package org.simpleframework.aop;

import com.imooc.controller.superadmin.HeadLineOperationController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.simpleframework.core.BeanContainer;
import org.simpleframework.inject.DependencyInjector;

public class AspectWeaverTest {
    @DisplayName("֯��ͨ���߼�����:doAop")
    @Test
    public void doAopTest(){
        BeanContainer beanContainer = BeanContainer.getInstance();
        beanContainer.loadBeans("com.imooc");

        new AspectWeaver().doAop();
        new DependencyInjector().doIoc();

        HeadLineOperationController headLineOperationController = (HeadLineOperationController) beanContainer.getBean(HeadLineOperationController.class);
        //headLineOperationController.addHeadLine(null, null);
        headLineOperationController.removeHeadLine();
    }
}
