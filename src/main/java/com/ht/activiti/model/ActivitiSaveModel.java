package com.ht.activiti.model;

import lombok.Data;

@Data
public class ActivitiSaveModel {

    private String modelId;

    private String name;

    private String description;

    private String json_xml;

    private String svg_xml;
}
