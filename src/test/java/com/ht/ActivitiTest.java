package com.ht;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.UserTask;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.junit.platform.commons.util.StringUtils;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author henry
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ActivitiApplication.class)
public class ActivitiTest {

    protected static final Logger logger = LoggerFactory.getLogger(ActivitiTest.class);

    @Resource
    private RepositoryService repositoryService;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private TaskService taskService;
    @Resource
    private HistoryService historyService;

    /**
     * 创建一个流程，需要手动设置name和key，key后面可以到画图工具里面去修改，问题不大
     */
    @Test
    public void createModel() throws UnsupportedEncodingException {
        Model model = repositoryService.newModel();
        //设置默认流程名称
        String name = "TEST";
        String description = "";
        int revision = 1;
        //设置key
        String key = "TEST-PROCESS";
        ObjectNode modelNode = objectMapper.createObjectNode();
        modelNode.put(ModelDataJsonConstants.MODEL_NAME, name);
        modelNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, description);
        modelNode.put(ModelDataJsonConstants.MODEL_REVISION, revision);
        model.setName(name);
        model.setKey(key);
        model.setMetaInfo(modelNode.toString());
        repositoryService.saveModel(model);
        String id = model.getId();
        ObjectNode editorNode = objectMapper.createObjectNode();
        editorNode.put("id", "canvas");
        editorNode.put("resourceId", "canvas");
        ObjectNode stencilSetNode = objectMapper.createObjectNode();
        stencilSetNode.put("namespace",
            "http://b3mn.org/stencilset/bpmn2.0#");
        editorNode.put("stencilset", stencilSetNode);
        repositoryService.addModelEditorSource(id, editorNode.toString().getBytes("utf-8"));
        //得到modelid
        System.out.println(id);
    }

    /**
     * 发布流程
     */
    @Test
    public void deploy() throws Exception {
        //获取模型
        Model modelData = repositoryService.getModel("1");
        byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());
        if (bytes == null) {
            logger.error("未找到对应modelId的流程模板！");
            return;
        }
        JsonNode modelNode = new ObjectMapper().readTree(bytes);
        BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        if (model.getProcesses().size() == 0) {
            logger.error("请设置一条流程图");
            return;
        }
        byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);
        //发布流程
        String processName = modelData.getName() + ".bpmn20.xml";
        Deployment deployment = repositoryService.createDeployment()
            .name(modelData.getName())
            .addString(processName, new String(bpmnBytes, "UTF-8"))
            .deploy();
        modelData.setDeploymentId(deployment.getId());
        repositoryService.saveModel(modelData);
    }

    /**
     * 开启一个流程
     */
    @Test
    public void startFlow() {
        Map<String, Object> map = new HashMap<>();
        map.put("firstPerson", "henry");
        map.put("secondPerson", "sherry");
        ProcessInstance processInstance = runtimeService
            .startProcessInstanceByKey("TEST_PROCESS", "businessCode02", map);
        System.out.println("启动成功");
        System.out.println("流程实例名称->" + processInstance.getName());
        System.out.println("流程实例ID->" + processInstance.getId());
    }

    /**
     * 查询当前个人待执行的任务
     */
    @Test
    public void queryPersonalTaskList() {
        // 流程定义key
        String processDefinitionKey = "TEST_PROCESS1";
        // 任务负责人
        String assignee = "sherry";
        List<Task> taskList = taskService.createTaskQuery()
            .processDefinitionKey(processDefinitionKey)
            .includeProcessVariables()
            .taskAssignee(assignee)
            .list();
        for (Task task : taskList) {
            System.out.println("----------------------------");
            System.out.println("流程实例id： " + task.getProcessInstanceId());
            System.out.println("任务id： " + task.getId());
            System.out.println("任务负责人： " + task.getAssignee());
            System.out.println("任务名称： " + task.getName());
            System.out.println("----------------------------");
        }
    }

    /**
     * 完成个人任务
     */
    @Test
    public void completTask() {
        //任务id
        String taskId = "65007";
//        任务负责人
        String assingee = "henry";
        Task task = taskService.createTaskQuery()
            .taskId(taskId)
            .taskAssignee(assingee)
            .singleResult();
        if (task != null) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("agree", 1);
            taskService.complete(taskId, map);
            System.out.println("完成任务");
        }
    }

    /**
     * 查询历史任务
     */
    @Test
    public void queryHis() {
        // 查询所有的历史节点
//        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery().processInstanceId("25001").finished().list();
//        for (HistoricActivityInstance hai : list) {
//            System.out.println("=================================");
//            System.out.println("活动ID:" + hai.getId());
//            System.out.println("流程实例ID:" + hai.getProcessInstanceId());
//            System.out.println("活动名称：" + hai.getActivityName());
//            System.out.println("办理人：" + hai.getAssignee());
//            System.out.println("开始时间：" + hai.getStartTime());
//            System.out.println("结束时间：" + hai.getEndTime());
//            System.out.println("=================================");
//        }
        //查询task的历史节点
        List<HistoricTaskInstance> list1 = historyService.createHistoricTaskInstanceQuery().processInstanceId("25001").finished().list();
        for (HistoricTaskInstance hai : list1) {
            System.out.println("=================================");
            System.out.println("活动ID:" + hai.getId());
            System.out.println("流程实例ID:" + hai.getProcessInstanceId());
            System.out.println("活动名称：" + hai.getName());
            System.out.println("办理人：" + hai.getAssignee());
            System.out.println("开始时间：" + hai.getStartTime());
            System.out.println("结束时间：" + hai.getEndTime());
            System.out.println("=================================");
        }
    }

    /**
     * 归还任务
     */
    public void returnTask(){
        //归还任务，也就是把assignee字段设置为空
        taskService.setAssignee("47507", null);
    }

    /**
     * 拾取任务
     */
    @Test
    public void claimTask() {
        //拾取任务
        taskService.claim("47507", "melo");
    }

    @Test
    public void queryCandidateTaskList() {
        // 流程定义key
        String processDefinitionKey = "TEST-PROCESS";
        // 任务候选人
        String candidateUser = "melo";
        // 创建TaskService
        //查询组任务
        List<Task> list = taskService.createTaskQuery()
            .processDefinitionKey(processDefinitionKey)
            .taskCandidateUser(candidateUser)//根据候选人查询
            .list();
        for (Task task : list) {
            System.out.println("----------------------------");
            System.out.println("流程实例id：" + task.getProcessInstanceId());
            System.out.println("任务id：" + task.getId());
            System.out.println("任务负责人：" + task.getAssignee());
            System.out.println("任务名称：" + task.getName());
            System.out.println("----------------------------");
        }
    }

    /**
     * 退回节点
     */
    @Test
    public void goBack() throws Exception {
        this.revoke("65001", "henry");
        System.out.println("退回成功");
    }

    /**
     * 根据名称退回
     */
    public void revoke(String processInstanceId, String nowUser) throws Exception {
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        if (task == null) {
            throw new Exception("流程未启动或已执行完成，无法撤回");
        }
        //通过processInstanceId查询历史节点
        List<HistoricTaskInstance> htiList = historyService.createHistoricTaskInstanceQuery()
            .processInstanceId(processInstanceId)
            .orderByTaskCreateTime()
            .asc()
            .list();
        String myTaskId = null;
        HistoricTaskInstance myTask = null;
        //找到当前运行的节点
        for (HistoricTaskInstance hti : htiList) {
            if (nowUser.equals(hti.getAssignee())) {
                myTaskId = hti.getId();
                myTask = hti;
                break;
            }
        }
        if (null == myTaskId) {
            throw new Exception("该任务非当前用户提交，无法撤回");
        }
        String processDefinitionId = myTask.getProcessDefinitionId();
        //获取流程模型
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        String myActivityId = null;
        //查询已经完成的流程节点，查询到上一条已完成的节点，则跳出循环
        List<HistoricActivityInstance> haiList = historyService.createHistoricActivityInstanceQuery()
            .executionId(myTask.getExecutionId()).finished().list();
        for (HistoricActivityInstance hai : haiList) {
            if (myTaskId.equals(hai.getTaskId())) {
                myActivityId = hai.getActivityId();
                break;
            }
        }
        FlowNode myFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(myActivityId);
        Execution execution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        String activityId = execution.getActivityId();
        FlowNode flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(activityId);
        //记录原活动方向
        List<SequenceFlow> oriSequenceFlows = new ArrayList<SequenceFlow>();
        oriSequenceFlows.addAll(flowNode.getOutgoingFlows());
        //清理活动方向
        flowNode.getOutgoingFlows().clear();
        //建立新方向
        List<SequenceFlow> newSequenceFlowList = new ArrayList<SequenceFlow>();
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlowId");
        newSequenceFlow.setSourceFlowElement(flowNode);
        newSequenceFlow.setTargetFlowElement(myFlowNode);
        newSequenceFlowList.add(newSequenceFlow);
        flowNode.setOutgoingFlows(newSequenceFlowList);
        Authentication.setAuthenticatedUserId(nowUser);
        taskService.addComment(task.getId(), task.getProcessInstanceId(), "撤回");
        //完成任务
        taskService.complete(task.getId());
        //恢复原方向
        flowNode.setOutgoingFlows(oriSequenceFlows);
        logger.info("退回成功！");
    }

    /**
     * 跳转指定节点
     */
    @Test
    public void jumptoNext() throws Exception {
        this.skip("180001");
    }

    public void skip(String instanceId) throws Exception {
        Task task = taskService.createTaskQuery().processInstanceId(instanceId).singleResult();
        if (task == null) {
            throw new Exception("流程未启动或已执行完成，无法撤回");
        }
        String processDefinitionId = task.getProcessDefinitionId();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        //获取当前节点
        Execution execution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        String activityId = execution.getActivityId();
        FlowNode flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(activityId);
        //需要跳转的节点
        FlowNode toFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement("sid-7BAD1FA7-8E4F-4555-BB6B-A55A02E5AF54");
        if (toFlowNode == null) {
            throw new Exception("跳转的下一节点为空");
        }
        //记录原活动方向
        List<SequenceFlow> oriSequenceFlows = new ArrayList<SequenceFlow>();
        oriSequenceFlows.addAll(flowNode.getOutgoingFlows());
        //清理活动方向
        flowNode.getOutgoingFlows().clear();
        //建立新方向
        List<SequenceFlow> newSequenceFlowList = new ArrayList<SequenceFlow>();
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlowId");
        newSequenceFlow.setSourceFlowElement(flowNode);
        newSequenceFlow.setTargetFlowElement(toFlowNode);
        newSequenceFlowList.add(newSequenceFlow);
        flowNode.setOutgoingFlows(newSequenceFlowList);
        taskService.addComment(task.getId(), task.getProcessInstanceId(), "跳转指定节点");
        //完成任务
        taskService.complete(task.getId());
        //恢复原方向
        flowNode.setOutgoingFlows(oriSequenceFlows);
        logger.info("跳转成功，from->{},to->{}", flowNode.getName(), toFlowNode.getName());
    }

    /**
     * 获取当前流程的所有节点信息(启动后的流程)
     */
    @Test
    public void getFlowNodes() {
        Task task = taskService.createTaskQuery().processInstanceId("180001").singleResult();
        if (task == null) {
//            throw new Portal("流程未启动或已执行完成，无法撤回");
            return;
        }
        String processDefinitionId = task.getProcessDefinitionId();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();
        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof UserTask) {
                UserTask userTask = (UserTask) flowElement;
                System.out.println(flowElement.getName());
                System.out.println(flowElement.getId());
                System.out.println(userTask.getAssignee());
                String assigneeEl = userTask.getAssignee();
                if (StringUtils.isBlank(assigneeEl)) {
                    continue;
                }
                if (assigneeEl.startsWith("${") && assigneeEl.endsWith("}") && assigneeEl.length() > 3) {
                    String assignee = assigneeEl.substring(2, assigneeEl.length() - 2);
                    System.out.println("assignee:" + assignee);
                }
            }
        }
    }

    /**
     * 获取变量名称用于自动化配置（基于流程模板）
     */
    @Test
    public void getVariables() throws Exception {
        String modelId = "1";
        //获取模型
        Model modelData = repositoryService.getModel(modelId);
        byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());
        if (bytes == null) {
            throw new Exception("模型数据为空，请先设计流程并成功保存，再进行发布!");
        }
        JsonNode modelNode = null;
        try {
            modelNode = new ObjectMapper().readTree(bytes);
        } catch (IOException e) {
            throw new Exception("读取模型数据异常！");
        }
        BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();
        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof UserTask) {
                UserTask userTask = (UserTask) flowElement;
                System.out.println("-----------------------------");
                System.out.println(flowElement.getName());
                System.out.println(flowElement.getId());
                String assigneeEl = userTask.getAssignee();
                if (StringUtils.isBlank(assigneeEl)) {
                    continue;
                }
                if (assigneeEl.startsWith("${") && assigneeEl.endsWith("}") && assigneeEl.length() > 3) {
                    String assignee = assigneeEl.substring(2, assigneeEl.length() - 2);
                    System.out.println("assignee:" + assignee);
                }
            }
        }
    }
}
