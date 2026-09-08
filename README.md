# COS341 SPL Syntax Practical — Project Plan

## 1. Project Strategy

Our group has chosen the **two-deadline option**.

However, our development strategy is **not** to split the practical into:

```text
Assignment 1 = Lexer
Assignment 2 = Parser
```

Instead, we will aim to complete the **entire practical before the first deadline**.

The second deadline will then be used as a **correction, improvement and finalisation period**.

Our approach is:

```text
                    DEVELOPMENT
                         │
                         ▼
              ┌─────────────────────┐
              │ Complete ENTIRE     │
              │ project             │
              │                     │
              │ Lexer               │
              │ Parser              │
              │ Syntax Tree         │
              │ XML                 │
              │ Error Handling      │
              │ Testing             │
              └──────────┬──────────┘
                         │
                         ▼
                 FIRST DEADLINE
                         │
                         ▼
                    MARKING /
                    FEEDBACK
                         │
                         ▼
              ┌─────────────────────┐
              │ FIX + IMPROVE       │
              │                     │
              │ Bugs                │
              │ Marks lost          │
              │ Parser issues       │
              │ Lexer issues        │
              │ XML issues          │
              │ Error messages      │
              │ Testing             │
              └──────────┬──────────┘
                         │
                         ▼
                FINAL DEADLINE
                25 OCTOBER
```

This gives us two advantages:

1. We try to have a complete working solution as early as possible.
2. We still get feedback before the final submission and can correct problems.

---

# 2. Architecture

Our group has decided to use a **separate lexer in front of the parser**.

The practical explicitly allows this approach, describing the option of "plugging" a separate lexer module in front of the parser.

Our final architecture is therefore:

```text
                         SPL.txt
                            │
                            ▼
                    ┌──────────────┐
                    │    LEXER     │
                    │              │
                    │ Person 1     │
                    └──────┬───────┘
                           │
                           ▼
                     TOKEN STREAM
                           │
                           ▼
                    ┌──────────────┐
                    │    PARSER    │
                    │              │
                    │ Person 2     │
                    │ Person 3     │
                    └──────┬───────┘
                           │
                           ▼
                     SYNTAX TREE
                           │
                           ▼
                    ┌──────────────┐
                    │  XML WRITER  │
                    │              │
                    │ Person 3     │
                    └──────┬───────┘
                           │
                           ▼
                       tree.xml
```

---

# 3. What the project actually does

The project is a syntax analyser for SPL.

We are **not** executing SPL programs.

The system takes:

```text
SPL.txt
```

and analyses whether the program is syntactically correct.

If it is invalid:

```text
SPL.txt
   ↓
Lexer
   ↓
Tokens
   ↓
Parser
   ↓
Syntax Error
```

If it is valid:

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
tree.xml
```

The practical states that an invalid SPL program must result in a meaningful syntax error and that a valid program must result in `tree.xml` containing the syntax tree.

---

# 4. Group Division

We are a group of three.

The division is based on **frontend vs backend**, while still requiring everyone to understand and test the complete system.

| Person       | Main Role | Primary Responsibility     |
| ------------ | --------- | -------------------------- |
| **Person 1** | Frontend  | Lexer                      |
| **Person 2** | Backend   | Parser / Grammar           |
| **Person 3** | Backend   | Parser / Syntax Tree / XML |

This does **not** mean that each person only works on their own section.

The complete system must be integrated and tested by everyone.

---

# 5. Person 1 — Lexer / Frontend

## Main responsibility

Person 1 owns the lexer.

The lexer converts:

```text
Characters → Tokens
```

For example:

```text
SPL.txt
   ↓
characters
   ↓
LEXER
   ↓
tokens
```

## Tasks

Person 1 is responsible for:

* Reading `SPL.txt`
* Character processing
* Tokenisation
* Token types
* Keyword recognition
* Number recognition
* User-defined-name recognition
* String recognition
* Symbol recognition
* Lexical errors
* Token position information
* Lexer testing
* Lexer/parser interface

The practical specifies lexical categories including `NUM`, `USER-DEFINED-NAME`, and `STRING`, with their corresponding regular expressions.

---

# 6. Person 2 — Parser / Grammar

## Main responsibility

Person 2 owns the main parser logic.

The parser converts:

```text
Tokens → Valid/Invalid SPL
```

The parser follows the supplied SPL context-free grammar.

Examples of grammar areas include:

```text
SPL_PROG
P
V_DECL
F_DECL
F_TYPE
ALGO
INSTR
TERM
BOOL
BRANCH
LOOP
CALL
```

The complete grammar is provided in the practical specification.

## Tasks

Person 2 is responsible for:

* Grammar analysis
* LL(1) analysis
* FIRST/FOLLOW analysis
* Identifying conflicts
* Deciding how to handle conflicts
* Parser design
* Parser implementation
* Token consumption
* Grammar-rule implementation
* Syntax validation
* Parser testing

The specification specifically requires the group to analyse whether the grammar is suitable for LL(1) parsing.

---

# 7. Person 3 — Parser / Syntax Tree / XML

## Main responsibility

Person 3 works on the parser backend alongside Person 2, with particular ownership of the syntax-tree and XML side.

The flow is:

```text
Parser
   ↓
Syntax Tree
   ↓
tree.xml
```

## Tasks

Person 3 is responsible for:

* Syntax-tree data structure
* Tree-node creation
* Unique node IDs
* Parent references
* Child references
* Node contents
* XML generation
* XML validation
* Error-output support
* Tree testing

The practical requires every tree node to have a unique ID and specifies the information required for root, inner and leaf nodes.

---

# 8. Shared Responsibilities

Although each person has an owner area, the following are **group responsibilities**:

* Understanding the specification
* Understanding the grammar
* LL(1) decision
* Integration
* Testing
* Debugging
* Documentation
* Final submission
* Test-day preparation

Nobody should be in a situation where they only understand their own component.

Everyone should understand:

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
XML
```

---

# 9. First Deadline — Complete Everything

## Goal

Our goal is to have a **complete working implementation by the first deadline**.

This means we should NOT plan:

```text
First deadline → only lexer
Second deadline → parser
```

Instead:

```text
First deadline → COMPLETE PROJECT
Second deadline → FIX + IMPROVE COMPLETE PROJECT
```

By the first deadline, we want:

### Lexer

```text
SPL.txt
   ↓
Lexer
   ↓
Correct tokens
```

### Parser

```text
Tokens
   ↓
Parser
   ↓
Valid / Invalid
```

### Syntax tree

```text
Valid program
   ↓
Syntax tree
```

### XML

```text
Syntax tree
   ↓
tree.xml
```

### Error handling

```text
Invalid program
   ↓
Meaningful syntax error
```

### Testing

All major grammar rules should already have been tested.

---

# 10. First Deadline Development Plan

## Stage 1 — Architecture

All three members agree on:

* Lexer design
* Token types
* Parser interface
* Tree-node structure
* XML structure
* Error format
* Git workflow

 **Possible architecture/design decisions**

1. **Programming language — Java**
   Use Java because it supports a clean object-oriented implementation of the lexer, parser, syntax tree and XML generator.

2. **Lexer–parser separation**
   Implement a dedicated lexer before the parser:
   `SPL.txt → Lexer → Tokens → Parser`.
   This keeps lexical analysis separate from syntax analysis.

3. **Token representation**
   Represent every lexical unit using a `Token` class containing at least its token type and relevant value/lexeme.

4. **Token types using an enum**
   Use a `TokenType` enum for keywords, operators, identifiers, numbers, strings and punctuation. This avoids comparing raw strings throughout the parser.

5. **Recursive-descent parser**
   Implement the parser using separate methods corresponding to grammar productions, such as `parseProgram()`, `parseInstruction()`, `parseTerm()`, etc.

6. **Grammar handling / LL(1)**
   Analyse the supplied grammar for LL(1) compatibility. Where common prefixes prevent straightforward predictive parsing, factor the grammar or otherwise adapt the parser rather than duplicating parsing logic. The specification explicitly requires your group to make this decision. 

7. **Generic syntax-tree nodes**
   Use a generic `Node` structure containing `id`, `contents`, `parent` and `children`, matching the required XML tree information. 

8. **Parser builds the tree directly**
   As the parser recognises grammar productions, it creates the corresponding syntax-tree nodes. This avoids having to parse the program a second time.

9. **Separate XML generation**
   Keep XML generation separate from parsing. The parser produces a syntax tree; an `XMLGenerator` converts that tree into `tree.xml`. This makes it easier to test the parser independently.

10. **Centralised error handling and testing**
    Use a dedicated syntax-error mechanism so errors can report what was expected and where the problem occurred. Build tests around valid and invalid SPL programs because the assignment requires meaningful syntax errors for invalid programs and a valid `tree.xml` for valid programs. 

### The overall decision

```text
                    SPL.txt
                       │
                       ▼
                ┌─────────────┐
                │    Lexer    │
                └──────┬──────┘
                       │
                    Tokens
                       │
                       ▼
                ┌─────────────┐
                │   Parser    │
                └──────┬──────┘
                       │
                 Syntax Tree
                       │
                       ▼
                ┌─────────────┐
                │ XMLGenerator│
                └──────┬──────┘
                       │
                       ▼
                   tree.xml
```
---

## Stage 2 — Lexer

Person 1 develops:

```text
SPL.txt → Tokens
```

Persons 2 and 3 test the output and make sure it supports the parser.

---

## Stage 3 — Grammar

Persons 2 and 3:

* Analyse the grammar
* Determine LL(1) suitability
* Identify conflicts
* Decide how the parser will handle them
* Document the decision

---

## Stage 4 — Parser

Person 2 leads implementation of:

```text
Tokens → Parser
```

Person 3 assists with parser integration and testing.

---

## Stage 5 — Syntax Tree

Person 3 implements:

```text
Parser → Syntax Tree
```

with:

* IDs
* Contents
* Parents
* Children

---

## Stage 6 — XML

Person 3 implements:

```text
Syntax Tree → tree.xml
```

---

## Stage 7 — Integration

All three connect:

```text
SPL.txt
   ↓
Lexer
   ↓
Token stream
   ↓
Parser
   ↓
Syntax tree
   ↓
tree.xml
```

---

## Stage 8 — Testing

The group tests:

### Valid programs

* Variable declarations
* Function declarations
* Printing
* Assignments
* Function calls
* Arithmetic
* Boolean expressions
* If/else
* Loops
* Nested structures
* Empty/nullable productions

### Invalid programs

* Missing tokens
* Incorrect tokens
* Missing semicolons
* Missing braces
* Missing parentheses
* Invalid expressions
* Invalid function calls
* Invalid assignments
* Invalid Boolean expressions
* Invalid declarations

---

# 11. After the First Deadline

Once the first version has been submitted and marked, we should treat the feedback as a **debugging report** for the project.

```text
FIRST SUBMISSION
       │
       ▼
    MARKING
       │
       ▼
    FEEDBACK
       │
       ▼
┌─────────────────────┐
│ What did we get    │
│ wrong?              │
│                     │
│ Lexer?              │
│ Parser?             │
│ Grammar?            │
│ XML?                │
│ Error handling?     │
│ Testing?            │
└──────────┬──────────┘
           │
           ▼
       FIX ISSUES
           │
           ▼
      RETEST SYSTEM
           │
           ▼
     FINAL VERSION
```

---

# 12. Second Deadline — 25 October

The second deadline is the **final submission deadline**.

The final deadline is **25 October 2026** and cannot be changed.

The time between the first deadline and 25 October should therefore be treated as a **quality-improvement period**.

## Tasks after the first submission

### 1. Fix feedback

Address every issue identified in the marking.

### 2. Fix bugs

Find problems that weren't caught before the first deadline.

### 3. Improve error messages

Make syntax errors more useful and understandable.

### 4. Improve XML

Verify:

* Unique IDs
* Parent references
* Child references
* Correct contents
* Correct tree structure
* Valid XML

### 5. Expand testing

Add additional valid and invalid SPL programs.

### 6. Integration testing

Run the complete pipeline repeatedly:

```text
SPL.txt
 ↓
Lexer
 ↓
Parser
 ↓
tree.xml
```

### 7. Test-day simulation

Use SPL programs that the team has not previously used and verify that the entire system behaves correctly.

---

# 13. Timeline

```text
NOW
 │
 │
 ├── Understand specification
 │
 ├── Agree architecture
 │
 ├── Build lexer
 │
 ├── Analyse grammar
 │
 ├── Build parser
 │
 ├── Build syntax tree
 │
 ├── Build XML
 │
 ├── Error handling
 │
 ├── Testing
 │
 ▼
FIRST DEADLINE
 │
 │
 ├── COMPLETE PROJECT
 │
 ├── Receive marking/feedback
 │
 ▼
POST-FIRST-DEADLINE
 │
 ├── Fix lexer problems
 ├── Fix parser problems
 ├── Fix grammar problems
 ├── Fix XML problems
 ├── Improve error messages
 ├── Expand tests
 ├── Integration testing
 │
 ▼
25 OCTOBER
 │
 ▼
FINAL SUBMISSION
```

---

# 14. What "Complete by the First Deadline" Means

The first deadline should represent a **fully functioning minimum final product**, not a half-finished project.

We should be able to demonstrate:

```text
              SPL.txt
                 │
                 ▼
              LEXER
                 │
                 ▼
              TOKENS
                 │
                 ▼
              PARSER
                 │
           ┌─────┴─────┐
           │           │
        INVALID       VALID
           │           │
           ▼           ▼
       Error       Syntax Tree
                       │
                       ▼
                   tree.xml
```

If we achieve this before the first deadline, the second deadline becomes our opportunity to **make the project correct, robust and polished rather than trying to finish it from scratch**.

---

# 15. Definition of Done

## First Deadline

We want:

* [] Lexer implemented
* [] Token types implemented
* [] Grammar analysed
* [] LL(1) decision made
* [] Parser implemented
* [] Syntax tree implemented
* [] Unique node IDs implemented
* [] Parent/child relationships implemented
* [] XML generation implemented
* [] Syntax errors implemented
* [] Valid SPL tested
* [] Invalid SPL tested
* [] Full lexer → parser → XML pipeline working

## Final Deadline — 25 October

We want:

* [] First-version feedback addressed
* [] Lexer bugs fixed
* [] Parser bugs fixed
* [] Tree/XML issues fixed
* [] Error messages improved
* [] Additional testing completed
* [] Full integration verified
* [] Documentation completed
* [] Final system stable
* [] Final submission ready

---

# 16. Our Overall Strategy

### **BUILD EARLY → SUBMIT → GET FEEDBACK → FIX → FINALISE**

The first deadline is our **complete-project checkpoint**.

The second deadline is our **final corrected version**.

This is preferable to deliberately leaving the parser/backend until after the first deadline because it means that, even if something goes wrong with the first marking, we already have the entire system implemented and can focus the remaining time on fixing it.

--- 
# 17. Possible file structure 
SPL Compiler
│
├── Lexer
│   ├── Lexer
│   ├── Token
│   └── TokenType
│
├── Parser
│   ├── Parser
│   ├── Grammar
│   └── ParseException
│
├── Syntax Tree
│   ├── Node
│   └── SyntaxTree
│
├── XML
│   └── XMLGenerator
│
├── Errors
│   └── SyntaxError
│
└── Main

Our target should therefore be:

> **"Complete everything once. Then use the second deadline to make everything better and correct anything we got wrong."**
