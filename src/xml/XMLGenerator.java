package xml;

import tree.Node;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class XMLGenerator {

    private XMLGenerator() {
        // Utility class
    }

    public static void generate(Node root, String filename)
            throws IOException {

        try (FileWriter writer = new FileWriter(filename)) {

            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

            writeNode(writer, root);
        }
    }

    private static void writeNode(FileWriter writer, Node node)
            throws IOException {

        writer.write("<node>\n");

        writer.write("    <id>"
                + node.getId()
                + "</id>\n");

        writer.write("    <contents>"
                + escapeXML(node.getContents())
                + "</contents>\n");

        if (node.getParent() != null) {
            writer.write("    <parent>"
                    + node.getParent().getId()
                    + "</parent>\n");
        }

        List<Node> children = node.getChildren();

        writer.write("    <children>\n");

        for (Node child : children) {
            writer.write("        <child>"
                    + child.getId()
                    + "</child>\n");
        }

        writer.write("    </children>\n");

        writer.write("</node>\n");

        for (Node child : children) {
            writeNode(writer, child);
        }
    }

    private static String escapeXML(String text) {

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}