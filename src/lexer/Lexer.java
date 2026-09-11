package lexer;

import java.util.*;
import java.util.regex.Pattern;

import errors.LexerException;

public class Lexer {
    private static final Map<String, TokenType> FIXED_LEXEMES = new HashMap<>();
    static{
        for(TokenType type: TokenType.values()){
            if(type != TokenType.EOF && type.getLabel() != null){
                FIXED_LEXEMES.put(type.getLabel(), type);
            }
        }
    }

    // Dynamic category patterns
    // NUM: 0 | (–)?0.(0-9)*(1-9) | (–)?(1-9)(0-9)*.(0-9)*(1-9) | (–)?(1-9)(0-9)*
    private static final Pattern NUM_PATTERN = Pattern.compile(
        "^0$" +
        "|^-?0\\.[0-9]*[1-9]$" +
        "|^-?[1-9][0-9]*\\.[0-9]*[1-9]$" +
        "|^-?[1-9][0-9]*$"
    );

    // USER-DEFINED-NAME: #(0-9|a-z)*
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "^#[a-z0-9]*$"
    );


    // STRING: "(,|.|:|–|?|!|0-9|a-z)*"
    private static final Pattern STRING_PATTERN = Pattern.compile(
        "^\"[,.:!?\\-0-9a-z]*\"$"
    );
    
    private final String source;

    public Lexer(String source){
        this.source = source;
    }
    
    public List<Token> tokenize() throws LexerException {
        List<Token> tokens = new ArrayList<>();

        int n = source.length();
        int i = 0;
        int line = 1;
        int col = 1;

        StringBuilder buffer = new StringBuilder();
        int tokenStartLine = 1;
        int tokenStartCol = 1;

        while(i < n){
            char c = source.charAt(i);

            if(buffer.length() == 0){
                tokenStartLine = line;
                tokenStartCol = col;
            }

            if(c == ' ' || c == '\n' || c == '\r'){
                if(buffer.length() > 0){
                    emit(buffer.toString(), tokenStartLine, tokenStartCol, tokens);
                    buffer.setLength(0);
                }
            }else if(c == '$'){
                throw new LexerException("Unexpected '$' in the source. '$' is reserved as the end-of-file marker and may not appear in the input file", line, col);
            }else{
                buffer.append(c);
            }

            boolean isNewline = (c == '\n' || (c == '\r' && !(i + 1 < n && source.charAt(i + 1) == '\n'))); 

            if(isNewline){
                line++;
                col = 1;
            }else{
                col++;
            }
            i++;
        }

        // We have reached the end of the input stream
        if(buffer.length() > 0){
            emit(buffer.toString(), tokenStartLine, tokenStartCol, tokens);
        }

        tokens.add(new Token(TokenType.EOF, "$", line, col));

        return tokens;
    }

    private void emit(String chunk, int startLine, int startCol, List<Token> tokens) throws LexerException{
        TokenType type = classify(chunk);
        if(type == null){
            throw new LexerException("Invalid token: '" + chunk + "'", startLine, startCol);
        }
        tokens.add(new Token(type, chunk, startLine, startCol));
    }

    private TokenType classify(String chunk){
        TokenType fixed = FIXED_LEXEMES.get(chunk);
        if(fixed != null){
            return fixed;
        }
        if(NUM_PATTERN.matcher(chunk).matches()){
            return TokenType.NUM_LITERAL;
        }
        if(NAME_PATTERN.matcher(chunk).matches()){
            return TokenType.USER_DEFINED_NAME;
        }

        if(STRING_PATTERN.matcher(chunk).matches()){
            return TokenType.STRING_LITERAL;
        }

        return null;
    }
}
