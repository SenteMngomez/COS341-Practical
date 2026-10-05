package semantic;

import java.util.ArrayList;
import java.util.List;

public class SymbolTable {

    private final List<Symbol> symbols;

    public SymbolTable() {
        symbols = new ArrayList<>();
    }

    public void add(Symbol symbol) {
        symbols.add(symbol);
    }

    public List<Symbol> getSymbols() {
        return symbols;
    }

    public Symbol find(
            String originalName,
            int scopeLevel,
            boolean function
    ) {
        for (int level = scopeLevel; level >= 0; level--) {

            for (Symbol symbol : symbols) {

                if (symbol.getOriginalName().equals(originalName)
                        && symbol.getScopeLevel() == level
                        && symbol.isFunction() == function) {

                    return symbol;
                }
            }
        }

        return null;
    }
}