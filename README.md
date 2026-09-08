# COS341 SPL Compiler Project

## 1. Project Overview

This project is a compiler for the **Students' Programming Language (SPL)**.

The current specification released by the course focuses on the **lexical analysis and syntax analysis phases**. However, the final project is expected to grow into a complete compiler pipeline, including later phases such as:

* Lexical analysis
* Syntax analysis
* Syntax tree construction
* XML output
* Semantic analysis
* Type checking
* Intermediate representation / intermediate code generation
* Potential later compiler phases

The goal is therefore **not** to build a lexer and parser as two isolated programs. We are building a modular compiler that can be extended as additional specifications are released.

### Overall pipeline

```text
SPL.txt
   │
   ▼
┌─────────────┐
│    Lexer    │
│ Characters  │
│     ↓       │
│   Tokens    │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Parser    │
│   Tokens    │
│      ↓      │
│ Syntax Tree │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ XML Builder │
│      ↓      │
│  tree.xml   │
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│ Future Compiler     │
│ Phases              │
│                     │
│ Semantic Analysis   │
│ Type Checking       │
│ IR Generation       │
│ etc.                │
└─────────────────────┘
```

---

# 2. Current Specification

The current SPL specification defines:

1. Lexical categories
2. The SPL context-free grammar
3. Requirements for syntax analysis
4. Syntax error handling
5. Syntax tree XML output

Every token in an SPL program must be followed by a `blank_space`, where a blank space is either:

* ASCII space (`32`)
* ASCII carriage return (`13`)

The specification defines lexical categories such as:

* `NUM`
* `USER-DEFINED-NAME`
* `STRING`

The parser must determine whether an input program conforms to the SPL grammar.

If the program is invalid, the compiler must produce a meaningful **Syntax Error Message** with helpful hints.

If the program is valid, the compiler must generate:

```text
tree.xml
```

The XML represents the syntax tree and uses unique node IDs so that the IDs can later be used as references by future compiler phases.

---

# 3. Development Language

## Java

The project will be implemented in **Java**.

### Why Java?

Java is suitable because:

* The project naturally benefits from object-oriented design.
* Lexer, parser, token, tree and XML components can be represented as separate classes.
* Java provides strong support for exceptions and error handling.
* XML generation can be implemented cleanly.
* JUnit can be used for automated testing.
* Java makes it easier to extend the project later with semantic analysis and intermediate code generation.
* Packages allow each team member's area of responsibility to remain organised.

---

# 4. Architecture

The project follows a modular compiler architecture.

```text
src/
│
├── lexer/
│   ├── Lexer.java
│   ├── Token.java
│   └── TokenType.java
│
├── parser/
│   ├── Parser.java
│   └── ...
│
├── tree/
│   ├── Node.java
│   └── ...
│
├── xml/
│   ├── XMLGenerator.java
│   └── ...
│
├── errors/
│   ├── LexerException.java
│   ├── ParserException.java
│   └── ...
│
├── semantic/
│   └── ...              # Future phase
│
├── ir/
│   └── ...              # Future phase
│
└── Main.java
```

The important design principle is:

> **Each compiler phase should have a clear responsibility and communicate with the next phase through well-defined data structures.**

---

# 5. Team Responsibilities

There are **three team members**.

The division is based on compiler components, **not on splitting the practical into separate assignment submissions**.

Both the lexer and parser are part of the compiler frontend, but they have different responsibilities.

---

## Person 1 — Lexer / Lexical Analysis

### Main responsibility

Convert the raw SPL source code into a stream of tokens.

```text
SPL.txt
   ↓
Lexer
   ↓
Token stream
```

### Responsibilities

* Read the input file.
* Process characters.
* Identify lexical units.
* Recognise:

  * Numbers
  * User-defined names
  * Strings
  * Keywords
  * Operators/symbols
  * Other terminals defined by the SPL specification
* Enforce lexical rules.
* Detect invalid tokens.
* Produce useful lexical error messages.
* Create the shared `Token` representation.
* Create the `TokenType` enum.
* Provide the token stream to the parser.

### Deliverable

A working lexer that can transform:

```text
SPL source code
```

into something conceptually like:

```text
Token(TokenType.NUM, "123")
Token(TokenType.USER_DEFINED_NAME, "x")
Token(TokenType.ASSIGN, "=")
...
```

The exact token representation will be decided during implementation.

---

# 6. Person 2 — Parser / Syntax Analysis

### Main responsibility

Implement the SPL grammar and determine whether the token stream forms a valid SPL program.

```text
Token stream
      ↓
    Parser
      ↓
Valid / Invalid
```

### Responsibilities

* Analyse the official SPL grammar.
* Determine whether the grammar is suitable for LL(1) parsing.
* Calculate/check FIRST and FOLLOW sets where necessary.
* Apply left-factoring where required.
* Implement the parser.
* Consume tokens produced by the lexer.
* Detect syntax errors.
* Produce meaningful syntax error messages.
* Coordinate with Person 3 on syntax-tree construction.

### Parser approach

The intended implementation is a **recursive-descent parser** based on the LL(1) grammar.

Conceptually:

```text
parseSPL_PROG()
    ↓
parseP()
    ↓
parseV_DECL()
parseF_DECL()
parseALGO()
```

Each grammar non-terminal can correspond to a parser method where appropriate.

---

# 7. LL(1) Grammar

The original grammar contains productions with common prefixes that would cause problems for a straightforward LL(1) recursive-descent parser.

For example:

```text
INSTR → ASSIGN | CALL
```

Both alternatives begin with:

```text
USER-DEFINED-NAME
```

Similarly:

```text
TERM → USER-DEFINED-NAME | CALL
```

Both alternatives again begin with:

```text
USER-DEFINED-NAME
```

Therefore, the grammar must be left-factored.

---

## Left-factored INSTR

Instead of:

```text
INSTR → ASSIGN | CALL | BRANCH | LOOP | ...
```

we use:

```text
INSTR → USER-DEFINED-NAME INSTR_TAIL
      | BRANCH
      | LOOP
      | ...
```

with:

```text
INSTR_TAIL → = TERM
           | ( INPUT )
```

This allows the parser to inspect the next token after the user-defined name and determine whether it is an assignment or function call.

### Example

```text
x = 5
```

becomes conceptually:

```text
USER-DEFINED-NAME
        ↓
      INSTR_TAIL
        ↓
       = TERM
```

Whereas:

```text
foo ( x )
```

becomes:

```text
USER-DEFINED-NAME
        ↓
      INSTR_TAIL
        ↓
     ( INPUT )
```

---

# 8. Left-factored TERM

The original grammar contains:

```text
TERM → USER-DEFINED-NAME
     | CALL
     | NUM
     | ...
```

Since a function call also starts with a user-defined name, these alternatives need to be factored.

The grammar can instead use:

```text
TERM → USER-DEFINED-NAME TERM_REST
     | NUM
     | mod ( TERM TERM )
     | add ( TERM TERM )
     | sub ( TERM TERM )
     | mul ( TERM TERM )
     | div ( TERM TERM )
     | neg ( TERM )
```

with:

```text
TERM_REST → ε
          | ( INPUT )
```

This means:

```text
x
```

can be parsed as a normal term, while:

```text
foo ( x )
```

can be parsed as a function call.

---

# 9. Current LL(1) Grammar

The team's current grammar is based on the official SPL grammar with the necessary left-factoring applied.

The important sections are:

```text
SPL_PROG → P $

P → V_DECL : F_DECL : ALGO

V_DECL → ε
       | USER-DEFINED-NAME V_DECL

F_DECL → ε
       | F_TYPE F_DECL

F_TYPE → void USER-DEFINED-NAME ( V_DECL ) { P return }

F_TYPE → num USER-DEFINED-NAME ( V_DECL )
         { P return ( TERM ) }

ALGO → ε
     | INSTR ; ALGO
```

Instructions:

```text
INSTR → print OUTP
      | nop
      | comment STRING
      | USER-DEFINED-NAME INSTR_TAIL
      | BRANCH
      | LOOP
```

Instruction tail:

```text
INSTR_TAIL → = TERM
           | ( INPUT )
```

Output:

```text
OUTP → ( TERM )
     | STRING
```

Terms:

```text
TERM → USER-DEFINED-NAME TERM_REST
     | NUM
     | mod ( TERM TERM )
     | add ( TERM TERM )
     | sub ( TERM TERM )
     | mul ( TERM TERM )
     | div ( TERM TERM )
     | neg ( TERM )
```

Term rest:

```text
TERM_REST → ε
          | ( INPUT )
```

Input:

```text
INPUT → ε
      | TERM INPUT
```

Boolean expressions:

```text
BOOL → not ( BOOL )
     | and ( BOOL BOOL )
     | or ( BOOL BOOL )
     | eq ( TERM TERM )
     | larger ( TERM TERM )
     | lesser ( TERM TERM )
```

Branch:

```text
BRANCH → if BOOL then { ALGO } else { ALGO }
```

Loops:

```text
LOOP → COND BOOL do { ALGO }
     | do { ALGO } COND BOOL
```

Conditions:

```text
COND → while
     | until
```

### Important

The `var` keyword has been **removed** from the proposed grammar.

Therefore:

```text
V_DECL → ε
       | USER-DEFINED-NAME V_DECL
```

matches the original specification's structure rather than introducing an additional `var` terminal.

---

# 10. Syntax Tree

The parser must construct a syntax tree for valid programs.

Each node requires a unique ID.

There are three important types of nodes:

### Root node

The root represents the start symbol:

```text
SPL_PROG
```

It contains:

* Unique ID
* `contents`
* IDs of its immediate children

### Inner node

Represents a non-terminal such as:

```text
P
ALGO
TERM
BOOL
INSTR
```

It contains:

* Unique ID
* `contents`
* Child IDs
* Parent ID

### Leaf node

Represents a terminal token that has been consumed.

For example:

```text
print
123
x
(
)
```

It contains:

* Unique ID
* `contents`
* Parent ID

---

# 11. Generic Node Design

A generic node structure should be used instead of creating a separate Java class for every grammar production.

Conceptually:

```text
Node
├── id
├── contents
├── parent
└── children
```

For example:

```text
Node {
    id: 15
    contents: "TERM"
    parent: 7
    children: [16, 17]
}
```

This allows the tree structure to remain flexible if the grammar changes.

It also makes it easier for later compiler phases to work with the tree.

---

# 12. XML Generation

The syntax tree must eventually be written to:

```text
tree.xml
```

The XML generator should be separate from the parser.

Architecture:

```text
Parser
   ↓
Syntax Tree
   ↓
XMLGenerator
   ↓
tree.xml
```

This separation means:

* The parser focuses on syntax.
* The tree classes focus on representing the tree.
* The XML generator focuses on serialisation.

This also makes debugging easier.

---

# 13. Error Handling

The compiler must provide useful errors instead of simply crashing.

There are two main categories at this stage.

### Lexer errors

Examples:

```text
Invalid character
Invalid number
Unterminated string
Invalid user-defined name
Missing required blank space
```

### Parser errors

Examples:

```text
Unexpected token
Expected ')'
Expected ';'
Expected 'then'
Expected 'else'
Expected expression
Unexpected end of file
```

Errors should ideally include:

* What went wrong
* Where it happened
* What was expected
* A useful hint for fixing it

For example:

```text
Syntax Error:
Expected ')' after function arguments.

Line: 4
Token: }

Hint:
Check that every '(' has a matching ')'.
```

---

# 14. Testing Strategy

Testing will happen throughout development rather than only at the end.

## Lexer tests

Test:

* Valid names
* Valid numbers
* Valid strings
* Keywords
* Symbols
* Whitespace
* Invalid characters
* Invalid lexical structures

---

## Parser tests

Test:

* Minimal valid programs
* Variable declarations
* Function declarations
* Assignments
* Function calls
* Arithmetic expressions
* Boolean expressions
* Branches
* Loops
* Nested constructs
* Invalid syntax

---

## Syntax tree tests

Verify:

* Every node has a unique ID.
* Parent IDs are correct.
* Child IDs are correct.
* The root is correct.
* Terminals appear as leaf nodes.
* Non-terminals appear as inner nodes.

---

## XML tests

Verify:

* `tree.xml` is generated.
* XML is well-formed.
* IDs are unique.
* Parent/child references are valid.
* The output can be rendered by a browser.
* The structure follows the specification.

---

# 15. Integration Testing

The entire pipeline must eventually be tested together:

```text
SPL.txt
   ↓
Lexer
   ↓
Tokens
   ↓
Parser
   ↓
Syntax Tree
   ↓
XML Generator
   ↓
tree.xml
```

A successful integration test should demonstrate that a valid SPL program can pass through the complete pipeline without manual intervention.

Invalid programs should stop at the appropriate stage and produce a useful error.

---

# 16. Assignment Strategy

There are **two deadlines**, but we are **not splitting the project into two separate pieces**.

The strategy is:

> **Complete the entire currently specified practical before the first deadline, then use the period before the final deadline to fix, improve and extend the project.**

This means the first deadline is treated as a **complete-project checkpoint**, not:

```text
Assignment 1 = Lexer
Assignment 2 = Parser
```

Instead:

```text
                FIRST DEADLINE
                     │
                     ▼
       ┌─────────────────────────┐
       │ Entire current project  │
       │ should be working       │
       │                         │
       │ Lexer                   │
       │ Parser                  │
       │ Syntax Tree             │
       │ XML                     │
       │ Error Handling          │
       │ Testing                 │
       └────────────┬────────────┘
                    │
                    ▼
              Marking / Feedback
                    │
                    ▼
       ┌─────────────────────────┐
       │ Improvements             │
       │                         │
       │ Bug fixes               │
       │ Feedback fixes          │
       │ Better error handling   │
       │ More testing            │
       │ Integration             │
       │ Documentation           │
       │ Future compiler phases  │
       └────────────┬────────────┘
                    │
                    ▼
             FINAL DEADLINE
```

---

# 17. Development Timeline

## Phase 1 — Architecture

* Agree on Java.
* Agree on project structure.
* Agree on token representation.
* Agree on parser/tree interfaces.
* Agree on Git workflow.
* Confirm grammar.

---

## Phase 2 — Lexer

Person 1 leads:

* Lexer implementation
* Token class
* Token types
* Lexical errors
* Lexer tests

Other team members integrate against the lexer interface.

---

## Phase 3 — Grammar / Parser

Person 2 leads:

* Grammar analysis
* FIRST/FOLLOW verification
* LL(1) verification
* Left-factoring
* Recursive-descent parser
* Syntax errors
* Parser tests

---

## Phase 4 — Syntax Tree / XML

Person 3 leads:

* Node class
* Parent/child relationships
* Unique IDs
* Tree construction
* XML generation
* XML validation

---

## Phase 5 — Integration

All team members:

```text
Lexer → Parser → Tree → XML
```

must work together.

Test using complete SPL programs.

---

## Phase 6 — First Deadline

The target is:

> **The entire currently specified practical is working.**

The project should be able to:

1. Read `SPL.txt`
2. Lex the source
3. Produce tokens
4. Parse the tokens
5. Detect syntax errors
6. Build the syntax tree
7. Generate `tree.xml`

---

## Phase 7 — Post-Marking Improvements

After receiving marking/feedback:

* Fix discovered bugs.
* Correct grammar issues.
* Improve error messages.
* Improve XML/tree generation.
* Add missing tests.
* Improve integration.
* Clean up code.
* Improve documentation.

At this point, additional compiler specifications may also have been released.

---

## Phase 8 — Future Compiler Phases

As later specifications become available, extend the existing architecture with components such as:

```text
Syntax Tree
     ↓
Semantic Analysis
     ↓
Type Checking
     ↓
Intermediate Representation
     ↓
Intermediate Code Generation
     ↓
...
```

The existing lexer/parser/tree architecture should therefore **not** be designed as a disposable solution.

---

# 18. Architecture Decisions

| Decision      | Choice                        | Reason                                    |
| ------------- | ----------------------------- | ----------------------------------------- |
| Language      | Java                          | OOP, testing, XML, extensibility          |
| Lexer         | Separate component            | Clean lexical/syntax separation           |
| Parser        | Recursive descent             | Natural fit for LL(1) grammar             |
| Grammar       | LL(1) / left-factored         | Allows predictable parser decisions       |
| Tokens        | `Token` class                 | Shared lexer/parser interface             |
| Token types   | `TokenType` enum              | Prevents string-based token handling      |
| Syntax tree   | Generic `Node`                | Flexible and extensible                   |
| IDs           | Unique per node               | Required for XML and future references    |
| XML           | Separate generator            | Keeps parser independent of output format |
| Errors        | Centralised error handling    | Consistent user feedback                  |
| Testing       | Automated + integration tests | Catch regressions early                   |
| Future phases | Separate packages             | Allows compiler to grow cleanly           |

---

# 19. Git / Collaboration Rules

Each person should work on their own feature branch.

Example:

```text
main
│
├── feature/lexer
├── feature/parser
└── feature/syntax-tree
```

Avoid directly committing unfinished work to `main`.

Before merging:

1. Pull the latest changes.
2. Resolve conflicts.
3. Run tests.
4. Verify the complete pipeline.
5. Review the changes.
6. Merge.

### Important

The interfaces between components should be agreed upon early.

For example:

```text
Lexer
   ↓
List<Token>
   ↓
Parser
   ↓
Node
   ↓
XMLGenerator
```

This allows team members to work independently without constantly changing each other's code.

---

# 20. Definition of Done

The current practical is considered complete when:

### Lexer

* [ ] Reads SPL source.
* [ ] Correctly recognises all specified lexical categories.
* [ ] Produces tokens.
* [ ] Detects lexical errors.

### Parser

* [ ] Implements the SPL grammar.
* [ ] Uses the verified LL(1) structure.
* [ ] Correctly accepts valid programs.
* [ ] Rejects invalid programs.
* [ ] Produces useful syntax errors.

### Syntax Tree

* [ ] Tree is created for valid programs.
* [ ] Root node is correct.
* [ ] All nodes have unique IDs.
* [ ] Parent/child relationships are correct.
* [ ] Terminals are represented correctly.

### XML

* [ ] `tree.xml` is generated.
* [ ] XML is well-formed.
* [ ] Node IDs are unique.
* [ ] Parent/child references are correct.
* [ ] XML can be rendered in a browser.

### Integration

* [ ] Lexer connects to parser.
* [ ] Parser connects to tree.
* [ ] Tree connects to XML generator.
* [ ] Complete test programs work.

### Documentation

* [ ] Architecture documented.
* [ ] Grammar documented.
* [ ] Team responsibilities documented.
* [ ] Setup instructions documented.
* [ ] Testing documented.
* [ ] Future compiler phases accounted for.

---

# 21. Overall Project Philosophy

The project should be treated as **one compiler developed incrementally**.

We are not building:

```text
Person 1 → Lexer project
Person 2 → Parser project
Person 3 → XML project
```

We are building:

```text
                 SPL COMPILER
                      │
          ┌───────────┴───────────┐
          │                       │
        FRONTEND             FUTURE PHASES
          │                       │
     ┌────┴────┐              Semantic
     │         │              Analysis
   Lexer     Parser               ↓
     │         │              Type Checking
     └────┬────┘                   ↓
          │                       IR
      Syntax Tree                  ↓
          │                 Code Generation
          ▼
       tree.xml
```

The team responsibilities are simply **ownership areas within the same system**.

The first deadline should therefore aim for a complete working implementation of everything currently specified.

The final deadline is then used to deliver the **fully refined and extended compiler**, including corrections from marking and any later compiler-phase specifications.
