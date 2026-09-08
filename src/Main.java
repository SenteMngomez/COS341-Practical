import tree.Node;
import xml.XMLGenerator;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        // Create the root node
        Node root = new Node("SPL_PROG");

        // Create nodes for:
        // P -> V_DECL : F_DECL : ALGO
        Node p = new Node("P");
        Node vDecl = new Node("V_DECL");
        Node colon1 = new Node(":");
        Node fDecl = new Node("F_DECL");
        Node colon2 = new Node(":");
        Node algo = new Node("ALGO");

        // Build the tree
        root.addChild(p);

        p.addChild(vDecl);
        p.addChild(colon1);
        p.addChild(fDecl);
        p.addChild(colon2);
        p.addChild(algo);

        // Generate XML
        try {

            XMLGenerator.generate(root, "tree.xml");

            System.out.println("SUCCESS!");
            System.out.println("tree.xml was generated.");

        } catch (IOException e) {

            System.out.println("ERROR generating tree.xml.");
            e.printStackTrace();
        }
    }
}