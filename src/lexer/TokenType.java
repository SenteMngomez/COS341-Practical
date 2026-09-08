package lexer;

public enum TokenType {
    // Delimiters & Markers
    COLON, SEMICOLON, LPAREN, RPAREN, LBRACE, RBRACE, ASSIGN, EOF,
    
    // Keywords
    VOID, NUM_KEYWORD, RETURN, PRINT, NOP, COMMENT,
    IF, THEN, ELSE, DO, WHILE, UNTIL,
    
    // Operators
    ADD, SUB, MUL, DIV, MOD, NEG,
    NOT, AND, OR, EQ, LARGER, LESSER,
    
    // Dynamic Terminals
    USER_DEFINED_NAME, NUM_LITERAL, STRING_LITERAL
}