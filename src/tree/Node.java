package src.tree;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private static int nextId = 0;

    private final int id;
    private final String contents;
    private Node parent;
    private final List<Node> children;

    public Node(String contents) {
        this.id = nextId++;
        this.contents = contents;
        this.children = new ArrayList<Node>();
    }

    public int getId() {
        return id;
    }

    public String getContents() {
        return contents;
    }

    public Node getParent() {
        return parent;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void addChild(Node child) {
        children.add(child);
        child.parent = this;
    }
}