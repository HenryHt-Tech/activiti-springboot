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
     * ???????????????????????????????????????name???key???key?????????????????????????????????????????????????????????
     */
    @Test
    public void createModel() throws UnsupportedEncodingException {
        Model model = repositoryService.newModel();
        //????????????????????????
        String name = "TEST";
        String description = "";
        int revision = 1;
        //??????key
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
        //??????modelid
        System.out.println(id);
    }

    /**
     * ????????????
     */
    @Test
    public void deploy() throws Exception {
        //????????????
        Model modelData = repositoryService.getModel("1");
        byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());
        if (bytes == null) {
            logger.error("???????????????modelId??????????????????");
            return;
        }
        JsonNode modelNode = new ObjectMapper().readTree(bytes);
        BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        if (model.getProcesses().size() == 0) {
            logger.error("????????????????????????");
            return;
        }
        byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);
        //????????????
        String processName = modelData.getName() + ".bpmn20.xml";
        Deployment deployment = repositoryService.createDeployment()
            .name(modelData.getName())
            .addString(processName, new String(bpmnBytes, "UTF-8"))
            .deploy();
        modelData.setDeploymentId(deployment.getId());
        repositoryService.saveModel(modelData);
    }

    /**
     * ??????????????????
     */
    @Test
    public void startFlow() {
        Map<String, Object> map = new HashMap<>();
        map.put("firstPerson", "henry");
        map.put("secondPerson", "sherry");
        ProcessInstance processInstance = runtimeService
            .startProcessInstanceByKey("TEST_PROCESS", "businessCode02", map);
        System.out.println("????????????");
        System.out.println("??????????????????->" + processInstance.getName());
        System.out.println("????????????ID->" + processInstance.getId());
    }

    /**
     * ????????????????????????????????????
     */
    @Test
    public void queryPersonalTaskList() {
        // ????????????key
        String processDefinitionKey = "TEST_PROCESS1";
        // ???????????????
        String assignee = "sherry";
        List<Task> taskList = taskService.createTaskQuery()
            .processDefinitionKey(processDefinitionKey)
            .includeProcessVariables()
            .taskAssignee(assignee)
            .list();
        for (Task task : taskList) {
            System.out.println("----------------------------");
            System.out.println("????????????id??? " + task.getProcessInstanceId());
            System.out.println("??????id??? " + task.getId());
            System.out.println("?????????????????? " + task.getAssignee());
            System.out.println("??????????????? " + task.getName());
            System.out.println("----------------------------");
        }
    }

    /**
     * ??????????????????
     */
    @Test
    public void completTask() {
        //??????id
        String taskId = "65007";
//        ???????????????
        String assingee = "henry";
        Task task = taskService.createTaskQuery()
            .taskId(taskId)
            .taskAssignee(assingee)
            .singleResult();
        if (task != null) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("agree", 1);
            taskService.complete(taskId, map);
            System.out.println("????????????");
        }
    }

    /**
     * ??????????????????
     */
    @Test
    public void queryHis() {
        // ???????????????????????????
//        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery().processInstanceId("25001").finished().list();
//        for (HistoricActivityInstance hai : list) {
//            System.out.println("=================================");
//            System.out.println("??????ID:" + hai.getId());
//            System.out.println("????????????ID:" + hai.getProcessInstanceId());
//            System.out.println("???????????????" + hai.getActivityName());
//            System.out.println("????????????" + hai.getAssignee());
//            System.out.println("???????????????" + hai.getStartTime());
//            System.out.println("???????????????" + hai.getEndTime());
//            System.out.println("=================================");
//        }
        //??????task???????????????
        List<HistoricTaskInstance> list1 = historyService.createHistoricTaskInstanceQuery().processInstanceId("25001").finished().list();
        for (HistoricTaskInstance hai : list1) {
            System.out.println("=================================");
            System.out.println("??????ID:" + hai.getId());
            System.out.println("????????????ID:" + hai.getProcessInstanceId());
            System.out.println("???????????????" + hai.getName());
            System.out.println("????????????" + hai.getAssignee());
            System.out.println("???????????????" + hai.getStartTime());
            System.out.println("???????????????" + hai.getEndTime());
            System.out.println("=================================");
        }
    }

    /**
     * ????????????
     */
    public void returnTask(){
        //???????????????????????????assignee??????????????????
        taskService.setAssignee("47507", null);
    }

    /**
     * ????????????
     */
    @Test
    public void claimTask() {
        //????????????
        taskService.claim("47507", "melo");
    }

    @Test
    public void queryCandidateTaskList() {
        // ????????????key
        String processDefinitionKey = "TEST-PROCESS";
        // ???????????????
        String candidateUser = "melo";
        // ??????TaskService
        //???????????????
        List<Task> list = taskService.createTaskQuery()
            .processDefinitionKey(processDefinitionKey)
            .taskCandidateUser(candidateUser)//?????????????????????
            .list();
        for (Task task : list) {
            System.out.println("----------------------------");
            System.out.println("????????????id???" + task.getProcessInstanceId());
            System.out.println("??????id???" + task.getId());
            System.out.println("??????????????????" + task.getAssignee());
            System.out.println("???????????????" + task.getName());
            System.out.println("----------------------------");
        }
    }

    /**
     * ????????????
     */
    @Test
    public void goBack() throws Exception {
        this.revoke("65001", "henry");
        System.out.println("????????????");
    }

    /**
     * ??????????????????
     */
    public void revoke(String processInstanceId, String nowUser) throws Exception {
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        if (task == null) {
            throw new Exception("????????????????????????????????????????????????");
        }
        //??????processInstanceId??????????????????
        List<HistoricTaskInstance> htiList = historyService.createHistoricTaskInstanceQuery()
            .processInstanceId(processInstanceId)
            .orderByTaskCreateTime()
            .asc()
            .list();
        String myTaskId = null;
        HistoricTaskInstance myTask = null;
        //???????????????????????????
        for (HistoricTaskInstance hti : htiList) {
            if (nowUser.equals(hti.getAssignee())) {
                myTaskId = hti.getId();
                myTask = hti;
                break;
            }
        }
        if (null == myTaskId) {
            throw new Exception("?????????????????????????????????????????????");
        }
        String processDefinitionId = myTask.getProcessDefinitionId();
        //??????????????????
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        String myActivityId = null;
        //??????????????????????????????????????????????????????????????????????????????????????????
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
        //?????????????????????
        List<SequenceFlow> oriSequenceFlows = new ArrayList<SequenceFlow>();
        oriSequenceFlows.addAll(flowNode.getOutgoingFlows());
        //??????????????????
        flowNode.getOutgoingFlows().clear();
        //???????????????
        List<SequenceFlow> newSequenceFlowList = new ArrayList<SequenceFlow>();
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlowId");
        newSequenceFlow.setSourceFlowElement(flowNode);
        newSequenceFlow.setTargetFlowElement(myFlowNode);
        newSequenceFlowList.add(newSequenceFlow);
        flowNode.setOutgoingFlows(newSequenceFlowList);
        Authentication.setAuthenticatedUserId(nowUser);
        taskService.addComment(task.getId(), task.getProcessInstanceId(), "??????");
        //????????????
        taskService.complete(task.getId());
        //???????????????
        flowNode.setOutgoingFlows(oriSequenceFlows);
        logger.info("???????????????");
    }

    /**
     * ??????????????????
     */
    @Test
    public void jumptoNext() throws Exception {
        this.skip("180001");
    }

    public void skip(String instanceId) throws Exception {
        Task task = taskService.createTaskQuery().processInstanceId(instanceId).singleResult();
        if (task == null) {
            throw new Exception("????????????????????????????????????????????????");
        }
        String processDefinitionId = task.getProcessDefinitionId();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        //??????????????????
        Execution execution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        String activityId = execution.getActivityId();
        FlowNode flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(activityId);
        //?????????????????????
        FlowNode toFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement("sid-7BAD1FA7-8E4F-4555-BB6B-A55A02E5AF54");
        if (toFlowNode == null) {
            throw new Exception("???????????????????????????");
        }
        //?????????????????????
        List<SequenceFlow> oriSequenceFlows = new ArrayList<SequenceFlow>();
        oriSequenceFlows.addAll(flowNode.getOutgoingFlows());
        //??????????????????
        flowNode.getOutgoingFlows().clear();
        //???????????????
        List<SequenceFlow> newSequenceFlowList = new ArrayList<SequenceFlow>();
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlowId");
        newSequenceFlow.setSourceFlowElement(flowNode);
        newSequenceFlow.setTargetFlowElement(toFlowNode);
        newSequenceFlowList.add(newSequenceFlow);
        flowNode.setOutgoingFlows(newSequenceFlowList);
        taskService.addComment(task.getId(), task.getProcessInstanceId(), "??????????????????");
        //????????????
        taskService.complete(task.getId());
        //???????????????
        flowNode.setOutgoingFlows(oriSequenceFlows);
        logger.info("???????????????from->{},to->{}", flowNode.getName(), toFlowNode.getName());
    }

    /**
     * ???????????????????????????????????????(??????????????????)
     */
    @Test
    public void getFlowNodes() {
        Task task = taskService.createTaskQuery().processInstanceId("180001").singleResult();
        if (task == null) {
//            throw new Portal("????????????????????????????????????????????????");
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
     * ???????????????????????????????????????????????????????????????
     */
    @Test
    public void getVariables() throws Exception {
        String modelId = "1";
        //????????????
        Model modelData = repositoryService.getModel(modelId);
        byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());
        if (bytes == null) {
            throw new Exception("????????????????????????????????????????????????????????????????????????!");
        }
        JsonNode modelNode = null;
        try {
            modelNode = new ObjectMapper().readTree(bytes);
        } catch (IOException e) {
            throw new Exception("???????????????????????????");
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
