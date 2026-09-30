package errors;

public class ParserException extends Exception {
    private final int line;
    private final int col;
    private final String lexeme;

    public ParserException(String message, int line) {
        this(message, line, 1, "");
    }

    public ParserException(String message, int line, int col, String lexeme) {
        super(message);
        this.line = line;
        this.col = col;
        this.lexeme = lexeme;
    }

    public int getLine() {
        return line;
    }

    public int getCol() {
        return col;
    }

    public String getLexeme() {
        return lexeme;
    }
}