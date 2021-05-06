package com.rmal30.auweatherbom.models;

import java.util.ArrayList;
import java.util.HashMap;

public class Tree{
    public String type, value;
    public ArrayList<Tree> children;
    public boolean hasChildren;
    public HashMap<String, String> properties;
    public Tree(String type, String value, HashMap<String,String> properties, boolean hasChildren){
        this.type = type;
        this.properties = properties;
        this.hasChildren = hasChildren;
        if(this.hasChildren) {
            this.children = new ArrayList<>();
        }else{
            this.value = value;
        }
    }
    public Tree(){

    }
}
