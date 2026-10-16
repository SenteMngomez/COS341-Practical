package generator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import errors.CodeGeneratorException;
import semantic.Symbol;
import semantic.SymbolTable;
import tree.Node;

/**
 * Phase 3a: code generation for SPL algorithms (ALGO, INSTR, ASSIGN, TERM,
 * BRANCH, LOOP, BOOL) into the C-like target language.
 *
 * Functions and function calls are deferred to Phase 3b: a call anywhere in
 * the algorithm raises a CodeGeneratorException.
 *
 * Conventions used throughout:
 * - Every emitted statement ends with " ;" and a newline.
 * - transTerm / transBool / transAlgo / transBranch / transLoop write
 * straight into one shared buffer, in output order.
 * - Generated temporaries are v1, v2, ... and labels are L1, L2, ...
 * (user variables from Phase 2a are sysN, so there are no clashes).
 */
public class CodeGenerator {

    private final Set<String> knownNames = new HashSet<>();

    private StringBuilder out;
    private int varCounter;
    private int labelCounter;

    /** "int" or "float" for the entire program (decided from Phase 2b facts). */
    private String typ;

    public CodeGenerator(SymbolTable symbolTable) {
        for (Symbol symbol : symbolTable.getSymbols()) {
            knownNames.add(symbol.getGeneratedName());
        }
    }

    /*
     * ============================================================
     * PUBLIC ENTRY POINTS
     * ============================================================
     */

    /**
     * Translates the top-level ALGO of the program (the Phase 3a deliverable).
     *
     * @param root the SPL_PROG node (a P node is accepted as well)
     */
    public String generate(Node root) throws CodeGeneratorException {
        Node p = topLevelP(root);
        Node algo = childAt(p, "ALGO", 2);

        reset(root);
        transAlgo(algo);
        return out.toString();
    }

    /**
     * TEST HELPER (not part of 3a): wraps the translated top-level ALGO in
     * a main() together with the declarations of the top-level variables so
     * that the result can be compiled and run. Phase 3b replaces this.
     */
    public String generateRunnable(Node root) throws CodeGeneratorException {
        Node p = topLevelP(root);
        Node vDecl = childAt(p, "V_DECL", 0);
        Node algo = childAt(p, "ALGO", 2);

        reset(root);
        out.append("#include <iostream>\n");
        out.append("int main()\n");
        out.append("{\n");

        Node current = vDecl;
        while (current != null && !current.getChildren().isEmpty()) {
            List<Node> children = current.getChildren();
            out.append(typ).append(' ').append(lookUp(children.get(0))).append(" ;\n");
            current = children.size() > 1 ? children.get(1) : null;
        }

        transAlgo(algo);
        out.append("return 0 ;\n");
        out.append("}\n");
        return out.toString();
    }

    /** The target type chosen by the last generate call ("int" or "float"). */
    public String getType() {
        return typ;
    }

    /*
     * ============================================================
     * ALGO / INSTR
     * ============================================================
     */

    // trans(ALGO) = { trans(INSTR) ; ASCII13 trans(ALGO) } | /* end */
    private void transAlgo(Node algo) throws CodeGeneratorException {
        Node current = algo;

        // Iterative: ALGO is a right-nested chain, one link per instruction.
        while (current != null && !current.getChildren().isEmpty()) {
            List<Node> children = current.getChildren();

            transInstr(children.get(0));
            out.append(" ;\n");

            current = children.size() > 1 ? children.get(1) : null;
        }

        out.append("/* end */\n");
    }

    private void transInstr(Node instr) throws CodeGeneratorException {
        List<Node> children = instr.getChildren();
        if (children.isEmpty()) {
            throw new CodeGeneratorException("Empty INSTR node (id " + instr.getId() + ").");
        }

        Node first = children.get(0);

        switch (first.getName()) {
            case "print":
                transOutp(children.get(1));
                break;

            case "nop":
                out.append("/* nop */");
                break;

            case "comment":
                out.append("/* ").append(stripQuotes(children.get(1).getValue())).append(" */");
                break;

            case "USER-DEFINED-NAME": {
                Node tail = children.get(1);
                if ("ASSIGN".equals(tail.getName())) {
                    transAssign(first, tail);
                } else {
                    throw deferredCall(first);
                }
                break;
            }

            case "BRANCH":
                transBranch(first);
                break;

            case "LOOP":
                transLoop(first);
                break;

            default:
                throw new CodeGeneratorException("Unknown instruction '" + first.getName()
                        + "' (id " + first.getId() + ").");
        }
    }

    // print STRING -> std::cout << "STRING"
    // print ( TERM ) -> typ vN ; code ; std::cout << vN
    private void transOutp(Node outp) throws CodeGeneratorException {
        Node arg = outp.getChildren().get(0);

        if ("STRING".equals(arg.getName())) {
            // The lexeme already contains the surrounding double quotes.
            out.append("std::cout << ").append(arg.getValue());
            return;
        }

        String v = newVar();
        declare(v);
        transTerm(arg, v);
        out.append("std::cout << ").append(v);
    }

    // NAME = TERM -> typ vN ; code ; intern = vN
    private void transAssign(Node nameNode, Node assign) throws CodeGeneratorException {
        Node term = assign.getChildren().get(0);

        String v = newVar();
        declare(v);
        transTerm(term, v);
        out.append(lookUp(nameNode)).append(" = ").append(v);
    }

    /*
     * ============================================================
     * TERM
     * ============================================================
     */

    // trans(TERM, resultVar): emits statements that leave the value in resultVar.
    private void transTerm(Node term, String result) throws CodeGeneratorException {
        List<Node> children = term.getChildren();
        if (children.isEmpty()) {
            throw new CodeGeneratorException("Empty TERM node (id " + term.getId() + ").");
        }

        Node first = children.get(0);

        switch (first.getName()) {
            case "USER-DEFINED-NAME":
                if (children.size() > 1 && "CALL".equals(children.get(1).getName())) {
                    throw deferredCall(first);
                }
                out.append(result).append(" = ").append(lookUp(first)).append(" ;\n");
                break;

            case "NUM":
                out.append(result).append(" = ").append(first.getValue()).append(" ;\n");
                break;

            case "mod":
            case "div":
            case "add":
            case "sub":
            case "mul": {
                String x = newVar();
                String y = newVar();
                declare(x);
                transTerm(children.get(1), x);
                declare(y);
                transTerm(children.get(2), y);
                out.append(result).append(" = ").append(x).append(' ')
                        .append(arithmeticOp(first.getName())).append(' ').append(y).append(" ;\n");
                break;
            }

            case "neg": {
                String x = newVar();
                declare(x);
                transTerm(children.get(1), x);
                out.append(result).append(" = - ").append(x).append(" ;\n");
                break;
            }

            default:
                throw new CodeGeneratorException("Unknown TERM form '" + first.getName()
                        + "' (id " + first.getId() + ").");
        }
    }

    private String arithmeticOp(String name) {
        switch (name) {
            case "mod":
                return "%";
            case "div":
                return "/";
            case "add":
                return "+";
            case "sub":
                return "-";
            default:
                return "*"; // mul
        }
    }

    /*
     * ============================================================
     * BOOL
     * ============================================================
     */

    /**
     * trans(BOOL, T, F): code that ends by jumping to T if the BOOL is true
     * and to F if it is false.
     *
     * @param fall the label that will be emitted immediately after this code
     *             (or null). Jumping to it is redundant, so it is omitted.
     *
     *             Every case ends in a definite jump, so swapping T and F is an
     *             exact
     *             implementation of not(...).
     */
    private void transBool(Node bool, String t, String f, String fall) throws CodeGeneratorException {
        List<Node> children = bool.getChildren();
        Node first = children.get(0);

        switch (first.getName()) {
            case "not":
                transBool(children.get(1), f, t, fall);
                break;

            case "or": {
                // B1 true -> T ; otherwise fall into B2.
                String next = newLabel();
                transBool(children.get(1), t, next, next);
                out.append(next).append(" : /* or */\n");
                transBool(children.get(2), t, f, fall);
                break;
            }

            case "and": {
                // B1 false -> F ; otherwise fall into B2.
                String next = newLabel();
                transBool(children.get(1), next, f, next);
                out.append(next).append(" : /* and */\n");
                transBool(children.get(2), t, f, fall);
                break;
            }

            case "eq":
            case "larger":
            case "lesser": {
                String x = newVar();
                String y = newVar();
                declare(x);
                transTerm(children.get(1), x);
                declare(y);
                transTerm(children.get(2), y);

                String cond = x + " " + comparisonOp(first.getName()) + " " + y;

                if (t.equals(fall)) {
                    // True case falls through: jump away only when false.
                    out.append("if ( ! ( ").append(cond).append(" ) ) goto ").append(f).append(" ;\n");
                } else {
                    out.append("if ( ").append(cond).append(" ) goto ").append(t).append(" ;\n");
                    if (!f.equals(fall)) {
                        out.append("goto ").append(f).append(" ;\n");
                    }
                }
                break;
            }

            default:
                throw new CodeGeneratorException("Unknown BOOL form '" + first.getName()
                        + "' (id " + first.getId() + ").");
        }
    }

    private String comparisonOp(String name) {
        switch (name) {
            case "eq":
                return "==";
            case "larger":
                return ">";
            default:
                return "<"; // lesser
        }
    }

    /*
     * ============================================================
     * BRANCH
     * ============================================================
     */

    // if BOOL then { ALGO_true } else { ALGO_false }
    private void transBranch(Node branch) throws CodeGeneratorException {
        List<Node> children = branch.getChildren();
        Node bool = children.get(0);
        Node algoTrue = children.get(1);
        Node algoFalse = children.get(2);

        String la = newLabel(); // true entrance
        String lb = newLabel(); // else entrance (directly behind the decision code)
        String lc = newLabel(); // exit

        out.append("/* if */\n");
        transBool(bool, la, lb, lb);
        out.append(lb).append(" : /* else */\n");
        transAlgo(algoFalse);
        out.append("goto ").append(lc).append(" ;\n");
        out.append(la).append(" : /* then */\n");
        transAlgo(algoTrue);
        out.append(lc).append(" : /* continue */\n");
    }

    /*
     * ============================================================
     * LOOP
     * ============================================================
     */

    private void transLoop(Node loop) throws CodeGeneratorException {
        List<Node> children = loop.getChildren();
        Node first = children.get(0);

        if ("COND".equals(first.getName())) {
            // COND BOOL do { ALGO }
            String cond = first.getChildren().get(0).getName(); // while | until
            transPreTestLoop(cond, children.get(1), children.get(2));

        } else if ("do".equals(first.getName())) {
            // do { ALGO } COND BOOL
            String cond = children.get(2).getChildren().get(0).getName();
            transPostTestLoop(cond, children.get(3), children.get(1));

        } else {
            throw new CodeGeneratorException("Unknown LOOP form (id " + loop.getId() + ").");
        }
    }

    // until BOOL do { ALGO } (while BOOL == until not(BOOL))
    private void transPreTestLoop(String cond, Node bool, Node body) throws CodeGeneratorException {
        String ls = newLabel();
        String le = newLabel();
        String d = newLabel();

        out.append(ls).append(" : /* ").append(cond).append(" */\n");
        if ("until".equals(cond)) {
            transBool(bool, le, d, d); // condition true -> exit
        } else {
            transBool(bool, d, le, d); // condition false -> exit
        }
        out.append(d).append(" : /* do */\n");
        transAlgo(body);
        out.append("goto ").append(ls).append(" ;\n");
        out.append(le).append(" : /* exit */\n");
    }

    // do { ALGO } while BOOL (do-until BOOL == do-while not(BOOL))
    private void transPostTestLoop(String cond, Node bool, Node body) throws CodeGeneratorException {
        String ld = newLabel();
        String le = newLabel();

        out.append(ld).append(" : /* do */\n");
        transAlgo(body);
        out.append("/* ").append(cond).append(" */\n");
        if ("while".equals(cond)) {
            transBool(bool, ld, le, le); // condition true -> repeat
        } else {
            transBool(bool, le, ld, le); // condition true -> exit
        }
        out.append(le).append(" : /* exit */\n");
    }

    /*
     * ============================================================
     * GENERATED VARIABLES AND LABELS
     * ============================================================
     */

    private String newVar() {
        return "v" + (++varCounter);
    }

    private String newLabel() {
        return "L" + (++labelCounter);
    }

    private void declare(String var) {
        out.append(typ).append(' ').append(var).append(" ;\n");
    }

    /*
     * ============================================================
     * HELPERS
     * ============================================================
     */

    private void reset(Node root) {
        out = new StringBuilder();
        varCounter = 0;
        labelCounter = 0;
        typ = containsFloatFeature(root) ? "float" : "int";
    }

    /*
     * Phase 2b guarantees a program is either all-int or all-float:
     * - div works on floats only, mod on ints only (and they never co-occur),
     * - mod forbids decimal numbers.
     * So: any div or decimal NUM => float, otherwise int.
     */
    private boolean containsFloatFeature(Node root) {
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Node node = stack.pop();

            if ("div".equals(node.getName())) {
                return true;
            }
            if ("NUM".equals(node.getName()) && node.getValue() != null
                    && node.getValue().indexOf('.') >= 0) {
                return true;
            }
            for (Node child : node.getChildren()) {
                stack.push(child);
            }
        }
        return false;
    }

    // lookUp(NAME, SymbT): the Phase 2a internal name, verified against the symbol
    // table.
    private String lookUp(Node nameNode) throws CodeGeneratorException {
        String generated = nameNode.getGeneratedName();
        if (generated == null || !knownNames.contains(generated)) {
            throw new CodeGeneratorException("Name '" + nameNode.getContents()
                    + "' has no symbol-table entry.");
        }
        return generated;
    }

    private CodeGeneratorException deferredCall(Node nameNode) {
        return new CodeGeneratorException("Function call '" + nameNode.getContents()
                + "' cannot be translated yet: functions and calls are handled in Phase 3b.");
    }

    private Node topLevelP(Node root) throws CodeGeneratorException {
        if (root == null) {
            throw new CodeGeneratorException("Cannot generate code for a null syntax tree.");
        }
        if ("P".equals(root.getName())) {
            return root;
        }
        for (Node child : root.getChildren()) {
            if ("P".equals(child.getName())) {
                return child;
            }
        }
        throw new CodeGeneratorException("Invalid syntax tree: no P node found.");
    }

    private Node childAt(Node p, String name, int index) throws CodeGeneratorException {
        List<Node> children = p.getChildren();
        if (children.size() > index && name.equals(children.get(index).getName())) {
            return children.get(index);
        }
        throw new CodeGeneratorException("Invalid P node: expected " + name + " at position " + index + ".");
    }

    private String stripQuotes(String s) {
        if (s != null && s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s == null ? "" : s;
    }
} 