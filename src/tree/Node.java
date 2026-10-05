package tree;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Node {

    private static final AtomicInteger idSequence =
            new AtomicInteger(1);

    private final int id;
    private final String name;
    private final String value;

    // Phase 2a: generated internal name
    private String generatedName;

    private Node parent;
    private final List<Node> children;

    /**
     * Resets the node ID sequence.
     * The next Node created will have ID 1.
     */
    public static void resetIds() {
        idSequence.set(1);
    }

    /**
     * Constructor for non-terminal nodes.
     */
    public Node(String name) {
        this.id = idSequence.getAndIncrement();
        this.name = name;
        this.value = null;
        this.children = new ArrayList<Node>();
        this.parent = null;
    }

    /**
     * Constructor for terminal nodes.
     */
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

    /**
     * Returns the system-generated name assigned during
     * Phase 2a semantic analysis.
     */
    public String getGeneratedName() {
        return generatedName;
    }

    /**
     * Assigns the system-generated name during Phase 2a.
     */
    public void setGeneratedName(String generatedName) {
        this.generatedName = generatedName;
    }

    public void addChild(Node child) {
        if (child == null) {
            throw new IllegalArgumentException(
                    "Child node cannot be null"
            );
        }

        children.add(child);
        child.parent = this;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }
}