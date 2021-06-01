package com.rmal30.auweatherbom.models;

import java.util.List;
import java.util.Map;

public class XMLTree {
    private String tag;
    private Map<String, String> attributes;
    private List<XMLTree> children;
    private String value;

    public XMLTree(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return this.tag;
    }

    public XMLTree(String tag, Map<String, String> attributes) {
        this(tag);
        this.attributes = attributes;
    }

    public void addAttribute(String key, String value) {
        this.attributes.put(key, value);
    }

    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    public XMLTree(String tag, Map<String, String> attributes, List<XMLTree> children) {
        this(tag, attributes);
        this.children = children;
    }

    public void addChildXML(XMLTree xmlTree) {
        this.children.add(xmlTree);
    }

    public void setChildren(List<XMLTree> children) {
        this.children = children;
    }

    public List<XMLTree> getChildren() {
        return this.children;
    }

    public XMLTree(String tag, Map<String, String> attributes, String value) {
        this(tag, attributes);
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
