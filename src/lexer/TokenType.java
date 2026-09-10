package lexer;

public enum TokenType {
    // Delimiters & Markers
    COLON(":"), SEMICOLON(";"), LPAREN("("), PAREN(")"),
    LBRACE("{"), RBRACE("}"), ASSIGN("="), DOLLAR("$"),
    
    // Keywords
    VOID("void"), NUM_KEYWORD("num"), RETURN("return"),
    PRINT("print"), NOP("nop"), COMMENT("comment"),
    IF("if"), THEN("then"), ELSE("else"),
    DO("do"), WHILE("while"), UNTIL("until"),
    
    // Operators
    ADD("add"), SUB("sub"), MUL("mul"), DIV("div"), MOD("mod"), NEG("neg"),
    NOT("not"), AND("and"), OR("or"), EQ("eq"), LARGER("larger"), LESSER("lesser"),
    
    // Dynamic Terminals
    USER_DEFINED_NAME, NUM_LITERAL, STRING_LITERAL,
    
    // Sentinel
    EOF;

    private final String label;

    private TokenType(){
        this(null);
    }

    private TokenType(String label){
        this.label = label;
    }

    public String getLabel(){
        return this.label;
    }
}

// EOF != DOLLAR.
// Lexer reads a literal $ → emits Token(DOLLAR, "$", line, col).
// After that (or once the character stream is exhausted),
// lexer emits exactly one Token(EOF, null, line, col) as the final item handed to the parser.
// Parser matches DOLLAR as part of the SPL_PROG rule (→ leaf node in the tree) and
// separately checks for EOF afterward to confirm there's no trailing junk in the file.