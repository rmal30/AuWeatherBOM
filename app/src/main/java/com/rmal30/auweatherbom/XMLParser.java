package com.rmal30.auweatherbom;

import android.util.Xml;

import com.rmal30.auweatherbom.models.XMLTree;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class XMLParser {

    //XML parser
    public static List<XMLTree> parseXML(String xmlString) throws XmlPullParserException, IOException {
        XmlPullParser xmlParser = Xml.newPullParser();
        int eventType;
        xmlParser.setInput(new StringReader(xmlString));
        eventType = xmlParser.getEventType();
        List<XMLTree> xmlTrees = new ArrayList<>();
        Stack<XMLTree> pendingTrees = new Stack<>();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    XMLTree xmlTree = new XMLTree(xmlParser.getName(), new HashMap<>());
                    for (int i = 0; i < xmlParser.getAttributeCount(); i++) {
                        String key = xmlParser.getAttributeName(i);
                        String value = xmlParser.getAttributeValue(i);
                        xmlTree.addAttribute(key, value);
                    }
                    pendingTrees.push(xmlTree);
                    break;
                case XmlPullParser.TEXT:
                    pendingTrees.peek().setValue(xmlParser.getText());
                    break;
                case XmlPullParser.END_TAG:
                    XMLTree currentTree = pendingTrees.pop();
                    if (pendingTrees.empty()) {
                        xmlTrees.add(currentTree);
                    } else {
                        XMLTree parentTree = pendingTrees.peek();
                        if(parentTree.getChildren() == null) {
                            parentTree.setChildren(new ArrayList<>());
                        }
                        parentTree.addChildXML(currentTree);
                    }
                    break;
                default:
                    break;
            }
            eventType = xmlParser.next();
        }
        return xmlTrees;
    }

    //Print out the object data as xml, used to verify that xml is read properly
    public String printXML(List<XMLTree> xmlTrees) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (XMLTree xmlTree : xmlTrees) {
            sb.append("Tag: ");
            sb.append(xmlTree.getTag());
            sb.append("\n");
            if (xmlTree.getAttributes() != null) {
                sb.append("Attributes:");
                for (Map.Entry<String, String> entry : xmlTree.getAttributes().entrySet()) {
                    sb.append(entry.getKey());
                    sb.append(" ");
                    sb.append(entry.getValue());
                    sb.append("\n");
                }
            }
            sb.append("Value: {");
            if (xmlTree.getChildren() != null) {
                sb.append(printXML(xmlTree.getChildren()));
            }
            if (xmlTree.getValue() != null) {
                sb.append(xmlTree.getValue());
            }
            sb.append("}");
            sb.append("\n\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
