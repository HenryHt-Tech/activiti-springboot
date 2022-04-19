package com.ht.activiti.service;

import javax.annotation.Resource;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.RepositoryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author henry
 */
@Service
public class ActivitiService {

    @Resource
    private RepositoryService repositoryService;

    /**
     * 获取当前运行流程中得BpmnModel
     * @param processDefinitionId 流程实例id
     * @return org.activiti.bpmn.model.BpmnModel
     */
    public BpmnModel getRuntimeBpmnModel(String processDefinitionId) throws Exception {
        if (StringUtils.isBlank(processDefinitionId)) {
            throw new Exception("流程实例id为空");
        }
        return repositoryService.getBpmnModel(processDefinitionId);
    }

}
