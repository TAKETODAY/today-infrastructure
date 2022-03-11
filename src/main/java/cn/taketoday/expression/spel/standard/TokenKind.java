/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

/**
 * Token Kinds.
 *
 * @author Andy Clement
 * @since 4.0
 */
enum TokenKind {

  // ordered by priority - operands first

  LITERAL_INT,

  LITERAL_LONG,

  LITERAL_HEXINT,

  LITERAL_HEXLONG,

  LITERAL_STRING,

  LITERAL_REAL,

  LITERAL_REAL_FLOAT,

  LPAREN("("),

  RPAREN(")"),

  COMMA(","),

  IDENTIFIER,

  COLON(":"),

  HASH("#"),

  RSQUARE("]"),

  LSQUARE("["),

  LCURLY("{"),

  RCURLY("}"),

  DOT("."),

  PLUS("+"),

  STAR("*"),

  MINUS("-"),

  SELECT_FIRST("^["),

  SELECT_LAST("$["),

  QMARK("?"),

  PROJECT("!["),

  DIV("/"),

  GE(">="),

  GT(">"),

  LE("<="),

  LT("<"),

  EQ("=="),

  NE("!="),

  MOD("%"),

  NOT("!"),

  ASSIGN("="),

  INSTANCEOF("instanceof"),

  MATCHES("matches"),

  BETWEEN("between"),

  SELECT("?["),

  POWER("^"),

  ELVIS("?:"),

  SAFE_NAVI("?."),

  BEAN_REF("@"),

  FACTORY_BEAN_REF("&"),

  SYMBOLIC_OR("||"),

  SYMBOLIC_AND("&&"),

  INC("++"),

  DEC("--");

  final char[] tokenChars;

  private final boolean hasPayload;  // is there more to this token than simply the kind

  TokenKind(String tokenString) {
    this.tokenChars = tokenString.toCharArray();
    this.hasPayload = (this.tokenChars.length == 0);
  }

  TokenKind() {
    this("");
  }

  @Override
  public String toString() {
    return (name() + (this.tokenChars.length != 0 ? "(" + new String(this.tokenChars) + ")" : ""));
  }

  public boolean hasPayload() {
    return this.hasPayload;
  }

  public int getLength() {
    return this.tokenChars.length;
  }

}
