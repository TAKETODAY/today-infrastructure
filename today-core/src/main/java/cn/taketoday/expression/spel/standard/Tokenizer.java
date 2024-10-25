/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.expression.spel.standard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import cn.taketoday.expression.spel.InternalParseException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.expression.spel.SpelParseException;

/**
 * Lex some input data into a stream of tokens that can then be parsed.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class Tokenizer {

  /**
   * Alternative textual operator names which must match enum constant names
   * in {@link TokenKind}.
   * <p>Note that {@code AND} and {@code OR} are also alternative textual
   * names, but they are handled later in {@link InternalSpelExpressionParser}.
   * <p>If this list gets changed, it must remain sorted since we use it with
   * {@link Arrays#binarySearch(Object[], Object)}.
   */
  private static final String[] ALTERNATIVE_OPERATOR_NAMES = {
          "DIV", "EQ", "GE", "GT", "LE", "LT", "MOD", "NE", "NOT"
  };

  private static final byte[] FLAGS = new byte[256];

  private static final byte IS_DIGIT = 0x01;

  private static final byte IS_HEXDIGIT = 0x02;

  static {
    for (int ch = '0'; ch <= '9'; ch++) {
      FLAGS[ch] |= IS_DIGIT | IS_HEXDIGIT;
    }
    for (int ch = 'A'; ch <= 'F'; ch++) {
      FLAGS[ch] |= IS_HEXDIGIT;
    }
    for (int ch = 'a'; ch <= 'f'; ch++) {
      FLAGS[ch] |= IS_HEXDIGIT;
    }
  }

  private final String expressionString;

  private final char[] charsToProcess;

  private int pos;

  private final int max;

  private final ArrayList<Token> tokens = new ArrayList<>();

  public Tokenizer(String inputData) {
    this.expressionString = inputData;
    this.charsToProcess = (inputData + "\0").toCharArray();
    this.max = charsToProcess.length;
    this.pos = 0;
  }

  public List<Token> process() {
    char[] charsToProcess = this.charsToProcess;
    while (pos < max) {
      char ch = charsToProcess[pos];
      if (isAlphabetic(ch)) {
        lexIdentifier();
      }
      else {
        switch (ch) {
          case '+' -> {
            if (isTwoCharToken(TokenKind.INC)) {
              pushPairToken(TokenKind.INC);
            }
            else {
              pushCharToken(TokenKind.PLUS);
            }
          }
          case '_' -> lexIdentifier(); // the other way to start an identifier
          case '-' -> {
            if (isTwoCharToken(TokenKind.DEC)) {
              pushPairToken(TokenKind.DEC);
            }
            else {
              pushCharToken(TokenKind.MINUS);
            }
          }
          case ':' -> pushCharToken(TokenKind.COLON);
          case '.' -> pushCharToken(TokenKind.DOT);
          case ',' -> pushCharToken(TokenKind.COMMA);
          case '*' -> pushCharToken(TokenKind.STAR);
          case '/' -> pushCharToken(TokenKind.DIV);
          case '%' -> pushCharToken(TokenKind.MOD);
          case '(' -> pushCharToken(TokenKind.LPAREN);
          case ')' -> pushCharToken(TokenKind.RPAREN);
          case '[' -> pushCharToken(TokenKind.LSQUARE);
          case '#' -> pushCharToken(TokenKind.HASH);
          case ']' -> pushCharToken(TokenKind.RSQUARE);
          case '{' -> pushCharToken(TokenKind.LCURLY);
          case '}' -> pushCharToken(TokenKind.RCURLY);
          case '@' -> pushCharToken(TokenKind.BEAN_REF);
          case '^' -> {
            if (isTwoCharToken(TokenKind.SELECT_FIRST)) {
              pushPairToken(TokenKind.SELECT_FIRST);
            }
            else {
              pushCharToken(TokenKind.POWER);
            }
          }
          case '!' -> {
            if (isTwoCharToken(TokenKind.NE)) {
              pushPairToken(TokenKind.NE);
            }
            else if (isTwoCharToken(TokenKind.PROJECT)) {
              pushPairToken(TokenKind.PROJECT);
            }
            else {
              pushCharToken(TokenKind.NOT);
            }
          }
          case '=' -> {
            if (isTwoCharToken(TokenKind.EQ)) {
              pushPairToken(TokenKind.EQ);
            }
            else {
              pushCharToken(TokenKind.ASSIGN);
            }
          }
          case '&' -> {
            if (isTwoCharToken(TokenKind.SYMBOLIC_AND)) {
              pushPairToken(TokenKind.SYMBOLIC_AND);
            }
            else {
              pushCharToken(TokenKind.FACTORY_BEAN_REF);
            }
          }
          case '|' -> {
            if (!isTwoCharToken(TokenKind.SYMBOLIC_OR)) {
              raiseParseException(pos, SpelMessage.MISSING_CHARACTER, "|");
            }
            pushPairToken(TokenKind.SYMBOLIC_OR);
          }
          case '?' -> {
            if (isTwoCharToken(TokenKind.SELECT)) {
              pushPairToken(TokenKind.SELECT);
            }
            else if (isTwoCharToken(TokenKind.ELVIS)) {
              pushPairToken(TokenKind.ELVIS);
            }
            else if (isTwoCharToken(TokenKind.SAFE_NAVI)) {
              pushPairToken(TokenKind.SAFE_NAVI);
            }
            else {
              pushCharToken(TokenKind.QMARK);
            }
          }
          case '$' -> {
            if (isTwoCharToken(TokenKind.SELECT_LAST)) {
              pushPairToken(TokenKind.SELECT_LAST);
            }
            else {
              lexIdentifier();
            }
          }
          case '>' -> {
            if (isTwoCharToken(TokenKind.GE)) {
              pushPairToken(TokenKind.GE);
            }
            else {
              pushCharToken(TokenKind.GT);
            }
          }
          case '<' -> {
            if (isTwoCharToken(TokenKind.LE)) {
              pushPairToken(TokenKind.LE);
            }
            else {
              pushCharToken(TokenKind.LT);
            }
          }
          case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> lexNumericLiteral(ch == '0');
          case ' ', '\t', '\r', '\n' -> this.pos++; // drift over white space
          case '\'' -> lexQuotedStringLiteral();
          case '"' -> lexDoubleQuotedStringLiteral();
          // hit sentinel at end of value
          case 0 -> this.pos++;  // will take us to the end
          case '\\' -> raiseParseException(pos, SpelMessage.UNEXPECTED_ESCAPE_CHAR);
          default -> raiseParseException(this.pos + 1, SpelMessage.UNSUPPORTED_CHARACTER, ch, (int) ch);
        }
      }
    }
    return this.tokens;
  }

  // STRING_LITERAL: '\''! (APOS|~'\'')* '\''!;
  private void lexQuotedStringLiteral() {
    char[] charsToProcess = this.charsToProcess;

    int start = this.pos;
    boolean terminated = false;
    while (!terminated) {
      this.pos++;

      char ch = charsToProcess[this.pos];
      if (ch == '\'') {
        // may not be the end if the char after is also a '
        if (charsToProcess[this.pos + 1] == '\'') {
          this.pos++;  // skip over that too, and continue
        }
        else {
          terminated = true;
        }
      }
      if (isExhausted()) {
        raiseParseException(start, SpelMessage.NON_TERMINATING_QUOTED_STRING);
      }
    }
    this.pos++;
    this.tokens.add(new Token(TokenKind.LITERAL_STRING, subarray(start, this.pos), start, this.pos));
  }

  // DQ_STRING_LITERAL: '"'! (~'"')* '"'!;
  private void lexDoubleQuotedStringLiteral() {
    char[] charsToProcess = this.charsToProcess;

    int start = this.pos;
    boolean terminated = false;
    while (!terminated) {
      this.pos++;
      char ch = charsToProcess[this.pos];
      if (ch == '"') {
        // may not be the end if the char after is also a "
        if (charsToProcess[this.pos + 1] == '"') {
          this.pos++;  // skip over that too, and continue
        }
        else {
          terminated = true;
        }
      }
      if (isExhausted()) {
        raiseParseException(start, SpelMessage.NON_TERMINATING_DOUBLE_QUOTED_STRING);
      }
    }
    this.pos++;
    this.tokens.add(new Token(TokenKind.LITERAL_STRING, subarray(start, this.pos), start, this.pos));
  }

  // REAL_LITERAL :
  // ('.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?) |
  // ((DECIMAL_DIGIT)+ '.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?) |
  // ((DECIMAL_DIGIT)+ (EXPONENT_PART) (REAL_TYPE_SUFFIX)?) |
  // ((DECIMAL_DIGIT)+ (REAL_TYPE_SUFFIX));
  // fragment INTEGER_TYPE_SUFFIX : ( 'L' | 'l' );
  // fragment HEX_DIGIT :
  // '0'|'1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9'|'A'|'B'|'C'|'D'|'E'|'F'|'a'|'b'|'c'|'d'|'e'|'f';
  //
  // fragment EXPONENT_PART : 'e' (SIGN)* (DECIMAL_DIGIT)+ | 'E' (SIGN)*
  // (DECIMAL_DIGIT)+ ;
  // fragment SIGN : '+' | '-' ;
  // fragment REAL_TYPE_SUFFIX : 'F' | 'f' | 'D' | 'd';
  // INTEGER_LITERAL
  // : (DECIMAL_DIGIT)+ (INTEGER_TYPE_SUFFIX)?;

  private void lexNumericLiteral(boolean firstCharIsZero) {
    final char[] charsToProcess = this.charsToProcess;

    boolean isReal = false;
    int start = this.pos;
    char ch = charsToProcess[this.pos + 1];
    boolean isHex = ch == 'x' || ch == 'X';

    // deal with hexadecimal
    if (firstCharIsZero && isHex) {
      this.pos = this.pos + 1;
      do {
        this.pos++;
      }
      while (isHexadecimalDigit(charsToProcess[this.pos]));
      if (isChar('L', 'l')) {
        pushHexIntToken(subarray(start + 2, this.pos), true, start, this.pos);
        this.pos++;
      }
      else {
        pushHexIntToken(subarray(start + 2, this.pos), false, start, this.pos);
      }
      return;
    }

    // real numbers must have leading digits

    // Consume first part of number
    do {
      this.pos++;
    }
    while (isDigit(charsToProcess[this.pos]));

    // a '.' indicates this number is a real
    ch = charsToProcess[this.pos];
    if (ch == '.') {
      isReal = true;
      int dotpos = this.pos;
      // carry on consuming digits
      do {
        this.pos++;
      }
      while (isDigit(charsToProcess[this.pos]));
      if (this.pos == dotpos + 1) {
        // the number is something like '3.'. It is really an int but may be
        // part of something like '3.toString()'. In this case process it as
        // an int and leave the dot as a separate token.
        this.pos = dotpos;
        pushIntToken(subarray(start, this.pos), false, start, this.pos);
        return;
      }
    }

    int endOfNumber = this.pos;

    // Now there may or may not be an exponent

    // Is it a long ?
    if (isChar('L', 'l')) {
      if (isReal) {  // 3.4L - not allowed
        raiseParseException(start, SpelMessage.REAL_CANNOT_BE_LONG);
      }
      pushIntToken(subarray(start, endOfNumber), true, start, endOfNumber);
      this.pos++;
    }
    else if (isExponentChar(charsToProcess[this.pos])) {
      isReal = true;  // if it wasn't before, it is now
      this.pos++;
      char possibleSign = charsToProcess[this.pos];
      if (isSign(possibleSign)) {
        this.pos++;
      }

      // exponent digits
      do {
        this.pos++;
      }
      while (isDigit(charsToProcess[this.pos]));
      boolean isFloat = false;
      if (isFloatSuffix(charsToProcess[this.pos])) {
        isFloat = true;
        endOfNumber = ++this.pos;
      }
      else if (isDoubleSuffix(charsToProcess[this.pos])) {
        endOfNumber = ++this.pos;
      }
      pushRealToken(subarray(start, this.pos), isFloat, start, this.pos);
    }
    else {
      ch = charsToProcess[this.pos];
      boolean isFloat = false;
      if (isFloatSuffix(ch)) {
        isReal = true;
        isFloat = true;
        endOfNumber = ++this.pos;
      }
      else if (isDoubleSuffix(ch)) {
        isReal = true;
        endOfNumber = ++this.pos;
      }
      if (isReal) {
        pushRealToken(subarray(start, endOfNumber), isFloat, start, endOfNumber);
      }
      else {
        pushIntToken(subarray(start, endOfNumber), false, start, endOfNumber);
      }
    }
  }

  private void lexIdentifier() {
    char[] charsToProcess = this.charsToProcess;

    int start = this.pos;
    do {
      this.pos++;
    }
    while (isIdentifier(charsToProcess[this.pos]));
    char[] subarray = subarray(start, this.pos);

    // Check if this is the alternative (textual) representation of an operator (see
    // alternativeOperatorNames)
    if (subarray.length == 2 || subarray.length == 3) {
      String asString = new String(subarray).toUpperCase(Locale.ROOT);
      int idx = Arrays.binarySearch(ALTERNATIVE_OPERATOR_NAMES, asString);
      if (idx >= 0) {
        pushOneCharOrTwoCharToken(TokenKind.valueOf(asString), start, subarray);
        return;
      }
    }
    this.tokens.add(new Token(TokenKind.IDENTIFIER, subarray, start, this.pos));
  }

  private void pushIntToken(char[] data, boolean isLong, int start, int end) {
    if (isLong) {
      this.tokens.add(new Token(TokenKind.LITERAL_LONG, data, start, end));
    }
    else {
      this.tokens.add(new Token(TokenKind.LITERAL_INT, data, start, end));
    }
  }

  private void pushHexIntToken(char[] data, boolean isLong, int start, int end) {
    if (data.length == 0) {
      if (isLong) {
        raiseParseException(start, SpelMessage.NOT_A_LONG, this.expressionString.substring(start, end + 1));
      }
      else {
        raiseParseException(start, SpelMessage.NOT_AN_INTEGER, this.expressionString.substring(start, end));
      }
    }
    if (isLong) {
      this.tokens.add(new Token(TokenKind.LITERAL_HEXLONG, data, start, end));
    }
    else {
      this.tokens.add(new Token(TokenKind.LITERAL_HEXINT, data, start, end));
    }
  }

  private void pushRealToken(char[] data, boolean isFloat, int start, int end) {
    if (isFloat) {
      this.tokens.add(new Token(TokenKind.LITERAL_REAL_FLOAT, data, start, end));
    }
    else {
      this.tokens.add(new Token(TokenKind.LITERAL_REAL, data, start, end));
    }
  }

  private char[] subarray(int start, int end) {
    return Arrays.copyOfRange(this.charsToProcess, start, end);
  }

  /**
   * Check if this might be a two character token.
   */
  private boolean isTwoCharToken(TokenKind kind) {
    return (kind.tokenChars.length == 2 &&
            this.charsToProcess[this.pos] == kind.tokenChars[0] &&
            this.charsToProcess[this.pos + 1] == kind.tokenChars[1]);
  }

  /**
   * Push a token of just one character in length.
   */
  private void pushCharToken(TokenKind kind) {
    this.tokens.add(new Token(kind, this.pos, this.pos + 1));
    this.pos++;
  }

  /**
   * Push a token of two characters in length.
   */
  private void pushPairToken(TokenKind kind) {
    this.tokens.add(new Token(kind, this.pos, this.pos + 2));
    this.pos += 2;
  }

  private void pushOneCharOrTwoCharToken(TokenKind kind, int pos, char[] data) {
    this.tokens.add(new Token(kind, data, pos, pos + kind.getLength()));
  }

  // ID: ('a'..'z'|'A'..'Z'|'_'|'$') ('a'..'z'|'A'..'Z'|'_'|'$'|'0'..'9'|DOT_ESCAPED)*;
  private boolean isIdentifier(char ch) {
    return isAlphabetic(ch) || isDigit(ch) || ch == '_' || ch == '$';
  }

  private boolean isChar(char a, char b) {
    char ch = this.charsToProcess[this.pos];
    return ch == a || ch == b;
  }

  private boolean isExponentChar(char ch) {
    return ch == 'e' || ch == 'E';
  }

  private boolean isFloatSuffix(char ch) {
    return ch == 'f' || ch == 'F';
  }

  private boolean isDoubleSuffix(char ch) {
    return ch == 'd' || ch == 'D';
  }

  private boolean isSign(char ch) {
    return ch == '+' || ch == '-';
  }

  private boolean isDigit(char ch) {
    if (ch > 255) {
      return false;
    }
    return (FLAGS[ch] & IS_DIGIT) != 0;
  }

  private boolean isAlphabetic(char ch) {
    return Character.isLetter(ch);
  }

  private boolean isHexadecimalDigit(char ch) {
    if (ch > 255) {
      return false;
    }
    return (FLAGS[ch] & IS_HEXDIGIT) != 0;
  }

  private boolean isExhausted() {
    return (this.pos == this.max - 1);
  }

  private void raiseParseException(int start, SpelMessage msg, Object... inserts) {
    throw new InternalParseException(new SpelParseException(this.expressionString, start, msg, inserts));
  }

}
