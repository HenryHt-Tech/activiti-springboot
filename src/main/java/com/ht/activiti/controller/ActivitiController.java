package com.ht.activiti.controller;

import com.ht.activiti.model.ActivitiSaveModel;
import com.ht.activiti.util.ActivitiTracingChart;
import java.io.IOException;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author henry
 */
@RestController("activiti")
public class ActivitiController {

    @Resource
    private ActivitiTracingChart activitiTracingChart;

    @PutMapping(value = "/{modelId}/save")
    @ResponseStatus(value = HttpStatus.OK)
    public void saveModel(HttpServletRequest request, @PathVariable String modelId, ActivitiSaveModel activitiSaveModel) {
        activitiSaveModel.setModelId(modelId);
        System.out.println("save it");
    }

    @GetMapping("/getFlowcChart")
    public void getFlowcChart(String processInstanceId, HttpServletResponse response) throws IOException {
        activitiTracingChart.generateFlowChart(processInstanceId, response.getOutputStream());
    }
}
