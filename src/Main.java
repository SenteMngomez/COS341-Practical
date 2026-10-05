import errors.LexerException;
import errors.ParserException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import lexer.Lexer;
import lexer.Token;

import parser.Parser;

import tree.Node;

import xml.XMLGenerator;

import semantic.ScopeAnalyzer;
import semantic.Symbol;
import semantic.SymbolTable;
import errors.SemanticException;

public class Main {

    private static final String OUTPUT_XML_PATH = "tree.xml";

    private static int exitCode = 0;

    public static void main(String[] args)
            throws InterruptedException {

        final String inputFilePath;

        if (args.length > 0) {

            inputFilePath = args[0];

        } else {

            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter input file path: ");

            inputFilePath = scanner.hasNextLine()
                    ? scanner.nextLine().trim()
                    : "";

            scanner.close();
        }

        /*
         * Use a larger stack because the syntax tree and XML generator
         * can become deeply nested for large SPL programs.
         */
        Thread worker = new Thread(null, () -> exitCode = compile(inputFilePath),"compiler",512L * 1024 * 1024);
        worker.start();
        worker.join();
        System.exit(exitCode);
    }

    private static int compile(String inputFilePath) {

        try {
            Files.deleteIfExists(Paths.get(OUTPUT_XML_PATH));
            String sourceCode = Files.readString(Paths.get(inputFilePath),StandardCharsets.UTF_8);
            System.out.println("--- Reading " + inputFilePath + " ---");

            // Tokenize
            Lexer lexer = new Lexer(sourceCode);
            List<Token> tokens = lexer.tokenize();
            System.out.println("[Lexer Success] Generated " + tokens.size() + " tokens.");

            // Parse
            System.out.println("\n--- Parsing ---");

            /*
             * IDs must start at 1 for every new syntax tree.
             */
            Node.resetIds();
            Parser parser = new Parser(tokens);
            Node astRoot = parser.parse();

            System.out.println("[Parser Success] Syntax tree root constructed: <" + astRoot.getContents() + ">");

            // Phase 2a: Names and Scopes
            System.out.println("\n--- Phase 2a: Names and Scopes ---");

            ScopeAnalyzer scopeAnalyzer = new ScopeAnalyzer();
            SymbolTable symbolTable = scopeAnalyzer.analyze(astRoot);

            System.out.println("[Semantic Success] Names and scopes analysed.");

            for (Symbol symbol : symbolTable.getSymbols()) {
                System.out.println(symbol.getOriginalName() + " -> "
                                + symbol.getGeneratedName()+ " | " + (symbol.isFunction() ? "function" : "variable")
                                + " | scope " + symbol.getScopeLevel());
            }

            // Generate XML
            System.out.println("\n--- Generating XML ---");
            XMLGenerator.generate( astRoot, OUTPUT_XML_PATH);
            System.out.println("[XML Success] Syntax tree written to " + OUTPUT_XML_PATH);
            return 0;

        } catch (LexerException e) {
            System.err.println("\n[Lexer Error] " + e.getMessage() + " (Line: "+ e.getLine() + ", Col: " + e.getCol() + ")");
            return 1;

        } catch (ParserException e) {
            System.err.println("\n[Parser Error] " + e.getMessage());
            return 2;

        } catch (SemanticException e) {
            System.err.println("\n[Semantic Error] " + e.getMessage());
            return 6;
        } catch (java.io.IOException e) {
            System.err.println("\n[File Error] Cannot read '" + inputFilePath + "' or write '" + OUTPUT_XML_PATH + "': " + e.getMessage());
            return 3;

        } catch (StackOverflowError e) {
            System.err.println("\n[System Error] The program is too large " + "or too deeply nested to process.");
            return 4;

        } catch (Exception e) {
            System.err.println("\n[System Error] " + e );
            return 5;
        } 
    }
}