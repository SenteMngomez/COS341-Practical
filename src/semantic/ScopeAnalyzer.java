package semantic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tree.Node;
import errors.SemanticException;

public class ScopeAnalyzer {

    private final SymbolTable symbolTable;
    private int generatedNameCounter;

    /*
     * Maps each P node to its scope.
     *
     * The P at the top level is scope 0.
     * A function's P is the function's scope.
     */
    private final Map<Node, Scope> scopes;

    /*
     * Used for detecting direct and indirect recursion.
     */
    private final Map<Symbol, Set<Symbol>> functionCalls;

    public ScopeAnalyzer() {
        symbolTable = new SymbolTable();
        generatedNameCounter = 1;
        scopes = new HashMap<>();
        functionCalls = new HashMap<>();
    }

    public SymbolTable analyze(Node root)
            throws SemanticException {

        if (root == null) {
            throw new SemanticException("Cannot analyse a null syntax tree.");
        }

        symbolTable.getSymbols().clear();
        scopes.clear();
        functionCalls.clear();
        generatedNameCounter = 1;

        Node p = findFirstChild(root, "P");

        if (p == null) {
            throw new SemanticException("Invalid syntax tree: no P node found.");
        }

        Scope globalScope = new Scope(null, 0, null);
        scopes.put(p, globalScope);
        analyseP(p, globalScope);
        detectRecursion();
        return symbolTable;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    /*
     * ============================================================
     * P / SCOPE ANALYSIS
     * ============================================================
     */

    private void analyseP(Node p, Scope scope)
            throws SemanticException {

        if (p == null) {
            return;
        }

        List<Node> children = p.getChildren();

        /*
         * P:
         *
         * V_DECL : F_DECL : ALGO
         *
         * The parser does not store the punctuation nodes.
         */

        Node vDecl = getChild(children, "V_DECL", 0);
        Node fDecl = getChild(children, "F_DECL", 1);
        Node algo = getChild(children, "ALGO", 2);

        /*
         * Variables declared directly in this P belong
         * to this scope.
         */
        if (vDecl != null) {
            collectVariables(vDecl, scope);
        }

        /*
         * Functions declared directly underneath this P
         * belong to this scope.
         */
        if (fDecl != null) {
            collectFunctions(fDecl, scope);
        }

        /*
         * Analyse the function bodies.
         */
        if (fDecl != null) {
            analyseFunctions(fDecl, scope);
        }

        /*
         * Finally resolve names in this algorithm.
         */
        if (algo != null) {
            analyseAlgorithm(algo, scope);
        }
    }

    /*
     * ============================================================
     * VARIABLE DECLARATIONS
     * ============================================================
     */

    private void collectVariables(Node vDecl, Scope scope) throws SemanticException {

        Node current = vDecl;
        while (current != null && "V_DECL".equals(current.getName())) {

            List<Node> children = current.getChildren();
            /*
             * V_DECL -> NAME V_DECL
             *
             * or
             *
             * V_DECL -> epsilon
             */
            if (children.isEmpty()) {
                return;
            }

            Node nameNode = children.get(0);

            if (!isUserDefinedName(nameNode)) {
                throw new SemanticException( "Invalid variable declaration.");
            }

            String name = nameNode.getContents();

            /*
             * A variable may not be declared twice
             * at the same scope.
             */
            if (scope.variables.containsKey(name)) {
                throw new SemanticException("Variable '" + name + "' has more than one declaration " + "at scope level " + scope.level + ".");
            }

            /*
             * Function parameters are stored in the same
             * scope as the function body's local V_DECL.
             *
             * Therefore this also detects a local variable
             * masking a parameter.
             */
            if (scope.parameters.containsKey(name)) {
                throw new SemanticException("Variable '" + name + "' is masking a function parameter.");
            }

            String generatedName = generateName();

            Symbol symbol = new Symbol(name, generatedName, false, scope.level, nameNode);

            scope.variables.put(name, symbol);
            symbolTable.add(symbol);

            nameNode.setGeneratedName(generatedName);

            if (children.size() < 2) {
                return;
            }

            current = children.get(1);
        }
    }

    /*
     * ============================================================
     * FUNCTION DECLARATIONS
     * ============================================================
     */

    private void collectFunctions(Node fDecl, Scope scope) throws SemanticException {

        Node current = fDecl;
        while (current != null && "F_DECL".equals(current.getName())) {

            List<Node> children = current.getChildren();
            /*
             * F_DECL -> epsilon
             */
            if (children.isEmpty()) {
                return;
            }

            Node fType = children.get(0);

            if (!"F_TYPE".equals(fType.getName())) {
                throw new SemanticException("Invalid function declaration.");
            }

            Node nameNode = findFunctionName(fType);

            if (nameNode == null) {
                throw new SemanticException("Function declaration has no name.");
            }

            String name = nameNode.getContents();

            /*
             * Function names must be unique underneath
             * the same F_DECL.
             */
            if (scope.functions.containsKey(name)) {
                throw new SemanticException("Function '" + name + "' has more than one declaration " + "at scope level " + scope.level + ".");
            }

            String generatedName = generateName();

            Symbol symbol = new Symbol(name, generatedName, true, scope.level,nameNode);

            scope.functions.put(name, symbol);
            symbolTable.add(symbol);

            nameNode.setGeneratedName(generatedName);

            functionCalls.put(symbol, new HashSet<>());

            if (children.size() < 2) {
                return;
            }

            current = children.get(1);
        }
    }

    /*
     * ============================================================
     * FUNCTION BODIES
     * ============================================================
     */

    private void analyseFunctions(Node fDecl, Scope parentScope) throws SemanticException {

        Node current = fDecl;
        while (current != null && "F_DECL".equals(current.getName())) {

            List<Node> children = current.getChildren();

            if (children.isEmpty()) {
                return;
            }

            Node fType = children.get(0);

            /*
             * Each function has its own scope.
             *
             * The function P is at parentScope.level + 1.
             */
            Node functionP = findFirstChild(fType, "P");

            if (functionP == null) {
                throw new SemanticException( "Function has no P body.");
            }

            Scope functionScope = new Scope( parentScope, parentScope.level + 1, getFunctionSymbol(fType, parentScope));

            scopes.put(functionP, functionScope);

            /*
             * Function parameters live in the same scope
             * as the function's local variables.
             */
            Node parameterDecl = findParameterVDecl(fType);

            if (parameterDecl != null) {
                collectParameters(parameterDecl, functionScope);
            }

            /*
             * The P contains local V_DECL, nested F_DECL
             * and ALGO.
             */
            analyseP(functionP, functionScope);

            /*
             * num functions: names inside "return ( TERM )" live in the
             * function's scope and must be resolved too.
             */
            Node returnTerm = findFirstChild(fType, "TERM");
            if (returnTerm != null) {
                analyseNodeForNames(returnTerm, functionScope);
            }

            if (children.size() < 2) {
                return;
            }

            current = children.get(1);
        }
    }

    private void collectParameters( Node vDecl, Scope scope) throws SemanticException {

        Node current = vDecl;

        while (current != null && "V_DECL".equals(current.getName())) {

            List<Node> children = current.getChildren();

            if (children.isEmpty()) {
                return;
            }

            Node nameNode = children.get(0);

            if (!isUserDefinedName(nameNode)) {
                throw new SemanticException("Invalid function parameter declaration.");
            }

            String name = nameNode.getContents();

            if (scope.parameters.containsKey(name)) {
                throw new SemanticException("Function parameter '" + name + "' has more than one declaration.");
            }

            String generatedName = generateName();

            Symbol symbol = new Symbol(name, generatedName, false, scope.level,  nameNode);

            scope.parameters.put(name, symbol);
            symbolTable.add(symbol);

            nameNode.setGeneratedName(generatedName);

            if (children.size() < 2) {
                return;
            }

            current = children.get(1);
        }
    }

    /*
     * ============================================================
     * ALGORITHM / NAME RESOLUTION
     * ============================================================
     */

    private void analyseAlgorithm(Node algo,Scope scope) throws SemanticException {

        if (algo == null) {
            return;
        }

        /*
         * ALGO can be deeply nested:
         *
         * ALGO -> INSTR ; ALGO
         */
        for (Node child : algo.getChildren()) {

            if ("INSTR".equals(child.getName())) {
                analyseInstruction(child, scope);
            }

            else if ("ALGO".equals(child.getName())) {
                analyseAlgorithm(child, scope);
            }
        }
    }

    private void analyseInstruction(Node instr, Scope scope) throws SemanticException {

        List<Node> children = instr.getChildren();

        for (int i = 0; i < children.size(); i++) {

            Node child = children.get(i);

            /*
             * An INSTR starting with a USER-DEFINED-NAME is
             * either an assignment or a function call.
             */
            if (isUserDefinedName(child)) {

                Node next = i + 1 < children.size() ? children.get(i + 1): null;

                if (next != null && "CALL".equals(next.getName())) {

                    resolveFunctionCall(child,next,scope);
                }
                else if (next != null && "ASSIGN".equals(next.getName())) {
                    resolveVariable(child,scope);
                    analyseNodeForNames(next,scope);
                }
                else {
                    resolveVariable(child,scope);
                }
            }
            else {
                analyseNodeForNames(child,scope);
            }
        }
    }

    private void analyseNodeForNames(Node node,Scope scope) throws SemanticException {

        if (node == null) {
            return;
        }

        /*
         * Don't treat declaration names as uses.
         * They were already handled while collecting
         * declarations.
         */
        if ("V_DECL".equals(node.getName()) || "F_DECL".equals(node.getName()) || "F_TYPE".equals(node.getName())) {
            return;
        }

        /*
         * Nested INSTR nodes (inside BRANCH / LOOP bodies) must go through
         * analyseInstruction so that "NAME CALL" is resolved as a function call.
         */
        if ("INSTR".equals(node.getName())) {
            analyseInstruction(node, scope);
            return;
        }

        /*
         * TERM -> NAME CALL : a function call used as a term. The function
         * name is a sibling of the CALL node, not a child of it.
         */
        if ("TERM".equals(node.getName())) {
            List<Node> kids = node.getChildren();
            if (kids.size() >= 2 && isUserDefinedName(kids.get(0)) && "CALL".equals(kids.get(1).getName())) {
                resolveFunctionCall(kids.get(0), kids.get(1), scope);
                return;
            }
        }

        /*
         * A CALL has a NAME as its first child.
         */
        if ("CALL".equals(node.getName())) {

            List<Node> children = node.getChildren();

            if (!children.isEmpty() && isUserDefinedName(children.get(0))) {
                resolveFunctionCall(children.get(0),node,scope);
            }

            /*
             * Then analyse its INPUT.
             */
            for (Node child : children) {
                if ("INPUT".equals(child.getName())) {
                    analyseInput(child, scope);
                }
            }

            return;
        }

        /*
         * A normal USER-DEFINED-NAME is a variable use.
         */
        if (isUserDefinedName(node)) {
            resolveVariable(node, scope);
            return;
        }

        for (Node child : node.getChildren()) {
            analyseNodeForNames(child, scope);
        }
    }

    private void analyseInput(Node input,Scope scope) throws SemanticException {
        if (input == null) {
            return;
        }

        for (Node child : input.getChildren()) {
            analyseNodeForNames(child, scope);
        }
    }

    /*
     * ============================================================
     * VARIABLE RESOLUTION
     * ============================================================
     */

    private void resolveVariable(Node nameNode, Scope scope) throws SemanticException {
        String name = nameNode.getContents();
        Symbol symbol = findVariable(name, scope);

        if (symbol == null) {
            throw new SemanticException("Variable '" + name + "' has no declaration.");
        }
        nameNode.setGeneratedName(symbol.getGeneratedName());
    }

    private Symbol findVariable(String name,Scope scope) {
        Scope current = scope;
        while (current != null) {
            Symbol parameter = current.parameters.get(name);
            if (parameter != null) {
                return parameter;
            }

            Symbol variable = current.variables.get(name);
            if (variable != null) {
                return variable;
            }

            current = current.parent;
        }

        return null;
    }

    /*
     * ============================================================
     * FUNCTION RESOLUTION
     * ============================================================
     */

    private void resolveFunctionCall(Node nameNode,Node callNode,Scope scope) throws SemanticException {
        String name = nameNode.getContents();
        Symbol function = findFunction(name, scope);

        if (function == null) {
            throw new SemanticException("Function '" + name+ "' has no declaration.");
        }

        nameNode.setGeneratedName(function.getGeneratedName());
        /*
         * Find the function containing this call.
         */
        Symbol currentFunction = scope.functionSymbol;

        if (currentFunction != null) {
            functionCalls.computeIfAbsent(currentFunction, key -> new HashSet<>()).add(function);
        }

        /*
         * Function call arguments are variables/terms,
         * so analyse them as normal names.
         */
        for (Node child : callNode.getChildren()) {

            if ("INPUT".equals(child.getName())) {
                analyseInput(child, scope);
            }
        }
    }

    private Symbol findFunction(String name, Scope scope) {
        // Spec 2a: a called function must be listed in the F_DECL at the SAME scope level.
        return scope.functions.get(name);
    }

    /*
     * ============================================================
     * RECURSION DETECTION
     * ============================================================
     */

    private void detectRecursion() throws SemanticException {
        Set<Symbol> visiting = new HashSet<>();
        Set<Symbol> visited = new HashSet<>();

        for (Symbol function : functionCalls.keySet()) {

            if (hasCycle(function, visiting,visited)) {
                throw new SemanticException("Function recursion is not allowed: " + function.getOriginalName());
            }
        }
    }

    private boolean hasCycle(Symbol function, Set<Symbol> visiting, Set<Symbol> visited) {
        if (visiting.contains(function)) {
            return true;
        }

        if (visited.contains(function)) {
            return false;
        }

        visiting.add(function);

        Set<Symbol> calls = functionCalls.get(function);

        if (calls != null) {
            for (Symbol called : calls) {
                if (hasCycle(called, visiting, visited)) {
                    return true;
                }
            }
        }

        visiting.remove(function);
        visited.add(function);

        return false;
    }

    /*
     * ============================================================
     * HELPERS
     * ============================================================
     */

    private String generateName() {
        return "sys" + generatedNameCounter++;
    }

    private boolean isUserDefinedName(Node node) {
        return node != null && "USER-DEFINED-NAME".equals(node.getName());
    }

    private Node findFirstChild( Node node, String name) {
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

    private Node getChild(List<Node> children, String name, int index) {
        if (children.size() > index && name.equals(children.get(index).getName())) {
            return children.get(index);
        }
        return findFirstChild(children.isEmpty() ? null : children.get(0).getParent(),name);
    }

    private Node findFunctionName(Node fType) {

        for (Node child : fType.getChildren()) {

            if (isUserDefinedName(child)) {
                return child;
            }
        }

        return null;
    }

    private Node findParameterVDecl(Node fType) {

        boolean foundName = false;

        for (Node child : fType.getChildren()) {

            if (isUserDefinedName(child)) {
                foundName = true;
                continue;
            }

            if (foundName && "V_DECL".equals(child.getName())) {
                return child;
            }
        }

        return null;
    }

    private Symbol getFunctionSymbol(
            Node fType,
            Scope parentScope
    ) throws SemanticException {

        Node nameNode = findFunctionName(fType);

        if (nameNode == null) {
            throw new SemanticException(
                    "Function has no name."
            );
        }

        Symbol symbol = parentScope.functions.get(
                nameNode.getContents()
        );

        if (symbol == null) {
            throw new SemanticException(
                    "Function '"
                            + nameNode.getContents()
                            + "' was not registered."
            );
        }

        return symbol;
    }

    /*
     * ============================================================
     * INTERNAL SCOPE CLASS
     * ============================================================
     */

    private static class Scope {

        private final Scope parent;
        private final int level;
        private final Symbol functionSymbol;

        private final Map<String, Symbol> variables;
        private final Map<String, Symbol> parameters;
        private final Map<String, Symbol> functions;

        private Scope(
                Scope parent,
                int level,
                Symbol functionSymbol
        ) {
            this.parent = parent;
            this.level = level;
            this.functionSymbol = functionSymbol;

            this.variables = new HashMap<>();
            this.parameters = new HashMap<>();
            this.functions = new HashMap<>();
        }
    }
}