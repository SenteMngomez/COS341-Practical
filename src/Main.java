import errors.LexerException;
import errors.ParserException;
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
    public static void main(String[] args) {

        String inputFilePath;

        if (args.length > 0) {
            inputFilePath = args[0];
        } else {
            
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter input file path: ");
            inputFilePath = scanner.nextLine();
        }

        String outputXmlPath = "tree.xml";

        try {
            //Read source code
            String sourceCode = Files.readString(Paths.get(inputFilePath));
            System.out.println("--- Reading " + inputFilePath + " ---");

            //Tokenize stream
            Lexer lexer = new Lexer(sourceCode);
            List<Token> tokens = lexer.tokenize();
            System.out.println("[Lexer Success] Generated " + tokens.size() + " tokens.");

            //Parse tokens into AST
            System.out.println("\n--- Parsing ---");
            Parser parser = new Parser(tokens);
            Node astRoot = parser.parse();
            System.out.println("[Parser Success] AST Root constructed: <" + astRoot.getContents() + ">");

            //Generate XML output via static call
            System.out.println("\n--- Generating XML ---");
            XMLGenerator.generate(astRoot, outputXmlPath);
            System.out.println("[XML Success] Syntax tree written to " + outputXmlPath);

        } catch (LexerException e) {
            System.err.println("\n[Lexer Error] " + e.getMessage());
        } catch (ParserException e) {
            System.err.println("\n[Parser Error] " + e.getMessage());
        } catch (Exception e) {
            System.err.println("\n[System Error] " + e.getMessage());
            e.printStackTrace();
        }
    }
}