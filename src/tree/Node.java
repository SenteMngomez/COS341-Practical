package tree;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Node {
    private static final AtomicInteger idSequence = new AtomicInteger(1);

    public final int id;
    public final String name;      // Non-terminal (e.g. "P", "ALGO") or Terminal
    public final String value;     // Leaf lexeme (e.g. "#x", "42"), null if non-terminal
    public final List<Node> children;
    public Integer parentId;       // Set during XML traversal or parent linkage

    // Constructor for Non-Terminal Nodes (e.g., P, ALGO, ASSIGN)
    public Node(String name) {
        this.id = idSequence.getAndIncrement();
        this.name = name;
        this.value = null;
        this.children = new ArrayList<>();
        this.parentId = null;
    }

    // Constructor for Terminal/Leaf Nodes (e.g., USER-DEFINED-NAME, NUM)
    public Node(String name, String value) {
        this.id = idSequence.getAndIncrement();
        this.name = name;
        this.value = value;
        this.children = new ArrayList<>();
        this.parentId = null;
    }

    public void addChild(Node child) {
        if (child != null) {
            child.parentId = this.id; // Link child directly to this parent ID
            this.children.add(child);
        }
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }
}