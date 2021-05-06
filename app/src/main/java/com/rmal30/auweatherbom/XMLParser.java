package com.rmal30.auweatherbom;

import com.rmal30.auweatherbom.models.Tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class XMLParser {
    //Old and slower way of parsing XML
    public ArrayList<Tree> parseXMLOld(String xmlData, int start, int end){
        char c, c2;
        if(start == 0){
            do{start++;}while(xmlData.charAt(start)!='>');
        }
        ArrayList<Tree> XMLNodes = new ArrayList<>();
        Tree XMLNode;
        HashMap<String, String> properties;
        String type;
        int begin, depth, j, endTag, center;
        boolean open = false, hasChildren;
        while(start<end){
            do{start++;}while(start<end && xmlData.charAt(start)!='<');
            if(start==end){
                return XMLNodes;
            }
            begin = start+1;
            do {
                c = xmlData.charAt(start);
                start++;
            }while(c!=' ' && c!='>');
            type = xmlData.substring(begin, start-1);
            if(c == ' '){
                properties = new HashMap<>();
                begin = start;
                center = start;
                while(c!='>') {
                    c = xmlData.charAt(start);
                    if (c == '"') {
                        open = !open;
                    }else if(c == '='){
                        center = start;
                    }else if (!open && (c == ' ' || c == '>')) {
                        properties.put(xmlData.substring(begin, center), xmlData.substring(center+2, start-1));
                        begin = start + 1;
                    }
                    start++;
                }
            }else{
                properties = null;
            }
            if(xmlData.charAt(start-2)!='/'){
                depth = 1; j = start + 1;
                c2 = xmlData.charAt(start);
                hasChildren = false;
                endTag = start;
                while (j < end && (depth > 0 || open)) {
                    c = c2;
                    c2 = xmlData.charAt(j);
                    if(c2 == '>'){
                        open = false;
                    }else if (c == '<') {
                        open = true;
                        if (c2 == '/') {
                            depth--;
                            endTag = j - 1;
                        } else {
                            depth++;
                            hasChildren = true;
                        }
                    }
                    j++;
                }
                if (hasChildren) {
                    XMLNode = new Tree(type, null, properties, true);
                    XMLNode.children = parseXMLOld(xmlData, start, endTag);
                } else {
                    XMLNode = new Tree(type, xmlData.substring(start, endTag), properties, false);
                }
                start = j + 1;
            }else{
                XMLNode = new Tree(type, null, properties, false);
            }
            XMLNodes.add(XMLNode);
        }
        return XMLNodes;
    }

    //XML parser
    public static Tree parseXML(String xmlData){
        if(xmlData==null){
            return null;
        }
        Tree node = new Tree();
        node.hasChildren = false;
        Stack<Tree> stack = new Stack<>();
        stack.add(node);
        char c;
        int i=0;
        do{i++;}while(xmlData.charAt(i)!='>');
        do{i++;}while(xmlData.charAt(i)!='<');
        while(!stack.isEmpty()) {
            node = stack.peek();
            if(node.type != null) {
                if(xmlData.charAt(i + 1) == '/') {
                    do {
                        i++;
                    } while (xmlData.charAt(i) != '>');
                    stack.pop();
                    if(!stack.isEmpty()) {
                        do {
                            i++;
                        } while (xmlData.charAt(i) != '<');
                    }
                }else {
                    node.hasChildren = true;
                    Tree child = new Tree();
                    child.hasChildren = false;
                    if(node.children == null) {
                        node.children = new ArrayList<>();
                    }
                    node.children.add(child);
                    stack.push(child);
                }
            } else {
                int begin = i, center;
                boolean open;
                do {
                    c = xmlData.charAt(i);
                    i++;
                } while (c != ' ' && c != '>');
                node.type = xmlData.substring(begin, i - 1);
                if (c == ' ') {
                    node.properties = new HashMap<>();
                    begin = i;
                    center = i;
                    open = false;
                    while (c != '>') {
                        c = xmlData.charAt(i);
                        if(c == '"') {
                            open = !open;
                        } else if (c == '=') {
                            center = i;
                        } else if (!open && (c == ' ' || c == '>')) {
                            node.properties.put(xmlData.substring(begin, center), xmlData.substring(center + 2, i - 1));
                            begin = i + 1;
                        }
                        i++;
                    }
                } else {
                    node.properties = null;
                }
                if (xmlData.charAt(i - 2) == '/') {
                    stack.pop();
                    do{i++;}while(xmlData.charAt(i)!='<');
                }
                begin=i;
                while (xmlData.charAt(i)!='<') {
                    i++;
                }
                if(xmlData.charAt(i+1)=='/'){
                    node.value = xmlData.substring(begin, i);
                }

            }
        }
        return node;
    }

    //Print out the object data as xml, used to verify that xml is read properly
    public String printXML(List<Tree> tree){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(Tree t:tree){
            sb.append("Type: ");
            sb.append(t.type);
            sb.append("\n");
            sb.append("Properties:");
            if(t.properties!=null) {
                for (String key : t.properties.keySet()) {
                    sb.append(key);
                    sb.append(" ");
                    sb.append(t.properties.get(key));
                    sb.append("\n");
                }
            }
            sb.append("Value: {");
            if(t.hasChildren){
                sb.append(printXML(t.children));
            }else{
                sb.append(t.value);
            }
            sb.append("}");
            sb.append("\n\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
