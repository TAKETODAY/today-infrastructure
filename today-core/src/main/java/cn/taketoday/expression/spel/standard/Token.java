/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.expression.spel.standard;

import cn.taketoday.lang.Nullable;

/**
 * Holder for a kind of token, the associated data and its position in the input data
 * stream (start/end).
 *
 * @author Andy Clement
 * @since 4.0
 */
class Token {

  public TokenKind kind;

  @Nullable
  public String data;

  public int startPos;  // index of first character

  public int endPos;  // index of char after the last character

  /**
   * Constructor for use when there is no particular data for the token
   * (e.g. TRUE or '+')
   *
   * @param startPos the exact start
   * @param endPos the index to the last character
   */
  public Token(TokenKind tokenKind, int startPos, int endPos) {
    this.kind = tokenKind;
    this.startPos = startPos;
    this.endPos = endPos;
  }

  public Token(TokenKind tokenKind, char[] tokenData, int startPos, int endPos) {
    this(tokenKind, startPos, endPos);
    this.data = new String(tokenData);
  }

  public boolean isIdentifier() {
    return (this.kind == TokenKind.IDENTIFIER);
  }

  public boolean isNumericRelationalOperator() {
    return (this.kind == TokenKind.GT || this.kind == TokenKind.GE || this.kind == TokenKind.LT ||
            this.kind == TokenKind.LE || this.kind == TokenKind.EQ || this.kind == TokenKind.NE);
  }

  public String stringValue() {
    return (this.data != null ? this.data : "");
  }

  public Token asInstanceOfToken() {
    return new Token(TokenKind.INSTANCEOF, this.startPos, this.endPos);
  }

  public Token asMatchesToken() {
    return new Token(TokenKind.MATCHES, this.startPos, this.endPos);
  }

  public Token asBetweenToken() {
    return new Token(TokenKind.BETWEEN, this.startPos, this.endPos);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('[').append(this.kind);
    if (this.kind.hasPayload()) {
      sb.append(':').append(this.data);
    }
    sb.append(']');
    sb.append('(').append(this.startPos).append(',').append(this.endPos).append(')');
    return sb.toString();
  }

}
