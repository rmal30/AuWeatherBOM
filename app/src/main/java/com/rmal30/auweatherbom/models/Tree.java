package com.rmal30.auweatherbom.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Tree {
    public String type, value;
    public List<Tree> children;
    public boolean hasChildren;
    public Map<String, String> properties;
    public Tree(String type, String value, Map<String, String> properties, boolean hasChildren) {
        this.type = type;
        this.properties = properties;
        this.hasChildren = hasChildren;

        if (this.hasChildren) {
            this.children = new ArrayList<>();
        } else {
            this.value = value;
        }
    }
    public Tree() {

    }
}
