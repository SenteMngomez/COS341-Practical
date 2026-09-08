package src;
import java.io.IOException;

import src.tree.Node;
import src.xml.XMLGenerator;

public class Main {

    public static void main(String[] args) {

        Node start = new Node("SPL_PROG");
        Node p = new Node("P");

        Node vDecl = new Node("V_DECL");
        Node colon1 = new Node(":");
        Node fDecl = new Node("F_DECL");
        Node colon2 = new Node(":");
        Node algo = new Node("ALGO");

        start.addChild(p);

        p.addChild(vDecl);
        p.addChild(colon1);
        p.addChild(fDecl);
        p.addChild(colon2);
        p.addChild(algo);

        try {
            XMLGenerator.generate(start, "tree.xml");

            System.out.println("tree.xml generated successfully!");

        } catch (IOException e) {
            System.out.println("Could not generate tree.xml");
            e.printStackTrace();
        }
    }
}