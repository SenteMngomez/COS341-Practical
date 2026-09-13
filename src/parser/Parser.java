package parser;

import errors.ParserException;
import java.util.List;
import lexer.Token;
import lexer.TokenType;
import tree.Node;

public class Parser {
    private final List<Token> tokens;
    private int currentPosition;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.currentPosition = 0;
    }

    //Entry
    
    public Node parse() throws ParserException {
        Node root = parseSPLProg();
        if (!isAtEnd()) {
            throw new ParserException("Unexpected tokens found after End-Of-File marker.", peek().line);
        }
        return root;
    }

    //Recursive Descent Parsing Methods

    private Node parseSPLProg() throws ParserException {
        Node node = new Node("SPL_PROG");
        node.addChild(parseP());
        match(TokenType.EOF);
        return node;
    }

    private Node parseP() throws ParserException {
        Node node = new Node("P");
        node.addChild(parseVDecl());
        consume(TokenType.COLON, "Expected ':' delimiter in program structure.");
        node.addChild(parseFDecl());
        consume(TokenType.COLON, "Expected ':' delimiter in program structure.");
        node.addChild(parseAlgo());
        return node;
    }

    private Node parseVDecl() throws ParserException {
        Node node = new Node("V_DECL");
        if (check(TokenType.USER_DEFINED_NAME)) {
            node.addChild(new Node("USER-DEFINED-NAME", advance().lexeme));
            node.addChild(parseVDecl());
        }
        // Epsilon case: If it's a COLON, V_DECL is finished.
        return node; 
    }

    private Node parseFDecl() throws ParserException {
        Node node = new Node("F_DECL");
        if (check(TokenType.VOID) || check(TokenType.NUM_KEYWORD)) {
            node.addChild(parseFType());
            node.addChild(parseFDecl());
        }
        // Epsilon case
        return node;
    }

    private Node parseFType() throws ParserException {
        Node node = new Node("F_TYPE");
        if (match(TokenType.VOID)) {
            node.addChild(new Node("void"));
            Token name = consume(TokenType.USER_DEFINED_NAME, "Expected function name.");
            node.addChild(new Node("USER-DEFINED-NAME", name.lexeme));
            consume(TokenType.LPAREN, "Expected '(' for parameter list.");
            node.addChild(parseVDecl());
            consume(TokenType.RPAREN, "Expected ')' after parameter list.");
            consume(TokenType.LBRACE, "Expected '{' to start function body");
            node.addChild(parseP());
            consume(TokenType.RETURN, "Expected 'return' keyword in void function.");
            consume(TokenType.RBRACE, "Expected '}' to end function body");

        } else if (match(TokenType.NUM_KEYWORD)) {
            node.addChild(new Node("num"));
            Token name = consume(TokenType.USER_DEFINED_NAME, "Expected function name.");
            node.addChild(new Node("USER-DEFINED-NAME", name.lexeme));
            consume(TokenType.LPAREN, "Expected '(' for parameter list.");
            node.addChild(parseVDecl());
            consume(TokenType.RPAREN, "Expected ')' after parameter list.");
            consume(TokenType.LBRACE, "Expected '{' to start function body");
            node.addChild(parseP());
            consume(TokenType.RETURN, "Expected 'return' keyword in num function.");
            consume(TokenType.LPAREN, "Expected '(' around term.");
            node.addChild(parseTerm());
            consume(TokenType.RPAREN, "Expected ')' around term.");
            consume(TokenType.RBRACE, "Expected '}' to end function body");

        } else {
            throw error("Expected 'void' or 'num' for function declaration.");
        }

        return node;
    }

    private Node parseAlgo() throws ParserException {
        Node node = new Node("ALGO");
        //Lookahead to check if ALGO is ending (epsilon case).
        //Since ALGO is followed by } (in F_TYPE, LOOP, BRANCH) or EOF.
        if (check(TokenType.RBRACE) || check(TokenType.RETURN) ||check(TokenType.EOF)) {
            return node; // Epsilon
        }
        
        node.addChild(parseInstr());
        consume(TokenType.SEMICOLON, "Expected ';' after instruction.");
        node.addChild(parseAlgo());
        return node;
    }

    private Node parseInstr() throws ParserException {
        Node node = new Node("INSTR");
        if (match(TokenType.PRINT)) {
            node.addChild(new Node("print"));
            node.addChild(parseOutp());

        } else if (match(TokenType.NOP)) {
            node.addChild(new Node("nop"));

        } else if (match(TokenType.COMMENT)) {
            node.addChild(new Node("comment"));
            Token str = consume(TokenType.STRING_LITERAL, "Expected string after comment.");
            node.addChild(new Node("STRING", str.lexeme));

        } else if (check(TokenType.USER_DEFINED_NAME)) {
            Token name = advance(); // Consume the identifier
            node.addChild(new Node("USER-DEFINED-NAME", name.lexeme));
            node.addChild(parseInstrTail());

        } else if (check(TokenType.IF)) {
            node.addChild(parseBranch());

        } else if (check(TokenType.DO) || check(TokenType.WHILE) || check(TokenType.UNTIL)) {
            node.addChild(parseLoop());

        } else {
            throw error("Invalid instruction start.");
        }

        return node;
    }

    private Node parseInstrTail() throws ParserException {
        if (match(TokenType.ASSIGN)) {
            Node node = new Node("ASSIGN");
            node.addChild(parseTerm());

            return node;
        } else if (match(TokenType.LPAREN)) {
            Node node = new Node("CALL");
            node.addChild(parseInput());
            consume(TokenType.RPAREN, "Expected ')' to close function call.");
            return node;
        }
        throw error("Expected '=' for assignment or '(' for function call after identifier.");
    }

    private Node parseOutp() throws ParserException {
        Node node = new Node("OUTP");
        if (match(TokenType.LPAREN)) {
            node.addChild(parseTerm());
            consume(TokenType.RPAREN, "Expected ')' after print expression.");

        } else if (check(TokenType.STRING_LITERAL)) {
            Token str = advance();
            node.addChild(new Node("STRING", str.lexeme));

        } else {
            throw error("Expected '(' or string literal in print output.");
        }
        return node;
    }

    private Node parseTerm() throws ParserException {
        Node node = new Node("TERM");
        if (check(TokenType.USER_DEFINED_NAME)) {
            Token name = advance();
            node.addChild(new Node("USER-DEFINED-NAME", name.lexeme));
            node.addChild(parseTermRest());

        } else if (check(TokenType.NUM_LITERAL)) {
            Token num = advance();
            node.addChild(new Node("NUM", num.lexeme));

        } else if (match(TokenType.MOD)) {
            node.addChild(new Node("mod"));
            parseBinaryTermOp(node);

        } else if (match(TokenType.ADD)) {
            node.addChild(new Node("add"));
            parseBinaryTermOp(node);

        } else if (match(TokenType.SUB)) {
            node.addChild(new Node("sub"));
            parseBinaryTermOp(node);

        } else if (match(TokenType.MUL)) {
            node.addChild(new Node("mul"));
            parseBinaryTermOp(node);

        } else if (match(TokenType.DIV)) {
            node.addChild(new Node("div"));
            parseBinaryTermOp(node);

        } else if (match(TokenType.NEG)) {
            node.addChild(new Node("neg"));
            consume(TokenType.LPAREN, "Expected '(' after 'neg'.");
            node.addChild(parseTerm());
            consume(TokenType.RPAREN, "Expected ')' after neg expression.");

        } else {
            throw error("Invalid expression term.");
        }
        return node;
    }
    
    private void parseBinaryTermOp(Node parentNode) throws ParserException {
        consume(TokenType.LPAREN, "Expected '(' after binary operator.");
        parentNode.addChild(parseTerm());
        parentNode.addChild(parseTerm());
        consume(TokenType.RPAREN, "Expected ')' after binary operands.");
    }

    private Node parseTermRest() throws ParserException {
        if (match(TokenType.LPAREN)) {
            Node node = new Node("CALL");
            node.addChild(parseInput());
            match(TokenType.RPAREN);
            return node;
        }
        //Epsilon case for standard variable usage
        return new Node("TERM_REST_EMPTY"); 
    }

    private Node parseInput() throws ParserException {
        Node node = new Node("INPUT");
        //Lookahead to check if we have reached the closing bracket of the call
        if (check(TokenType.RPAREN)) {
            return node; // Epsilon
        }

        node.addChild(parseTerm());
        node.addChild(parseInput());
        return node;
    }

    private Node parseBool() throws ParserException {
        Node node = new Node("BOOL");
        if (match(TokenType.NOT)) {
            node.addChild(new Node("not"));
            consume(TokenType.LPAREN, "Expected '(' after 'not'.");
            node.addChild(parseBool());
            consume(TokenType.RPAREN, "Expected ')' after not expression.");

        } else if (match(TokenType.AND) || match(TokenType.OR)) {
            node.addChild(new Node(previous().lexeme));
            consume(TokenType.LPAREN, "Expected '(' after logical operator.");
            node.addChild(parseBool());
            node.addChild(parseBool());
            consume(TokenType.RPAREN, "Expected ')' after logical operands.");

        } else if (match(TokenType.EQ) || match(TokenType.LARGER) || match(TokenType.LESSER)) {
            node.addChild(new Node(previous().lexeme));
            consume(TokenType.LPAREN, "Expected '(' after comparison operator.");
            node.addChild(parseTerm());
            node.addChild(parseTerm());
            consume(TokenType.RPAREN, "Expected ')' after comparison operands.");

        } else {
            throw error("Invalid boolean expression.");
        }
        return node;
    }

    private Node parseBranch() throws ParserException {
        Node node = new Node("BRANCH");
        consume(TokenType.IF, "Expected 'if'.");
        node.addChild(parseBool());
        consume(TokenType.THEN, "Expected 'then' after branch condition.");
        consume(TokenType.LBRACE, "Expected '{' before then block.");
        node.addChild(parseAlgo());
        consume(TokenType.RBRACE, "Expected '}' after then block.");
        consume(TokenType.ELSE, "Expected 'else' after then block.");
        consume(TokenType.LBRACE, "Expected '{' before else block.");
        node.addChild(parseAlgo());
        consume(TokenType.RBRACE, "Expected '}' after else block.");
        return node;

    }

    private Node parseLoop() throws ParserException {
        Node node = new Node("LOOP");
        if (check(TokenType.WHILE) || check(TokenType.UNTIL)) {
            node.addChild(parseCond());
            node.addChild(parseBool());
            consume(TokenType.DO, "Expected 'do' after loop condition.");
            consume(TokenType.LBRACE, "Expected '{' to start loop body.");
            node.addChild(parseAlgo());
            consume(TokenType.RBRACE, "Expected '}' to end loop body.");

        } else if (match(TokenType.DO)) {
            node.addChild(new Node("do"));
            consume(TokenType.LBRACE, "Expected '{' after 'do'.");
            node.addChild(parseAlgo());
            consume(TokenType.RBRACE, "Expected '}' after do block.");
            node.addChild(parseCond());
            node.addChild(parseBool());
            
        } else {
            throw error("Invalid loop syntax.");
        }
        return node;
    }

    private Node parseCond() throws ParserException {
        Node node = new Node("COND");
        if (match(TokenType.WHILE)) {
            node.addChild(new Node("while"));
        } else if (match(TokenType.UNTIL)) {
            node.addChild(new Node("until"));
        } else {
            throw error("Expected 'while' or 'until'.");
        }
        return node;
    }

    //Helper Methods

    private boolean check(TokenType type) {
        if (currentPosition >= tokens.size()) return false;
        return peek().type == type;
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private Token consume(TokenType type, String errorMessage) throws ParserException {
        if (check(type)) return advance();
        throw error(errorMessage);
    }

    private Token advance() {
        if (!isAtEnd()) currentPosition++;
        return previous();
    }

    private Token peek() {
        return tokens.get(currentPosition);
    }

    private Token previous() {
        return tokens.get(currentPosition - 1);
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private ParserException error(String message) {
        return new ParserException(message + " Found: " + peek().type, peek().line);
    }
}