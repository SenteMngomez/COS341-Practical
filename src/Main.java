import errors.CodeGeneratorException;
import errors.LexerException;
import errors.ParserException;
import errors.SemanticException;
import generator.CodeGenerator;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import semantic.ScopeAnalyzer;
import semantic.Symbol;
import semantic.SymbolTable;
import semantic.TypeAnalyzer;
import tree.Node;
import xml.XMLGenerator;

public class Main {

    private static final String OUTPUT_XML_PATH = "tree.xml";

    // Phase 3b: generated executable C++ with inlined function calls
    private static final String OUTPUT_CODE_PATH = "output.cpp";

    // Compatibility output path for the existing runnable option
    private static final String OUTPUT_RUNNABLE_PATH = "output_runnable.cpp";

    // Enabled by passing "--runnable" as the second command-line argument
    private static boolean writeRunnable = false;

    private static int exitCode = 0;

    public static void main(String[] args)
            throws InterruptedException {

        final String inputFilePath;

        writeRunnable = args.length > 1 && "--runnable".equals(args[1]);

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
        Thread worker = new Thread(null, () -> exitCode = compile(inputFilePath), "compiler", 512L * 1024 * 1024);
        worker.start();
        worker.join();
        System.exit(exitCode);
    }

    private static int compile(String inputFilePath) {

        try {
            Files.deleteIfExists(Paths.get(OUTPUT_XML_PATH));
            Files.deleteIfExists(Paths.get(OUTPUT_CODE_PATH));
            Files.deleteIfExists(Paths.get(OUTPUT_RUNNABLE_PATH));
            String sourceCode = Files.readString(Paths.get(inputFilePath), StandardCharsets.UTF_8);
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

            // Keep Phase 1 output available even when later semantic phases fail.
            System.out.println("\n--- Generating XML ---");
            XMLGenerator.generate(astRoot, OUTPUT_XML_PATH);
            System.out.println("[XML Success] Syntax tree written to " + OUTPUT_XML_PATH);

            // Phase 2a: Names and Scopes
            System.out.println("\n--- Phase 2a: Names and Scopes ---");

            ScopeAnalyzer scopeAnalyzer = new ScopeAnalyzer();
            SymbolTable symbolTable = scopeAnalyzer.analyze(astRoot);

            System.out.println("[Semantic Success] Names and scopes analysed.");

            System.out.println("--- Resolved Names ---");
            printResolvedNames(astRoot);
            System.out.println("----------------------");

            for (Symbol symbol : symbolTable.getSymbols()) {
                System.out.println(symbol.getOriginalName() + " -> " + symbol.getGeneratedName() + " | "
                        + (symbol.isFunction() ? "function" : "variable") + " | scope " + symbol.getScopeLevel());
            }

            // Phase 2b: Type Analysis
            System.out.println("\n--- Phase 2b: Type Analysis ---");

            TypeAnalyzer typeAnalyzer = new TypeAnalyzer();
            typeAnalyzer.analyze(astRoot, symbolTable);

            System.out.println("[Semantic Success] Type analysis passed.");

            // Phase 3a & 3b: Code Generation with function inlining
            System.out.println("\n--- Phase 3b: Code Generation ---");

            CodeGenerator codeGenerator = new CodeGenerator(symbolTable);
            String targetCode = codeGenerator.generate(astRoot);
            Files.writeString(Paths.get(OUTPUT_CODE_PATH), targetCode, StandardCharsets.UTF_8);
            System.out.println("[CodeGen Success] Algorithm code (" + codeGenerator.getType() + ") written to "
                    + OUTPUT_CODE_PATH);

            if (writeRunnable) {
                String runnable = codeGenerator.generateRunnable(astRoot);
                Files.writeString(Paths.get(OUTPUT_RUNNABLE_PATH), runnable, StandardCharsets.UTF_8);
                System.out.println("[CodeGen Success] Runnable test wrapper written to " + OUTPUT_RUNNABLE_PATH);
            }
            return 0;

        } catch (LexerException e) {
            System.err.println(
                    "\n[Lexer Error] " + e.getMessage() + " (Line: " + e.getLine() + ", Col: " + e.getCol() + ")");
            return 1;

        } catch (ParserException e) {
            System.err.println("\n[Parser Error] " + e.getMessage() + " (Line: " + e.getLine() + ", Col: "
                    + e.getCol() + ", Lexeme: '" + e.getLexeme() + "')");
            return 2;

        } catch (SemanticException e) {
            System.err.println("\n[Semantic Error] " + e.getMessage());
            return 6;

        } catch (CodeGeneratorException e) {
            System.err.println("\n[CodeGen Error] " + e.getMessage());
            return 7;

        } catch (java.io.IOException e) {
            System.err.println("\n[File Error] Cannot read '" + inputFilePath + "' or write '" + OUTPUT_XML_PATH + "': "
                    + e.getMessage());
            return 3;

        } catch (StackOverflowError e) {
            System.err.println("\n[System Error] The program is too large " + "or too deeply nested to process.");
            return 4;

        } catch (Exception e) {
            System.err.println("\n[System Error] " + e);
            return 5;
        }
    }

    private static void printResolvedNames(Node node) {
        if ("USER-DEFINED-NAME".equals(node.getName())) {
            System.out.println(
                    node.getContents() + " -> " + node.getGeneratedName());
        }

        for (Node child : node.getChildren()) {
            printResolvedNames(child);
        }
    }
}