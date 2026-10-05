package semantic;

import tree.Node;

public class Symbol {

    private final String originalName;
    private final String generatedName;
    private final boolean function;
    private final int scopeLevel;
    private final Node declarationNode;

    public Symbol(
            String originalName,
            String generatedName,
            boolean function,
            int scopeLevel,
            Node declarationNode
    ) {
        this.originalName = originalName;
        this.generatedName = generatedName;
        this.function = function;
        this.scopeLevel = scopeLevel;
        this.declarationNode = declarationNode;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getGeneratedName() {
        return generatedName;
    }

    public boolean isFunction() {
        return function;
    }

    public int getScopeLevel() {
        return scopeLevel;
    }

    public Node getDeclarationNode() {
        return declarationNode;
    }
}