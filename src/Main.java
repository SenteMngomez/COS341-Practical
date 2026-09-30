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

public class Main {

    private static final String OUTPUT_XML_PATH = "tree.xml";
    private static int exitCode = 0;

    public static void main(String[] args) throws InterruptedException {

        final String inputFilePath;

        if (args.length > 0) {
            inputFilePath = args[0];
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter input file path: ");
            inputFilePath = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        }

        // Big stack: the syntax tree is a deep chain for long programs and the XML writer is recursive.
        Thread worker = new Thread(null, () -> exitCode = compile(inputFilePath), "compiler", 512L * 1024 * 1024);
        worker.start();
        worker.join();
        System.exit(exitCode);
    }

    private static int compile(String inputFilePath) {
        try {
            Files.deleteIfExists(Paths.get(OUTPUT_XML_PATH));
            
            String sourceCode = Files.readString(Paths.get(inputFilePath), StandardCharsets.UTF_8);
            System.out.println("--- Reading " + inputFilePath + " ---");

            // Tokenize stream
            Lexer lexer = new Lexer(sourceCode);
            List<Token> tokens = lexer.tokenize();
            System.out.println("[Lexer Success] Generated " + tokens.size() + " tokens.");

            // Parse tokens into syntax tree
            System.out.println("\n--- Parsing ---");
            Parser parser = new Parser(tokens);
            Node astRoot = parser.parse();
            System.out.println("[Parser Success] Syntax tree root constructed: <" + astRoot.getContents() + ">");

            // Generate XML output
            System.out.println("\n--- Generating XML ---");
            XMLGenerator.generate(astRoot, OUTPUT_XML_PATH);
            System.out.println("[XML Success] Syntax tree written to " + OUTPUT_XML_PATH);
            return 0;

        } catch (LexerException e) {
            System.err.println("\n[Lexer Error] " + e.getMessage() + " (Line: " + e.getLine() + ", Col: " + e.getCol() + ")");
            return 1;
        } catch (ParserException e) {
            System.err.println("\n[Parser Error] " + e.getMessage());
            return 2;
        } catch (java.io.IOException e) {
            System.err.println("\n[File Error] Cannot read '" + inputFilePath + "' or write '" + OUTPUT_XML_PATH + "': " + e.getMessage());
            return 3;
        } catch (StackOverflowError e) {
            System.err.println("\n[System Error] The program is too large or too deeply nested to process.");
            return 4;
        } catch (Exception e) {
            System.err.println("\n[System Error] " + e);
            return 5;
        }
    }
}