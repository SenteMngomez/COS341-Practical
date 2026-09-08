# COS341 SPL Compiler Project

## 1. Project Overview

This project is a compiler for the **Students' Programming Language (SPL)**.

The current specification focuses on the **lexical analysis and syntax analysis phases**, including:

* Lexical analysis
* Syntax analysis
* Syntax tree construction
* XML output
* Syntax error handling

The final project is expected to grow beyond these phases as additional specifications are released. Future phases may include:

* Semantic analysis
* Type checking
* Intermediate representation
* Intermediate code generation
* Other compiler phases

Therefore, the project will be designed as **one modular compiler**, rather than separate programs for each assignment or team member.

### Overall pipeline

```text
                         SPL.txt
                            │
                            ▼
                     ┌─────────────┐
                     │    LEXER    │
                     │             │
                     │ Characters  │
                     │      ↓      │
                     │   Tokens    │
                     └──────┬──────┘
                            │
                            ▼
                     ┌─────────────┐
                     │   PARSER    │
                     │             │
                     │   Tokens    │
                     │      ↓      │
                     │ Syntax Tree │
                     └──────┬──────┘
                            │
                            ▼
                     ┌─────────────┐
                     │ XML BUILDER │
                     │             │
                     │      ↓      │
                     │  tree.xml   │
                     └──────┬──────┘
                            │
                            ▼
                  ┌─────────────────────┐
                  │   FUTURE PHASES     │
                  │                     │
                  │ Semantic Analysis   │
                  │ Type Checking       │
                  │ IR Generation       │
                  │ Code Generation     │
                  └─────────────────────┘
```

---

# 2. Team Structure

There are **three people working on the project**.

The project is divided according to **compiler components**, not into three separate projects.

| Team Member  | Main Responsibility      | Secondary Responsibility       |
| ------------ | ------------------------ | ------------------------------ |
| **Person 1** | Lexer / Lexical Analysis | Token system & lexical testing |
| **Person 2** | Parser / Syntax Analysis | Grammar, LL(1), parser errors  |
| **Person 3** | Syntax Tree / XML        | Integration, tree/XML testing  |

### Important

The three people are working on **one compiler**.

The division of responsibilities is primarily for ownership and development efficiency. Everyone is still responsible for:

* Integration
* Testing
* Debugging
* Code reviews
* Documentation
* Helping with components when required

The project should therefore work as:

```text
              ONE COMPILER
                   │
        ┌──────────┼──────────┐
        │          │          │
      Person 1   Person 2   Person 3
        │          │          │
      Lexer      Parser    Tree/XML
        │          │          │
        └──────────┼──────────┘
                   │
             Integration
                   │
              Complete
               System
```

---

# 3. Person 1 — Lexer / Lexical Analysis

## Main responsibility

Person 1 owns the **lexical analysis phase**.

The lexer converts the raw SPL source code into tokens.

```text
SPL.txt
   ↓
Lexer
   ↓
Token stream
```

### Responsibilities

* Read the SPL source file.
* Process the source character-by-character.
* Recognise all lexical categories defined by the specification.
* Recognise:

  * `NUM`
  * `USER-DEFINED-NAME`
  * `STRING`
  * Keywords
  * Symbols
  * Other terminals required by the grammar
* Enforce whitespace/blank-space rules.
* Detect invalid lexical structures.
* Create the `Token` class.
* Create the `TokenType` enum.
* Produce the token stream consumed by the parser.
* Write lexer tests.

### Main classes

```text
lexer/
├── Lexer.java
├── Token.java
└── TokenType.java
```

### Example

Input:

```text
x = 10
```

The lexer should conceptually produce:

```text
USER_DEFINED_NAME("x")
ASSIGN("=")
NUM("10")
```

The exact implementation will be determined during development.

---

# 4. Person 2 — Parser / Syntax Analysis

## Main responsibility

Person 2 owns the **syntax analysis phase**.

The parser receives tokens from the lexer and determines whether they form a valid SPL program according to the grammar.

```text
Token stream
     ↓
  Parser
     ↓
Valid / Invalid
```

### Responsibilities

* Analyse the official SPL grammar.
* Verify the grammar's LL(1) suitability.
* Calculate/check FIRST and FOLLOW sets.
* Apply left-factoring where required.
* Implement the parser.
* Consume tokens from the lexer.
* Recognise grammar productions.
* Detect syntax errors.
* Produce meaningful syntax error messages.
* Work with Person 3 to construct the syntax tree.
* Write parser tests.

### Main classes

```text
parser/
├── Parser.java
└── ...
```

### Parser approach

The intended approach is a **recursive-descent parser** based on the left-factored LL(1) grammar.

Conceptually:

```text
parseSPL_PROG()
      ↓
   parseP()
      ↓
 ┌────┼────────┐
 ↓    ↓        ↓
V_DECL F_DECL ALGO
```

---

# 5. Person 3 — Syntax Tree / XML / Integration

## Main responsibility

Person 3 owns the **syntax tree representation and XML generation**.

The parser must construct a tree representing the valid SPL program, which is then written to `tree.xml`.

```text
Parser
   ↓
Syntax Tree
   ↓
XML Generator
   ↓
tree.xml
```

### Responsibilities

* Design the generic `Node` class.
* Implement:

  * Unique node IDs
  * Parent references
  * Child references
  * Node contents
* Work with Person 2 to integrate tree construction into the parser.
* Implement XML generation.
* Ensure XML follows the specification.
* Validate generated XML.
* Test tree structure.
* Test XML output.
* Help integrate the lexer and parser into the complete application.

### Main classes

```text
tree/
├── Node.java
└── ...

xml/
├── XMLGenerator.java
└── ...
```

---

# 6. Shared Responsibilities

Although each person has a primary area, the project cannot be completed successfully if everyone works completely independently.

All three members share responsibility for:

### Integration

```text
Lexer → Parser → Syntax Tree → XML
```

### Testing

Each member should test their own component, but the entire team should also test the complete compiler.

### Debugging

If an integration problem occurs, the responsible team members should debug it together.

### Documentation

Everyone contributes to:

* README
* Architecture documentation
* Grammar documentation
* Testing documentation
* Setup instructions
* Design decisions

### Code review

Team members should review each other's changes before merging them.

---

# 7. LL(1) Grammar

The official grammar contains common-prefix situations that must be addressed for a straightforward LL(1) recursive-descent parser.

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

Again, both alternatives begin with:

```text
USER-DEFINED-NAME
```

These productions are therefore left-factored.

---

## Left-factored INSTR

```text
INSTR → print OUTP
      | nop
      | comment STRING
      | USER-DEFINED-NAME INSTR_TAIL
      | BRANCH
      | LOOP
```

with:

```text
INSTR_TAIL → = TERM
           | ( INPUT )
```

This allows the parser to determine whether a user-defined name represents an assignment or a function call by examining the next token.

---

# 8. Left-factored TERM

The term production is similarly factored:

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

Therefore:

```text
x
```

can represent a normal term, while:

```text
foo ( x )
```

can represent a function call.

---

# 9. Current Grammar

The current team grammar follows the official grammar with the necessary left-factoring.

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

The `var` keyword is **not part of the current grammar**.

---

# 10. Syntax Tree

The syntax tree will use a generic node structure.

```text
Node
├── id
├── contents
├── parent
└── children
```

### Root node

The root represents:

```text
SPL_PROG
```

### Inner nodes

Represent non-terminals such as:

```text
P
ALGO
TERM
BOOL
INSTR
```

### Leaf nodes

Represent consumed terminal tokens such as:

```text
print
123
x
(
)
```

Every node must have a unique ID.

Parent and child IDs will allow the tree to be represented correctly in XML and will also support later compiler phases.

---

# 11. XML Generation

The XML generator is separate from the parser.

```text
Parser
   ↓
Node tree
   ↓
XMLGenerator
   ↓
tree.xml
```

This separation ensures that:

* The parser focuses on syntax.
* The tree represents compiler structure.
* The XML generator handles output formatting.

This also makes the system easier to extend later.

---

# 12. Error Handling

The compiler must provide meaningful errors.

## Lexer errors

Possible examples:

```text
Invalid character
Invalid number
Invalid string
Unterminated string
Invalid user-defined name
Missing required blank space
```

## Parser errors

Possible examples:

```text
Unexpected token
Expected ')'
Expected ';'
Expected 'then'
Expected 'else'
Unexpected end of file
```

Errors should ideally contain:

* Location
* Unexpected token
* Expected token/construct
* Helpful hint

Example:

```text
Syntax Error:
Expected ')' after function arguments.

Line: 4

Hint:
Check that every '(' has a matching ')'.
```

---

# 13. Project Structure

```text
SPL-Compiler/
│
├── src/
│   │
│   ├── lexer/
│   │   ├── Lexer.java
│   │   ├── Token.java
│   │   └── TokenType.java
│   │
│   ├── parser/
│   │   ├── Parser.java
│   │   └── ...
│   │
│   ├── tree/
│   │   ├── Node.java
│   │   └── ...
│   │
│   ├── xml/
│   │   ├── XMLGenerator.java
│   │   └── ...
│   │
│   ├── errors/
│   │   ├── LexerException.java
│   │   ├── ParserException.java
│   │   └── ...
│   │
│   ├── semantic/
│   │   └── ...              # Future
│   │
│   ├── ir/
│   │   └── ...              # Future
│   │
│   └── Main.java
│
├── tests/
│   ├── lexer/
│   ├── parser/
│   ├── tree/
│   ├── xml/
│   └── integration/
│
├── examples/
│   ├── valid/
│   └── invalid/
│
├── README.md
└── ...
```

---

# 14. Development Timeline

## Phase 1 — Architecture

**All three**

* Agree on Java.
* Agree on project structure.
* Agree on token representation.
* Agree on parser/tree interfaces.
* Confirm grammar.
* Set up Git repository.

---

## Phase 2 — Lexer

**Person 1 leads**

* Implement lexer.
* Implement tokens.
* Implement token types.
* Implement lexical errors.
* Create lexer tests.

**Persons 2 and 3:**

* Review lexer interface.
* Create test inputs.
* Begin parser/tree development using the agreed interfaces.

---

## Phase 3 — Parser

**Person 2 leads**

* Verify FIRST/FOLLOW.
* Verify LL(1).
* Implement recursive-descent parser.
* Implement syntax errors.
* Create parser tests.

**Person 1:**

* Fix lexer issues discovered during parser integration.

**Person 3:**

* Implement tree structure and parser/tree integration.

---

## Phase 4 — Syntax Tree & XML

**Person 3 leads**

* Implement `Node`.
* Implement IDs.
* Implement parent/child relationships.
* Implement XML generator.
* Validate `tree.xml`.

**Person 2:**

* Integrate tree construction into parser.

**Person 1:**

* Assist with complete pipeline testing.

---

# 15. Full Integration

All three members work together on:

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

The goal is for the entire pipeline to run successfully from a single input program.

---

# 16. Assignment Strategy

The project has **two deadlines**.

We are **not** splitting the project into:

```text
Assignment 1 → Lexer
Assignment 2 → Parser
```

Instead, our strategy is:

> **Complete the entire currently specified practical by the first deadline, then use the period before the final deadline to improve the project based on marking/feedback and implement later compiler phases as their specifications become available.**

### First deadline

Target a complete implementation of the currently released specification:

```text
Lexer
  ↓
Parser
  ↓
Syntax Tree
  ↓
tree.xml
```

Including:

* Lexical error handling
* Syntax error handling
* Testing
* Integration
* Documentation

### After first deadline

Use feedback/marking to:

* Fix bugs
* Correct mistakes
* Improve error messages
* Improve XML
* Improve tree structure
* Increase test coverage
* Improve code quality
* Improve documentation

Then continue implementing later compiler phases as specifications are released.

### Final deadline

The final submission should be the **fully integrated, tested and refined compiler**, including the required later phases.

---

# 17. Definition of Done

## Lexer

* [ ] All specified lexical categories implemented.
* [ ] Token stream produced correctly.
* [ ] Invalid lexical input detected.
* [ ] Lexer tests written.

## Parser

* [ ] Grammar verified.
* [ ] LL(1) conflicts addressed.
* [ ] Recursive-descent parser implemented.
* [ ] Valid programs accepted.
* [ ] Invalid programs rejected.
* [ ] Meaningful syntax errors produced.
* [ ] Parser tests written.

## Syntax Tree

* [ ] Valid programs produce a syntax tree.
* [ ] Correct root node.
* [ ] Unique node IDs.
* [ ] Correct parent references.
* [ ] Correct child references.
* [ ] Correct terminal/non-terminal representation.

## XML

* [ ] `tree.xml` generated.
* [ ] XML is well-formed.
* [ ] Node IDs are unique.
* [ ] Parent/child references are correct.
* [ ] XML renders correctly.

## Integration

* [ ] Lexer connects to parser.
* [ ] Parser connects to syntax tree.
* [ ] Syntax tree connects to XML.
* [ ] Complete valid programs work.
* [ ] Complete invalid programs produce useful errors.

## Future phases

* [ ] Semantic analysis
* [ ] Type checking
* [ ] Intermediate representation
* [ ] Intermediate code generation
* [ ] Other required phases from later specifications

---

# 18. Architecture Decisions

| Decision      | Choice                     | Reason                                      |
| ------------- | -------------------------- | ------------------------------------------- |
| Team size     | 3 people                   | Three-person development team               |
| Language      | Java                       | OOP, testing, XML and extensibility         |
| Lexer         | Separate component         | Clean separation of lexical/syntax analysis |
| Parser        | Recursive descent          | Suitable for LL(1) grammar                  |
| Grammar       | Left-factored LL(1)        | Avoids common-prefix conflicts              |
| Tokens        | `Token` class              | Shared lexer/parser interface               |
| Token types   | `TokenType` enum           | Consistent token identification             |
| Tree          | Generic `Node`             | Flexible and extensible                     |
| IDs           | Unique IDs                 | Required by tree/XML specification          |
| XML           | Separate generator         | Keeps parser independent of output          |
| Errors        | Centralised error handling | Consistent messages                         |
| Testing       | Unit + integration tests   | Detect errors early                         |
| Future phases | Separate packages          | Allows compiler to grow                     |

---

# 19. Git Workflow

Each person should work on their own feature branch.

Example:

```text
main
│
├── feature/lexer
├── feature/parser
└── feature/syntax-tree
```

### Before merging

1. Pull the latest changes.
2. Resolve conflicts.
3. Run tests.
4. Test integration.
5. Review the changes.
6. Merge.

### Important

The team should agree on shared interfaces **before implementation gets too far**.

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

This allows all three people to work in parallel.

---

# 20. Final Project Philosophy

This project should be treated as:

> **One compiler, developed incrementally by three people.**

The team split is:

```text
             SPL COMPILER
                  │
       ┌──────────┼──────────┐
       │          │          │
   PERSON 1    PERSON 2    PERSON 3
       │          │          │
     LEXER      PARSER    TREE / XML
       │          │          │
       └──────────┼──────────┘
                  │
             INTEGRATION
                  │
                  ▼
          COMPLETE COMPILER
                  │
                  ▼
        FUTURE COMPILER PHASES
```

The first deadline is a target for a **complete working implementation of the currently released specification**.

The final deadline is for the **fully developed compiler**, incorporating feedback, fixes, testing and the additional compiler phases released later.

The three team members own different components, but the final product belongs to the **whole team**.
