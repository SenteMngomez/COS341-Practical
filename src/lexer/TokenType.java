package lexer;

public enum TokenType {
    // Delimiters & Markers
    COLON(":"), SEMICOLON(";"), LPAREN("("), RPAREN(")"),
    LBRACE("{"), RBRACE("}"), ASSIGN("="),
    
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
    
    // Sentinel: appended by lexer at end-of-input
    EOF("$");

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