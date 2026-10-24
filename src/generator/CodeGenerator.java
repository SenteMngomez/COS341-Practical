package generator;

import errors.CodeGeneratorException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import semantic.Symbol;
import semantic.SymbolTable;
import tree.Node;

/**
 * Phase 3b: code generation for SPL programs into the C-like target language.
 * Function calls are expanded by inlining their function bodies.
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
    private final Map<String, Node> functionDefinitions = new HashMap<>();
    private final Deque<Map<String, String>> nameScopes = new ArrayDeque<>();

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
     * Translates the complete SPL program, including the non-executable
     * function code mine and the executable main algorithm.
     *
     * @param root the SPL_PROG node (a P node is accepted as well)
     */
    public String generate(Node root) throws CodeGeneratorException {
        Node p = topLevelP(root);
        Node vDecl = childAt(p, "V_DECL", 0);
        Node fDecl = childAt(p, "F_DECL", 1);
        Node algo = childAt(p, "ALGO", 2);

        reset(root);
        indexFunctions(root);

        out.append("/* begin non-executable code mine */\n");
        emitFunctionMine(fDecl);
        out.append("/* end non-executable code mine */\n");
        out.append("#include <iostream>\n");
        out.append("int main()\n");
        out.append("{\n");
        emitTopLevelDeclarations(vDecl);
        transAlgo(algo);
        out.append("return 0 ;\n");
        out.append("}\n");
        return out.toString();
    }

    /**
     * Compatibility entry point retained for the existing command-line
     * option. Phase 3b output is executable already.
     */
    public String generateRunnable(Node root) throws CodeGeneratorException {
        return generate(root);
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
                } else if ("CALL".equals(tail.getName())) {
                    transCall(first, tail, null);
                } else {
                    throw new CodeGeneratorException("Unknown instruction tail '"
                            + tail.getName() + "' (id " + tail.getId() + ").");
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

    /** Evaluates arguments and expands one function call at its use site. */
    private void transCall(Node nameNode, Node call, String result)
            throws CodeGeneratorException {
        Node fType = functionDefinitions.get(lookUp(nameNode));

        if (fType == null) {
            throw new CodeGeneratorException("Function '" + nameNode.getContents()
                    + "' has no code-generation definition.");
        }

        List<Node> arguments = inputTerms(call.getChildren().isEmpty()
                ? null : call.getChildren().get(0));

        Node parameters = childByName(fType, "V_DECL");
        List<Node> parameterNodes = declarationNames(parameters);

        if (arguments.size() != parameterNodes.size()) {
            throw new CodeGeneratorException("Function '" + nameNode.getContents()
                    + "' expects " + parameterNodes.size() + " argument(s) but was called with "
                    + arguments.size() + ".");
        }

        List<String> argumentValues = new java.util.ArrayList<>();

        for (Node argument : arguments) {
            String value = newVar();
            declare(value);
            transTerm(argument, value);
            argumentValues.add(value);
        }

        transFunctionInline(fType, parameterNodes, argumentValues, result);
    }

    /**
     * Emits one fresh function expansion; labels come from the shared counter.
     */
    private void transFunctionInline(
            Node fType,
            List<Node> parameterNodes,
            List<String> argumentValues,
            String result
    ) throws CodeGeneratorException {

        Map<String, String> scope = new HashMap<>();
        nameScopes.push(scope);

        for (int i = 0; i < parameterNodes.size(); i++) {
            String parameter = newVar();
            declare(parameter);
            scope.put(lookUpUnscoped(parameterNodes.get(i)), parameter);
            out.append(parameter).append(" = ").append(argumentValues.get(i)).append(" ;\n");
        }

        Node body = childByName(fType, "P");

        if (body == null) {
            nameScopes.pop();
            throw new CodeGeneratorException("Function has no body (id " + fType.getId() + ").");
        }

        emitScopedDeclarations(childByName(body, "V_DECL"), scope);
        transAlgo(childByName(body, "ALGO"));

        if ("num".equals(fType.getChildren().get(0).getName())) {

            Node returnTerm = childByName(fType, "TERM");

            if (returnTerm == null || result == null) {
                nameScopes.pop();
                throw new CodeGeneratorException("Numeric function has no return continuation.");
            }

            String returnValue = newVar();
            declare(returnValue);
            transTerm(returnTerm, returnValue);
            out.append(result).append(" = ").append(returnValue).append(" ;\n");
        }

        nameScopes.pop();
    }

    /** Writes function implementations into the non-executable code mine. */
    private void emitFunctionMine(Node fDecl) throws CodeGeneratorException {

        Node current = fDecl;

        while (current != null && "F_DECL".equals(current.getName())) {

            if (!current.getChildren().isEmpty()) {

                Node fType = current.getChildren().get(0);
                StringBuilder previous = out;
                out = new StringBuilder();

                transFunctionMine(fType);

                String mine = out.toString();
                out = previous;
                appendAsComments(mine);

                Node body = childByName(fType, "P");
                emitFunctionMine(childByName(body, "F_DECL"));
            }

            current = current.getChildren().size() > 1
                    ? current.getChildren().get(1) : null;
        }
    }

    /** Produces the commented representation of one function declaration. */
    private void transFunctionMine(Node fType) throws CodeGeneratorException {

        Node name = childByName(fType, "USER-DEFINED-NAME");

        out.append("function ").append(lookUpUnscoped(name)).append("\n");
        out.append("{\n");

        List<Node> parameters = declarationNames(childByName(fType, "V_DECL"));

        String result = null;
        if ("num".equals(fType.getChildren().get(0).getName())) {
            result = newVar();
            declare(result);
        }

        transFunctionInline(fType, parameters, mineArguments(parameters), result);

        out.append("}\n");
    }

    /** Supplies placeholders while constructing the commented code mine. */
    private List<String> mineArguments(List<Node> parameters) {
        List<String> arguments = new java.util.ArrayList<>();

        for (Node ignored : parameters) {
            arguments.add("/* parameter */ 0");
        }
        return arguments;
    }

    private void appendAsComments(String text) {

        for (String line : text.split("\\R", -1)) {
            out.append("// ").append(line).append('\n');
        }
    }

    private void emitTopLevelDeclarations(Node vDecl) throws CodeGeneratorException {
        Node current = vDecl;

        while (current != null && !current.getChildren().isEmpty()) {
            out.append(typ).append(' ').append(lookUpUnscoped(current.getChildren().get(0)))
                    .append(" = 0 ;\n");
            current = current.getChildren().size() > 1 ? current.getChildren().get(1) : null;
        }
    }

    private void emitScopedDeclarations(Node vDecl, Map<String, String> scope)
            throws CodeGeneratorException {

        Node current = vDecl;
        while (current != null && !current.getChildren().isEmpty()) {
            Node name = current.getChildren().get(0);
            String generated = lookUpUnscoped(name);
            String fresh = newVar();
            scope.put(generated, fresh);
            declareUserVariable(fresh);
            current = current.getChildren().size() > 1 ? current.getChildren().get(1) : null;
        }
    }

    /** Collects the names in a right-nested declaration list. */
    private List<Node> declarationNames(Node vDecl) {

        List<Node> names = new java.util.ArrayList<>();
        Node current = vDecl;

        while (current != null && "V_DECL".equals(current.getName())
                && !current.getChildren().isEmpty()) {
            names.add(current.getChildren().get(0));
            current = current.getChildren().size() > 1 ? current.getChildren().get(1) : null;
        }
        return names;
    }

    /** Collects arguments from the LL(1)-friendly right-nested INPUT list. */
    private List<Node> inputTerms(Node input) {

        List<Node> terms = new java.util.ArrayList<>();
        Node current = input;

        while (current != null && "INPUT".equals(current.getName())
                && !current.getChildren().isEmpty()) {
            terms.add(current.getChildren().get(0));
            current = current.getChildren().size() > 1 ? current.getChildren().get(1) : null;
        }

        return terms;
    }

    private Node childByName(Node node, String name) {
        if (node == null) {
            return null;
        }

        for (Node child : node.getChildren()) {
            if (name.equals(child.getName())) {
                return child;
            }
        }
        return null;
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
                    transCall(first, children.get(1), result);
                    break;
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
                out.append(next).append(" : ; /* or */\n");
                transBool(children.get(2), t, f, fall);
                break;
            }

            case "and": {
                // B1 false -> F ; otherwise fall into B2.
                String next = newLabel();
                transBool(children.get(1), next, f, next);
                out.append(next).append(" : ; /* and */\n");
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
        out.append(lb).append(" : ; /* else */\n");
        transAlgo(algoFalse);
        out.append("goto ").append(lc).append(" ;\n");
        out.append(la).append(" : ; /* then */\n");
        transAlgo(algoTrue);
        out.append(lc).append(" : ; /* continue */\n");
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

        out.append(ls).append(" : ; /* ").append(cond).append(" */\n");
        if ("until".equals(cond)) {
            transBool(bool, le, d, d); // condition true -> exit
        } else {
            transBool(bool, d, le, d); // condition false -> exit
        }
        out.append(d).append(" : ; /* do */\n");
        transAlgo(body);
        out.append("goto ").append(ls).append(" ;\n");
        out.append(le).append(" : ; /* exit */\n");
    }

    // do { ALGO } while BOOL (do-until BOOL == do-while not(BOOL))
    private void transPostTestLoop(String cond, Node bool, Node body) throws CodeGeneratorException {
        String ld = newLabel();
        String le = newLabel();

        out.append(ld).append(" : ; /* do */\n");
        transAlgo(body);
        out.append("/* ").append(cond).append(" */\n");
        if ("while".equals(cond)) {
            transBool(bool, ld, le, le); // condition true -> repeat
        } else {
            transBool(bool, le, ld, le); // condition true -> exit
        }
        out.append(le).append(" : ; /* exit */\n");
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

    private void declareUserVariable(String var) {
        out.append(typ).append(' ').append(var).append(" ;\n");
        out.append(var).append(" = 0 ;\n");
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
        functionDefinitions.clear();
        nameScopes.clear();
    }

    /** Indexes all function declarations by their semantic generated names. */
    private void indexFunctions(Node node) {
        if (node == null) {
            return;
        }

        if ("F_DECL".equals(node.getName())) {
            List<Node> children = node.getChildren();
            if (!children.isEmpty() && "F_TYPE".equals(children.get(0).getName())) {
                Node name = childByName(children.get(0), "USER-DEFINED-NAME");
                if (name != null && name.getGeneratedName() != null) {
                    functionDefinitions.put(name.getGeneratedName(), children.get(0));
                }
            }
        }

        for (Node child : node.getChildren()) {
            indexFunctions(child);
        }
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
        String generated = lookUpUnscoped(nameNode);
        for (Map<String, String> scope : nameScopes) {
            String renamed = scope.get(generated);
            if (renamed != null) {
                return renamed;
            }
        }
        return generated;
    }

    private String lookUpUnscoped(Node nameNode) throws CodeGeneratorException {
        String generated = nameNode.getGeneratedName();
        if (generated == null || !knownNames.contains(generated)) {
            throw new CodeGeneratorException("Name '" + nameNode.getContents()
                    + "' has no symbol-table entry.");
        }
        return generated;
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