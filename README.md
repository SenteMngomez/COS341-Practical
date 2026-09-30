# COS341 SPL Compiler Project

## 1. Project Overview

This project is a compiler for the **Students' Programming Language (SPL)**.

The compiler is being developed incrementally across multiple compiler phases. The current project includes the front-end phases and will later be extended with semantic analysis and code generation.

### Current compiler phases

The current front-end consists of:

* Lexical analysis
* Syntax analysis
* Syntax tree construction
* XML output
* Lexical and syntax error handling

Later project phases include:

* Semantic analysis
* Type analysis
* Intermediate representation
* Code generation

The project is therefore designed as **one modular compiler**, rather than separate programs for each assignment or project phase.

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
                  ┌───────────────────┐
                  │ FUTURE PHASES     │
                  │                   │
                  │ Semantic Analysis │
                  │ Type Analysis     │
                  │ Code Generation   │
                  └───────────────────┘
```

---

# 2. Project Phases and Assessment Scope

The project is divided into multiple phases.

## Phase 1 — Front-End

**Phase 1 consists of:**

```text
Lexical Analysis
       ↓
Syntax Analysis
       ↓
Syntax Tree
       ↓
XML Output
```

The Phase 1 front-end must be provided as a **pre-compiled, runnable executable**.

The tutors will perform **black-box testing** of the submitted executable. They will not inspect or recompile the source code.

### Phase 1 assessment

The Phase 1 submission is tested using **6 black-box test cases**, with each test case worth **0.5 points**.

Therefore:

```text
6 × 0.5 = 3 points maximum
```

Phase 1 is worth a maximum of **3 points**.

### Important

Phase 2 is **not part of the Phase 1 front-end submission**.

The complete semantic-analysis phase will be assessed later as part of the final project assessment.

---

# 3. Phase 1 Submission Requirements

The Phase 1 submission consists of:

1. A runnable executable containing the front-end.
2. A short user manual in PDF format.
3. Both files packaged into one ZIP file.

The filenames must contain the project group number.

For example:

```text
group-17.exe
group-17.pdf
group-17.zip
```

The user manual must contain:

* Instructions explaining how tutors operate the software.
* The full names of every group member.
* The student numbers of every group member.

### Upload responsibility

Only the designated **speaker** of the group uploads the ZIP file to ClickUp.

Other group members should **not** submit additional copies.

### Critical submission requirements

The executable must be runnable by the tutors.

The tutors:

* Will black-box test the executable.
* Will not inspect the source code.
* Will not recompile the source code.
* Will not attempt to repair a broken executable.

If the submitted executable cannot run, the group receives **0 points for the front-end assessment**.

The executable must therefore be tested on the intended target environment before submission.

---

# 4. Front-End Testing Strategy

The front-end should be tested using both automatically generated valid programs and deliberately invalid programs.

## 4.1 Automatically generating valid SPL programs

A useful testing technique is to create auxiliary software that generates syntactically valid SPL programs from the **start symbol of the SPL grammar**.

The generated programs do not need to make semantic sense.

Their purpose is to guarantee syntactic validity.

Conceptually:

```text
SPL_PROG
   ↓
Grammar productions
   ↓
Automatically generated SPL.txt
   ↓
Lexer
   ↓
Parser
```

If the generator correctly follows the grammar, the generated programs should be syntactically valid and therefore parseable by the parser.

This provides a useful way of testing the parser over a large variety of grammar combinations.

## 4.2 Testing syntax-error detection

A correctly generated `SPL.txt` can then be manually modified by introducing small syntax errors.

For example:

```text
Correct:
foo ( x )

Modified:
foo ( x
```

The parser should detect the deliberately introduced syntax error.

Because the location of the error is known, the resulting error message can also be evaluated for usefulness and correctness.

---

# 5. The `$` Symbol

The `$` symbol in the grammar is a **meta-symbol used to reason about the parser**.

It is **not an actual member-token of the SPL language**.

Therefore:

```text
SPL_PROG → P $
```

does **not** mean that an SPL input file should contain `$` at the end.

A real `SPL.txt` file must **not contain `$` merely because the grammar uses it as the end-of-input marker**.

The parser therefore must not attempt to consume a `$` token from `SPL.txt`.

---

# 6. Team Structure

There are **three people working on the project**.

The project is divided according to compiler components rather than into three separate projects.

| Team Member  | Main Responsibility      | Secondary Responsibility       |
| ------------ | ------------------------ | ------------------------------ |
| **Person 1** | Lexer / Lexical Analysis | Token system & lexical testing |
| **Person 2** | Parser / Syntax Analysis | Grammar, LL(1), parser errors  |
| **Person 3** | Syntax Tree / XML        | Integration, tree/XML testing  |

The three people are working on **one compiler**.

Everyone remains responsible for:

* Integration
* Testing
* Debugging
* Code reviews
* Documentation
* Helping with components when required

---

# 7. Person 1 — Lexer / Lexical Analysis

## Main responsibility

Person 1 owns the lexical analysis phase.

The lexer converts raw SPL source code into tokens.

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
  * Other grammar terminals
* Enforce whitespace/blank-space rules.
* Detect invalid lexical structures.
* Implement the `Token` class.
* Implement the `TokenType` enum.
* Produce the token stream consumed by the parser.
* Write lexer tests.

### Main classes

```text
lexer/
├── Lexer.java
├── Token.java
└── TokenType.java
```

---

# 8. Person 2 — Parser / Syntax Analysis

## Main responsibility

Person 2 owns the syntax analysis phase.

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
* Verify LL(1) suitability.
* Calculate/check FIRST and FOLLOW sets.
* Apply left-factoring where required.
* Implement the parser.
* Consume tokens from the lexer.
* Recognise grammar productions.
* Detect syntax errors.
* Produce meaningful syntax-error messages.
* Work with Person 3 to construct the syntax tree.
* Write parser tests.

### Parser approach

The intended approach is a **recursive-descent parser** based on the left-factored LL(1) grammar.

Conceptually:

```text
parseSPL_PROG()
       ↓
    parseP()
       ↓
 ┌─────┼─────────┐
 ↓     ↓         ↓
V_DECL F_DECL   ALGO
```

---

# 9. Person 3 — Syntax Tree / XML / Integration

## Main responsibility

Person 3 owns the syntax-tree representation and XML generation.

The parser constructs a tree representing the valid SPL program, which is then written to `tree.xml`.

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
* Integrate tree construction with the parser.
* Implement XML generation.
* Ensure XML follows the specification.
* Validate generated XML.
* Test tree structure.
* Test XML output.
* Help integrate the complete compiler pipeline.

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

# 10. Shared Responsibilities

Although each person has a primary area, the compiler cannot be developed successfully as three completely independent programs.

All three members share responsibility for:

## Integration

```text
Lexer → Parser → Syntax Tree → XML
```

## Testing

Each member should test their own component while the complete team also tests the integrated compiler.

## Debugging

Integration problems should be investigated by the relevant team members together.

## Documentation

Everyone contributes to:

* README
* Architecture documentation
* Grammar documentation
* Testing documentation
* Setup instructions
* Design decisions
* User manual

## Code review

Team members should review each other's changes before merging them.

---

# 11. LL(1) Grammar

The official grammar contains common-prefix situations that must be addressed for a straightforward LL(1) recursive-descent parser.

For example:

```text
INSTR → ASSIGN | CALL
```

Both alternatives begin with a user-defined name.

Similarly:

```text
TERM → USER-DEFINED-NAME | CALL
```

These productions therefore require left-factoring.

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

This allows the parser to distinguish between an assignment and a function call by examining the next token.

## Left-factored TERM

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

# 12. Current Grammar

The current team grammar follows the official grammar with the required left-factoring.

> **Note:** `$` is shown here only because it is part of the grammar notation. It is not included in an actual `SPL.txt` input file.

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

## Instructions

```text
INSTR → print OUTP
      | nop
      | comment STRING
      | USER-DEFINED-NAME INSTR_TAIL
      | BRANCH
      | LOOP
```

## Instruction tail

```text
INSTR_TAIL → = TERM
           | ( INPUT )
```

## Output

```text
OUTP → ( TERM )
     | STRING
```

## Terms

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

## Term rest

```text
TERM_REST → ε
          | ( INPUT )
```

## Input

```text
INPUT → ε
      | TERM INPUT
```

## Boolean expressions

```text
BOOL → not ( BOOL )
     | and ( BOOL BOOL )
     | or ( BOOL BOOL )
     | eq ( TERM TERM )
     | larger ( TERM TERM )
     | lesser ( TERM TERM )
```

## Branch

```text
BRANCH → if BOOL then { ALGO } else { ALGO }
```

## Loops

```text
LOOP → COND BOOL do { ALGO }
     | do { ALGO } COND BOOL
```

## Conditions

```text
COND → while
     | until
```

The `var` keyword is **not part of the current grammar**.

---

# 13. Syntax Tree

The syntax tree uses a generic node structure.

```text
Node
├── id
├── contents
├── parent
└── children
```

## Root node

The root represents:

```text
SPL_PROG
```

## Inner nodes

Inner nodes represent non-terminals such as:

```text
P
ALGO
TERM
BOOL
INSTR
```

## Leaf nodes

Leaf nodes represent consumed terminal tokens such as:

```text
print
123
x
(
)
```

Every node must have a unique ID.

Parent and child references allow the tree to be represented correctly in XML and provide the structure required by later compiler phases.

---

# 14. XML Generation

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

The resulting tree representation will also serve as input to later semantic-analysis phases.

---

# 15. Error Handling

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

# 16. Phase 2 — Semantic Analysis

Phase 2 is separate from the Phase 1 front-end.

It is **not required for the September Phase 1 front-end submission**.

Phase 2 will be assessed later as part of the final project assessment.

The supplied Phase 2 specification currently includes **Phase 2b: Type Analysis**.

---

# 17. Phase 2b — Type Analysis

The type analyser operates on the syntax tree produced by the parser.

The Phase 2b specification assumes that, during the preceding Phase 2a, user-defined names have been consistently replaced by system-generated names and that the relevant information is available in a symbol table.

The type analyser then fills the symbol table with type information while traversing the syntax tree.

Initially:

```text
type = unknown
```

for each relevant node.

During type analysis, these attributes are progressively replaced with more specific information.

Possible types include:

```text
unknown
numeric
boolean
procedure
ok
```

The type analyser must emit an appropriate error if the syntax tree violates one of the specified semantic rules.

If no statically detectable type error exists, it must not emit an error and the syntax tree should be ready for the subsequent code-generation phase.

---

# 18. Phase 2b Type Rules

## Program

```text
SPL_PROG → P
```

If:

```text
type_of(P) = ok
```

then:

```text
type_of(SPL_PROG) = ok
```

## Program body

```text
P → V_DECL : F_DECL : ALGO
```

requires:

```text
type_of(V_DECL) = ok
type_of(F_DECL) = ok
type_of(ALGO) = ok
```

Then:

```text
type_of(P) = ok
```

## Variable declarations

```text
V_DECL → ε
```

gives:

```text
type_of(V_DECL) = ok
```

For:

```text
V_DECL → NAME V_DECL
```

the declared `NAME` becomes:

```text
numeric
```

provided the remainder of the declaration is valid.

## Function declarations

A `void` function gives its name the type:

```text
procedure
```

A `num` function gives its name the type:

```text
numeric
```

provided its body, declarations and return expression satisfy the relevant rules.

---

# 19. Type Rules for Expressions and Instructions

## Output

For:

```text
OUTP → ( TERM )
```

the `TERM` must be:

```text
numeric
```

String output is valid:

```text
OUTP → STRING
```

## Print

```text
INSTR → print OUTP
```

is valid when:

```text
type_of(OUTP) = ok
```

## Assignment

```text
ASSIGN → NAME = TERM
```

requires:

```text
type_of(NAME) = numeric
type_of(TERM) = numeric
```

## Terms

A `NAME` used as a term must be numeric:

```text
TERM → NAME
```

A numeric literal is numeric:

```text
TERM → NUM
```

A function call is numeric when the called function has type:

```text
numeric
```

Arithmetic operations such as:

```text
mod
add
sub
mul
div
neg
```

produce a numeric result when their operands satisfy the relevant numeric rules.

---

# 20. Special `mod` / `div` Rule

The Phase 2b specification introduces a deliberately simplified rule for handling integer and floating-point numbers.

If the syntax tree contains **both**:

```text
mod
```

and:

```text
div
```

the entire syntax tree must be rejected.

The required error message is:

```text
A float-integer-conflict might perhaps be possible
```

Additionally, if the syntax tree contains a `mod` node, no `NUM` token at the leaves of the tree may contain a decimal point.

This simplified rule avoids requiring the type analyser to distinguish integer and floating-point subtypes throughout the entire syntax tree.

---

# 21. Boolean Type Analysis

Boolean expressions have type:

```text
boolean
```

when their operands satisfy the relevant rules.

## NOT

```text
BOOL → not ( BOOL )
```

requires the inner `BOOL` to be:

```text
boolean
```

## AND / OR

```text
BOOL → and ( BOOL BOOL )
BOOL → or ( BOOL BOOL )
```

require both operands to be:

```text
boolean
```

## Comparisons

```text
BOOL → eq ( TERM TERM )
BOOL → larger ( TERM TERM )
BOOL → lesser ( TERM TERM )
```

require both terms to be:

```text
numeric
```

The resulting boolean expression then has type:

```text
boolean
```

---

# 22. Branch and Loop Type Analysis

## Branches

```text
BRANCH → if BOOL then { ALGO } else { ALGO }
```

requires:

```text
type_of(BOOL) = boolean
type_of(ALGO) = ok
type_of(ALGO) = ok
```

The branch then has type:

```text
ok
```

## Conditions

Both conditions are valid:

```text
COND → while
COND → until
```

and have type:

```text
ok
```

## Loops

Both loop forms:

```text
LOOP → COND BOOL do { ALGO }
```

and:

```text
LOOP → do { ALGO } COND BOOL
```

require:

```text
type_of(COND) = ok
type_of(BOOL) = boolean
type_of(ALGO) = ok
```

The resulting loop has type:

```text
ok
```

---

# 23. Type-Analysis Algorithm

The type analyser will implement a **tree-crawler algorithm** over the syntax tree.

Conceptually:

```text
Syntax Tree
     ↓
Tree Crawler
     ↓
Symbol Table
     ↓
Type Information
     ↓
Semantic Errors / Valid Tree
```

The algorithm must:

1. Traverse the syntax tree.
2. Consult the symbol table for names.
3. Apply the semantic/type rule associated with each grammar production.
4. Update type information as rules are satisfied.
5. Detect statically identifiable type errors.
6. Emit appropriate error messages for invalid trees.
7. Produce no error for a tree that satisfies all specified rules.

A valid tree and its corresponding symbol table will then be ready for the subsequent code-generation phase.

---

# 24. Project Structure

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
│   │   ├── TypeAnalyzer.java
│   │   └── ...
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
│   ├── semantic/
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

# 25. Development Timeline

## Phase 1 — Architecture

**All three members**

* Agree on Java.
* Agree on project structure.
* Agree on token representation.
* Agree on parser/tree interfaces.
* Confirm grammar.
* Set up Git repository.

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
* Begin parser/tree development using agreed interfaces.

## Phase 3 — Parser

**Person 2 leads**

* Verify FIRST/FOLLOW.
* Verify LL(1).
* Implement recursive-descent parser.
* Implement syntax errors.
* Create parser tests.

**Person 1:**

* Fix lexer issues discovered during integration.

**Person 3:**

* Implement tree structure.
* Integrate tree construction with parser.

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

## Phase 5 — Semantic Analysis

After the front-end is complete and Phase 2 specifications are applicable:

* Implement symbol-table support.
* Implement semantic analysis.
* Implement Phase 2a requirements.
* Implement Phase 2b type analysis.
* Implement type-error reporting.
* Add semantic-analysis tests.

## Phase 6 — Code Generation

After the relevant specification is released:

* Implement the required intermediate representation.
* Implement code generation.
* Integrate code generation with the semantic-analysis pipeline.
* Test the complete compiler.

---

# 26. Full Integration

The complete compiler should eventually operate as:

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
Semantic Analysis
   ↓
Type Analysis
   ↓
Intermediate Representation
   ↓
Code Generation
   ↓
Generated Output
```

For the Phase 1 front-end, the required pipeline is:

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

---

# 27. Testing Strategy

Testing is divided into component testing, integration testing and black-box testing.

## Lexer testing

Test:

* Valid names.
* Valid numbers.
* Strings.
* Keywords.
* Symbols.
* Whitespace rules.
* Invalid characters.
* Invalid lexical structures.

## Parser testing

Test:

* Automatically generated syntactically valid programs.
* Manually written valid programs.
* Deliberately corrupted programs.
* Missing delimiters.
* Incorrect keywords.
* Missing semicolons.
* Unexpected tokens.
* Incorrect nesting.
* End-of-input errors.

## Syntax tree testing

Test:

* Correct root.
* Correct node contents.
* Unique IDs.
* Parent relationships.
* Child relationships.
* Terminal/non-terminal representation.

## XML testing

Test:

* Well-formed XML.
* Correct node representation.
* Correct IDs.
* Correct parent/child relationships.
* Correct output file generation.

## Semantic/type testing

Test:

* Valid numeric expressions.
* Valid boolean expressions.
* Function types.
* Procedure calls.
* Assignments.
* Invalid numeric operations.
* Invalid boolean operations.
* `mod`/`div` conflicts.
* Decimal numbers in trees containing `mod`.
* Invalid function return expressions.

---

# 28. Assignment Strategy

The project should be developed as **one compiler**, rather than as independent assignment programs.

The immediate objective for Phase 1 is:

```text
Complete Front-End
        │
        ├── Lexer
        ├── Parser
        ├── Syntax Tree
        ├── XML
        ├── Error Handling
        └── Testing
```

After Phase 1:

* Fix bugs identified during testing.
* Improve error messages.
* Improve XML output.
* Improve tree structure.
* Increase test coverage.
* Improve documentation.
* Continue with the semantic-analysis phases.

Phase 2, including type analysis, belongs to the later project assessment rather than the September front-end submission.

---

# 29. Definition of Done

## Lexer

* [ ] All specified lexical categories implemented.
* [ ] Token stream produced correctly.
* [ ] Invalid lexical input detected.
* [ ] Lexer tests written.
* [ ] Whitespace rules correctly enforced.

## Parser

* [ ] Grammar verified.
* [ ] LL(1) conflicts addressed.
* [ ] Recursive-descent parser implemented.
* [ ] Valid programs accepted.
* [ ] Invalid programs rejected.
* [ ] Meaningful syntax errors produced.
* [ ] `$` is not expected as a literal input token.
* [ ] Parser tests written.
* [ ] Automatically generated valid SPL programs successfully parsed.
* [ ] Deliberately corrupted valid programs correctly rejected.

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

## Phase 1 Executable

* [ ] Executable is pre-compiled.
* [ ] Executable runs successfully on the intended environment.
* [ ] Executable has been tested independently of the development environment.
* [ ] User manual created.
* [ ] All group member names included in user manual.
* [ ] All student numbers included in user manual.
* [ ] Executable filename contains group number.
* [ ] PDF filename contains group number.
* [ ] ZIP filename contains group number.
* [ ] Only the designated speaker uploads the ZIP.

## Integration

* [ ] Lexer connects to parser.
* [ ] Parser connects to syntax tree.
* [ ] Syntax tree connects to XML.
* [ ] Complete valid programs work.
* [ ] Complete invalid programs produce useful errors.
* [ ] Complete pipeline tested.

## Phase 2 — Semantic / Type Analysis

* [ ] Phase 2a requirements implemented.
* [ ] Symbol table contains required semantic information.
* [ ] Type analyser implemented.
* [ ] Syntax-tree crawler implemented.
* [ ] Type information initially represented as `unknown`.
* [ ] Type information updated according to semantic rules.
* [ ] Numeric expressions checked.
* [ ] Boolean expressions checked.
* [ ] Function/procedure types checked.
* [ ] Assignment types checked.
* [ ] `mod`/`div` conflict detected.
* [ ] Decimal-number restriction for `mod` trees enforced.
* [ ] Appropriate semantic errors produced.
* [ ] Valid trees produce no type-analysis errors.

## Future phases

* [ ] Intermediate representation.
* [ ] Code generation.
* [ ] Other required phases from later specifications.

---

# 30. Architecture Decisions

| Decision          | Choice                                 | Reason                                        |
| ----------------- | -------------------------------------- | --------------------------------------------- |
| Team size         | 3 people                               | Three-person development team                 |
| Language          | Java                                   | OOP, testing, XML and extensibility           |
| Lexer             | Separate component                     | Clean separation of lexical/syntax analysis   |
| Parser            | Recursive descent                      | Suitable for LL(1) grammar                    |
| Grammar           | Left-factored LL(1)                    | Avoids common-prefix conflicts                |
| Tokens            | `Token` class                          | Shared lexer/parser interface                 |
| Token types       | `TokenType` enum                       | Consistent token identification               |
| Tree              | Generic `Node`                         | Flexible and extensible                       |
| IDs               | Unique IDs                             | Required by tree/XML specification            |
| XML               | Separate generator                     | Keeps parser independent of output            |
| Errors            | Centralised error handling             | Consistent messages                           |
| Testing           | Unit + integration + black-box testing | Detect errors at multiple levels              |
| Semantic analysis | Separate phase                         | Keeps front-end and semantic analysis modular |
| Type analysis     | Tree crawler + symbol table            | Matches Phase 2b specification                |
| Future phases     | Separate packages                      | Allows compiler to grow                       |

---

# 31. Git Workflow

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

The team should agree on shared interfaces before implementation gets too far.

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
  ↓
tree.xml
```

Later:

```text
tree.xml / Syntax Tree
        ↓
Symbol Table
        ↓
Type Analyzer
        ↓
Semantic Validation
        ↓
Code Generation
```

---

# 32. Final Project Philosophy

This project should be treated as:

> **One compiler, developed incrementally by three people.**

The team split is:

```text
                 SPL COMPILER
                      │
          ┌───────────┼───────────┐
          │           │           │
      PERSON 1    PERSON 2    PERSON 3
          │           │           │
        LEXER       PARSER     TREE / XML
          │           │           │
          └───────────┼───────────┘
                      │
                 INTEGRATION
                      │
                      ▼
                FRONT-END
                      │
                      ▼
             SEMANTIC ANALYSIS
                      │
                      ▼
               TYPE ANALYSIS
                      │
                      ▼
              CODE GENERATION
                      │
                      ▼
             COMPLETE COMPILER
```

The **Phase 1 deadline** is the target for a complete, runnable and testable front-end.

The **final project assessment** extends the compiler with the later semantic-analysis, type-analysis and code-generation requirements.

The three team members own different components, but the final product belongs to the **whole team**.
