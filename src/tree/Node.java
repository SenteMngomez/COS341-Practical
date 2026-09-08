package tree;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Node {

    private static final AtomicInteger idSequence =
            new AtomicInteger(1);

    private final int id;

    // Name of the node:
    // Non-terminal: "P", "ALGO", "TERM"
    // Terminal: "USER-DEFINED-NAME", "NUM", etc.
    private final String name;

    // Actual lexeme for a terminal.
    // Example: "#x", "42", "hello"
    // null for non-terminals.
    private final String value;

    private Node parent;

    private final List<Node> children;

    // Constructor for non-terminal nodes
    public Node(String name) {
        this.id = idSequence.getAndIncrement();
        this.name = name;
        this.value = null;
        this.children = new ArrayList<Node>();
        this.parent = null;
    }

    // Constructor for terminal nodes
    public Node(String name, String value) {
        this.id = idSequence.getAndIncrement();
        this.name = name;
        this.value = value;
        this.children = new ArrayList<Node>();
        this.parent = null;
    }

    public int getId() {
        return id;
    }

    public String getContents() {
        if (value != null) {
            return value;
        }

        return name;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public Node getParent() {
        return parent;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void addChild(Node child) {

        if (child != null) {
            children.add(child);
            child.parent = this;
        }
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }
}