package com.looksee.audit.informationArchitecture.models;

import java.util.ArrayList;
import java.util.List;

public class HeaderNode {
    private String tag;
    private String text;
    private List<HeaderNode> children;

    public HeaderNode(String tag, String text) {
        this.tag = tag;
        this.text = text;
        this.children = new ArrayList<>();
    }

    public String getTag() {
        return tag;
    }

    public String getText() {
        return text;
    }

    public List<HeaderNode> getChildren() {
        return children;
    }

    public void addChild(HeaderNode child) {
        this.children.add(child);
    }
}
