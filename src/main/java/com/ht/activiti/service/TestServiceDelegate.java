package com.ht.activiti.service;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

/**
 * 服务节点监听器，当节点走到服务节点时，会走次方法调用execute来执行业务逻辑
 * @author henry
 */
public class TestServiceDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        System.out.println("开始处理业务逻辑-->...");
    }

}
