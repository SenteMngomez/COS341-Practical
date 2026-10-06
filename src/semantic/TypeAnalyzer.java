package semantic;

import java.util.List;

import errors.SemanticException;
import tree.Node;

public class TypeAnalyzer {

    public static final String UNKNOWN = "unknown";
    public static final String NUMERIC = "numeric";
    public static final String BOOLEAN = "boolean";
    public static final String PROCEDURE = "procedure";
    public static final String OK = "ok";

    private SymbolTable symbolTable;

    public void analyze(Node root, SymbolTable symbolTable) throws SemanticException {
        if (root == null) {
            throw new SemanticException("Cannot type-analyse a null syntax tree.");
        }

        if (symbolTable == null) {
            throw new SemanticException("Cannot type-analyse without a symbol table.");
        }

        this.symbolTable = symbolTable;

        initialiseTypes(root);

        /*
         * Special Phase 2b rule:
         * A tree containing both mod and div is invalid.
         */
        boolean hasMod = containsNode(root, "mod");
        boolean hasDiv = containsNode(root, "div");

        if (hasMod && hasDiv) {
            throw new SemanticException("A float-integer-conflict might perhaps be possible");
        }

        /*
         * If mod occurs anywhere, no decimal NUM may occur.
         */
        if (hasMod && containsDecimalNumber(root)) {
            throw new SemanticException("A decimal number cannot occur in a syntax tree containing mod.");
        }

        analyseNode(root);

        if (!OK.equals(root.getType())) {
            throw new SemanticException( "Type analysis failed: root type is " + root.getType() + ".");
        }
    }

    /*
     * ============================================================
     * GENERAL TREE ANALYSIS
     * ============================================================
     */

    private String analyseNode(Node node) throws SemanticException {
        if (node == null) {
            return UNKNOWN;
        }

        String name = node.getName();

        switch (name) {
            case "SPL_PROG":
                return analyseSPLProg(node);

            case "P":
                return analyseP(node);

            case "V_DECL":
                return analyseVDecl(node);

            case "F_DECL":
                return analyseFDecl(node);

            case "F_TYPE":
                return analyseFType(node);

            case "ALGO":
                return analyseAlgo(node);

            case "INSTR":
                return analyseInstr(node);

            case "OUTP":
                return analyseOutp(node);

            case "CALL":
                return analyseCall(node);

            case "INPUT":
                return analyseInput(node);

            case "ASSIGN":
                return analyseAssign(node);

            case "TERM":
                return analyseTerm(node);

            case "BRANCH":
                return analyseBranch(node);

            case "BOOL":
                return analyseBool(node);

            case "COND":
                return analyseCond(node);

            case "LOOP":
                return analyseLoop(node);

            case "USER-DEFINED-NAME":
                return analyseName(node);

            case "NUM":
                node.setType(NUMERIC);
                return NUMERIC;

            case "STRING":
                node.setType(OK);
                return OK;

            default:
                /*
                 * Punctuation/keyword/terminal nodes do not have
                 * independent semantic types.
                 */
                return UNKNOWN;
        }
    }

    /*
     * ============================================================
     * PROGRAM
     * ============================================================
     */

    private String analyseSPLProg(Node node) throws SemanticException {
        Node p = child(node, "P");
        requireType(analyseNode(p), OK, "P must have type ok.");

        node.setType(OK);
        return OK;
    }

    /*
     * ============================================================
     * P
     * ============================================================
     */

    private String analyseP(Node node) throws SemanticException {
        Node vDecl = child(node, "V_DECL");
        Node fDecl = child(node, "F_DECL");
        Node algo = child(node, "ALGO");

        requireType(analyseNode(vDecl), OK, "Variable declarations are invalid.");
        requireType(analyseNode(fDecl),OK,"Function declarations are invalid.");
        requireType(analyseNode(algo),OK, "Algorithm is invalid.");

        node.setType(OK);
        return OK;
    }

    /*
     * ============================================================
     * VARIABLE DECLARATIONS
     * ============================================================
     */

    private String analyseVDecl(Node node) throws SemanticException {
        if (node.getChildren().isEmpty()) {
            node.setType(OK);
            return OK;
        }

        Node name = node.getChildren().get(0);

        if (!"USER-DEFINED-NAME".equals(name.getName())) {
            throw new SemanticException("Invalid variable declaration.");
        }

        /*
         * Every declared variable is numeric.
         */
        setNameType(name, NUMERIC);

        if (node.getChildren().size() > 1) {
            requireType(analyseNode(node.getChildren().get(1)), OK,"Invalid variable declaration.");
        }
        node.setType(OK);
        return OK;
    }

    /*
     * ============================================================
     * FUNCTION DECLARATIONS
     * ============================================================
     */

    private String analyseFDecl(Node node) throws SemanticException {
        if (node.getChildren().isEmpty()) {
            node.setType(OK);
            return OK;
        }

        requireType(analyseNode(node.getChildren().get(0)),OK,"Invalid function declaration.");

        if (node.getChildren().size() > 1) {
            requireType(analyseNode(node.getChildren().get(1)), OK,"Invalid function declaration.");
        }
        node.setType(OK);
        return OK;
    }

    private String analyseFType(Node node) throws SemanticException {
        Node name = findUserDefinedName(node);
        Node vDecl = null;
        Node p = null;
        Node term = null;

        for (Node child : node.getChildren()) {
            if ("V_DECL".equals(child.getName())) {
                vDecl = child;
            }

            if ("P".equals(child.getName())) {
                p = child;
            }

            if ("TERM".equals(child.getName())) {
                term = child;
            }
        }

        if (name == null || p == null || vDecl == null) {
            throw new SemanticException("Invalid function declaration.");
        }

        /*
         * Determine whether this is void or num from
         * the first child of F_TYPE.
         */
        boolean isVoid = hasChild(node, "void");

        /*
         * Give the function its expected type before analysing
         * its body. This allows calls to functions declared later
         * in the program to be checked.
         */
        Symbol functionSymbol = symbolFor(name);

        if (isVoid) {
            functionSymbol.setType(PROCEDURE);
        } else {
            functionSymbol.setType(NUMERIC);
        }

        requireType(analyseNode(vDecl), OK, "Function parameters are invalid.");
        requireType(analyseNode(p), OK, "Function body is invalid.");

        if (!isVoid) {
            if (term == null) {
                throw new SemanticException("Numeric function has no return expression.");
            }
            requireType(analyseNode(term), NUMERIC, "Numeric function must return a numeric TERM.");
        }

        node.setType(OK);
        return OK;
    }

    /*
     * ============================================================
     * ALGORITHM
     * ============================================================
     */

    private String analyseAlgo(Node node) throws SemanticException {

        if (node.getChildren().isEmpty()) {
            node.setType(OK);
            return OK;
        }

        for (Node child : node.getChildren()) {

            if ("INSTR".equals(child.getName())
                    || "ALGO".equals(child.getName())) {

                requireType(
                        analyseNode(child),
                        OK,
                        "Invalid algorithm instruction."
                );
            }
        }

        node.setType(OK);
        return OK;
    }

    /*
     * ============================================================
     * INSTRUCTION
     * ============================================================
     */

    private String analyseInstr(Node node)throws SemanticException {

        List<Node> children = node.getChildren();

        for (int i = 0; i < children.size(); i++) {

            Node child = children.get(i);
            String name = child.getName();

            // print
            if ("OUTP".equals(name)) {
                requireType(
                        analyseNode(child),
                        OK,
                        "Invalid output instruction."
                );
            }

            // assignment or function call starts with a NAME
            else if ("USER-DEFINED-NAME".equals(name)) {

                Symbol symbol = symbolFor(child);

                Node next = (i + 1 < children.size())
                        ? children.get(i + 1)
                        : null;

                if (next == null) {
                    throw new SemanticException(
                            "Invalid instruction after name '"
                                    + child.getContents() + "'."
                    );
                }

                // NAME ( INPUT )
                if ("CALL".equals(next.getName())) {

                    String callType = analyseCallWithName(next, symbol);

                    // Instruction-level calls must be procedures.
                    requireType(
                            callType,
                            PROCEDURE,
                            "Instruction-level function call must be a procedure."
                    );
                }

                // NAME = TERM
                else if ("ASSIGN".equals(next.getName())) {

                    requireType(
                            symbol.getType(),
                            NUMERIC,
                            "Assignment target must be numeric."
                    );

                    requireType(
                            analyseNode(next),
                            OK,
                            "Invalid assignment."
                    );
                }

                else {
                    throw new SemanticException(
                            "Invalid instruction after name '"
                                    + child.getContents() + "'."
                    );
                }
            }

            // branch
            else if ("BRANCH".equals(name)) {
                requireType(
                        analyseNode(child),
                        OK,
                        "Invalid branch."
                );
            }

            // loop
            else if ("LOOP".equals(name)) {
                requireType(
                        analyseNode(child),
                        OK,
                        "Invalid loop."
                );
            }

            // nop / comment / other terminals
            else if ("NOP".equals(name)
                    || "nop".equals(name)
                    || "COMMENT".equals(name)
                    || "comment".equals(name)
                    || "STRING".equals(name)) {
                // These are already valid instructions.
            }
        }

        node.setType(OK);
        return OK;
    }

    private String analyseOutp(Node node) throws SemanticException {
        if (node.getChildren().isEmpty()) {
            throw new SemanticException("Invalid output expression.");
        }

        for (Node child : node.getChildren()) {
            if ("TERM".equals(child.getName())) {
                requireType(
                        analyseNode(child),
                        NUMERIC,
                        "Output TERM must be numeric."
                );
            } else if ("STRING".equals(child.getName())) {
                requireType(
                        analyseNode(child),
                        OK,
                        "Invalid output string."
                );
            }
        }

        node.setType(OK);
        return OK;
    }

    /*
     * ============================================================
     * ASSIGNMENT
     * ============================================================
     */

    private String analyseAssign(Node node)
            throws SemanticException {

        /*
         * ASSIGN contains only TERM because the NAME is the
         * preceding child of INSTR.
         */
        Node term = child(node, "TERM");

        requireType(
                analyseNode(term),
                NUMERIC,
                "Assignment requires a numeric TERM."
        );

        node.setType(OK);
        return OK;
    }

    /*
     * ============================================================
     * TERMS
     * ============================================================
     */

    private String analyseTerm(Node node)
            throws SemanticException {

        if (node.getChildren().isEmpty()) {
            throw new SemanticException(
                    "Invalid TERM."
            );
        }

        Node first = node.getChildren().get(0);
        String name = first.getName();

        /*
         * TERM -> NAME TERM_REST
         */
        if ("USER-DEFINED-NAME".equals(name)) {

            Symbol symbol = symbolFor(first);

            /*
             * If TERM_REST contains a CALL, this is a function call.
             */
            Node rest = child(node, "TERM_REST");

            if (rest != null && !rest.getChildren().isEmpty()) {

                Node call = child(rest, "CALL");

                if (call != null) {
                    return analyseCallWithName(
                            call,
                            symbol
                    );
                }
            }

            if (!NUMERIC.equals(symbol.getType())) {
                throw new SemanticException(
                        "Variable '" + first.getContents()
                                + "' is not numeric."
                );
            }

            node.setType(NUMERIC);
            return NUMERIC;
        }

        /*
         * TERM -> NUM
         */
        if ("NUM".equals(name)) {
            node.setType(NUMERIC);
            return NUMERIC;
        }

        /*
         * Binary arithmetic operators.
         */
        if ("mod".equals(name)
                || "div".equals(name)
                || "add".equals(name)
                || "sub".equals(name)
                || "mul".equals(name)) {

            List<Node> terms = childrenNamed(node, "TERM");

            if (terms.size() != 2) {
                throw new SemanticException(
                        "Arithmetic operation requires two TERM operands."
                );
            }

            requireType(
                    analyseNode(terms.get(0)),
                    NUMERIC,
                    "Arithmetic operands must be numeric."
            );

            requireType(
                    analyseNode(terms.get(1)),
                    NUMERIC,
                    "Arithmetic operands must be numeric."
            );

            node.setType(NUMERIC);
            return NUMERIC;
        }

        /*
         * TERM -> neg ( TERM )
         */
        if ("neg".equals(name)) {

            Node term = child(node, "TERM");

            requireType(
                    analyseNode(term),
                    NUMERIC,
                    "neg requires a numeric TERM."
            );

            node.setType(NUMERIC);
            return NUMERIC;
        }

        throw new SemanticException(
                "Unknown TERM form: " + name
        );
    }

    /*
     * ============================================================
     * CALL
     * ============================================================
     */

    private String analyseCall(Node node)
            throws SemanticException {

        /*
         * In the parser tree the CALL inside INSTR does not
         * contain the function NAME. The NAME is its preceding
         * INSTR child.
         *
         * Therefore this method is mainly used for calls that
         * occur inside TERM where the name has already been found.
         */
        Node input = child(node, "INPUT");

        if (input != null) {
            requireType(
                    analyseNode(input),
                    OK,
                    "Invalid function call input."
            );
        }

        node.setType(OK);
        return OK;
    }

    private String analyseCallWithName(
            Node call,
            Symbol function
    ) throws SemanticException {

        if (!function.isFunction()) {
            throw new SemanticException(
                    "'" + function.getOriginalName()
                            + "' is not a function."
            );
        }

        Node input = child(call, "INPUT");

        if (input != null) {
            requireType(
                    analyseNode(input),
                    OK,
                    "Invalid function call input."
            );
        }

        /*
         * A void function is a procedure.
         * A num function is a numeric TERM.
         */
        if (PROCEDURE.equals(function.getType())) {
            call.setType(PROCEDURE);
            return PROCEDURE;
        }

        if (NUMERIC.equals(function.getType())) {
            call.setType(NUMERIC);
            return NUMERIC;
        }

        throw new SemanticException(
                "Function '" + function.getOriginalName()
                        + "' has unknown type."
        );
    }

    /*
     * ============================================================
     * INPUT
     * ============================================================
     */

    private String analyseInput(Node node)
            throws SemanticException {

        if (node.getChildren().isEmpty()) {
            node.setType(OK);
            return OK;
        }

        for (Node child : node.getChildren()) {

            if ("TERM".equals(child.getName())) {

                requireType(
                        analyseNode(child),
                        NUMERIC,
                        "Function arguments must be numeric."
                );
            }

            else if ("INPUT".equals(child.getName())) {

                requireType(
                        analyseNode(child),
                        OK,
                        "Invalid function input."
                );
            }
        }

        node.setType(OK);
        return OK;
    }

    /*
     * ============================================================
     * BOOLEAN EXPRESSIONS
     * ============================================================
     */

    private String analyseBool(Node node)
            throws SemanticException {

        Node operation = firstNonStructuralChild(node);

        if (operation == null) {
            throw new SemanticException(
                    "Invalid BOOL expression."
            );
        }

        String op = operation.getName();

        /*
         * not ( BOOL )
         */
        if ("not".equals(op)) {

            List<Node> bools = childrenNamed(node, "BOOL");

            if (bools.size() != 1) {
                throw new SemanticException(
                        "not requires one BOOL operand."
                );
            }

            requireType(
                    analyseNode(bools.get(0)),
                    BOOLEAN,
                    "not requires a boolean operand."
            );

            node.setType(BOOLEAN);
            return BOOLEAN;
        }

        /*
         * and / or
         */
        if ("and".equals(op)
                || "or".equals(op)) {

            List<Node> bools = childrenNamed(node, "BOOL");

            if (bools.size() != 2) {
                throw new SemanticException(
                        op + " requires two BOOL operands."
                );
            }

            requireType(
                    analyseNode(bools.get(0)),
                    BOOLEAN,
                    op + " requires boolean operands."
            );

            requireType(
                    analyseNode(bools.get(1)),
                    BOOLEAN,
                    op + " requires boolean operands."
            );

            node.setType(BOOLEAN);
            return BOOLEAN;
        }

        /*
         * Comparisons.
         */
        if ("eq".equals(op)
                || "larger".equals(op)
                || "lesser".equals(op)) {

            List<Node> terms = childrenNamed(node, "TERM");

            if (terms.size() != 2) {
                throw new SemanticException(
                        op + " requires two TERM operands."
                );
            }

            requireType(
                    analyseNode(terms.get(0)),
                    NUMERIC,
                    "Comparison operands must be numeric."
            );

            requireType(
                    analyseNode(terms.get(1)),
                    NUMERIC,
                    "Comparison operands must be numeric."
            );

            node.setType(BOOLEAN);
            return BOOLEAN;
        }

        throw new SemanticException(
                "Unknown BOOL operation: " + op
        );
    }

    /*
     * ============================================================
     * BRANCH
     * ============================================================
     */

    private String analyseBranch(Node node)
            throws SemanticException {

        Node bool = child(node, "BOOL");
        List<Node> algos = childrenNamed(node, "ALGO");

        requireType(
                analyseNode(bool),
                BOOLEAN,
                "Branch condition must be boolean."
        );

        if (algos.size() != 2) {
            throw new SemanticException(
                    "Branch must contain two algorithms."
            );
        }

        requireType(
                analyseNode(algos.get(0)),
                OK,
                "Then branch is invalid."
        );

        requireType(
                analyseNode(algos.get(1)),
                OK,
                "Else branch is invalid."
        );

        node.setType(OK);
        return OK;
    }

    /*
     * ============================================================
     * CONDITION
     * ============================================================
     */

    private String analyseCond(Node node) {

        node.setType(OK);
        return OK;
    }

    /*
     * ============================================================
     * LOOP
     * ============================================================
     */

    private String analyseLoop(Node node)
            throws SemanticException {

        Node cond = child(node, "COND");
        Node bool = child(node, "BOOL");
        Node algo = child(node, "ALGO");

        requireType(
                analyseNode(cond),
                OK,
                "Loop condition is invalid."
        );

        requireType(
                analyseNode(bool),
                BOOLEAN,
                "Loop condition BOOL must be boolean."
        );

        requireType(
                analyseNode(algo),
                OK,
                "Loop body is invalid."
        );

        node.setType(OK);
        return OK;
    }

    /*
     * ============================================================
     * NAMES / SYMBOL TABLE
     * ============================================================
     */

    private String analyseName(Node node)
            throws SemanticException {

        Symbol symbol = symbolFor(node);

        if (symbol == null) {
            throw new SemanticException(
                    "No symbol-table entry for '"
                            + node.getContents() + "'."
            );
        }

        node.setType(symbol.getType());

        return symbol.getType();
    }

    private void setNameType(Node node, String type)
            throws SemanticException {

        Symbol symbol = symbolFor(node);

        if (symbol == null) {
            throw new SemanticException(
                    "No symbol-table entry for '"
                            + node.getContents() + "'."
            );
        }

        symbol.setType(type);
        node.setType(type);
    }

    private Symbol symbolFor(Node node)
            throws SemanticException {

        String generatedName = node.getGeneratedName();

        if (generatedName == null) {
            throw new SemanticException(
                    "Name '" + node.getContents()
                            + "' has not been resolved by Phase 2a."
            );
        }

        Symbol symbol =
                symbolTable.findByGeneratedName(generatedName);

        if (symbol == null) {
            throw new SemanticException(
                    "Generated name '" + generatedName
                            + "' was not found in the symbol table."
            );
        }

        return symbol;
    }

    /*
     * ============================================================
     * TYPE HELPERS
     * ============================================================
     */

    private void requireType(
            String actual,
            String expected,
            String message
    ) throws SemanticException {

        if (!expected.equals(actual)) {
            throw new SemanticException(
                    message
                            + " Expected "
                            + expected
                            + " but found "
                            + actual
                            + "."
            );
        }
    }

    private void initialiseTypes(Node node) {

        node.setType(UNKNOWN);

        for (Node child : node.getChildren()) {
            initialiseTypes(child);
        }
    }

    /*
     * ============================================================
     * TREE HELPERS
     * ============================================================
     */

    private Node child(Node node, String name) {

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

    private boolean hasChild(Node node, String name) {

        return child(node, name) != null;
    }

    private Node findUserDefinedName(Node node) {

        for (Node child : node.getChildren()) {

            if ("USER-DEFINED-NAME".equals(child.getName())) {
                return child;
            }
        }

        return null;
    }

    private Node firstNonStructuralChild(Node node) {

        for (Node child : node.getChildren()) {

            String name = child.getName();

            if (!"BOOL".equals(name)
                    && !"TERM".equals(name)) {

                return child;
            }
        }

        return null;
    }

    private List<Node> childrenNamed(
            Node node,
            String name
    ) {

        List<Node> result = new java.util.ArrayList<>();

        for (Node child : node.getChildren()) {

            if (name.equals(child.getName())) {
                result.add(child);
            }
        }

        return result;
    }

    private boolean containsNode(
            Node node,
            String name
    ) {

        if (name.equals(node.getName())) {
            return true;
        }

        for (Node child : node.getChildren()) {

            if (containsNode(child, name)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsDecimalNumber(Node node) {

        if ("NUM".equals(node.getName())
                && node.getValue() != null
                && node.getValue().contains(".")) {

            return true;
        }

        for (Node child : node.getChildren()) {

            if (containsDecimalNumber(child)) {
                return true;
            }
        }

        return false;
    }
}