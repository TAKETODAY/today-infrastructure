/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.persistence.query;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.persistence.IllegalEntityException;
import infra.persistence.sql.LogicalOperator;

/**
 * Parses a {@link infra.persistence.annotation.GroupExpression @GroupExpression}
 * string into an AST of {@link Node nodes}.
 *
 * <p>The expression is a boolean formula over property names joined with
 * {@code NOT}, {@code AND} / {@code &&}, {@code OR} / {@code ||} and
 * {@code XOR} (case insensitive for words), and parenthesized for grouping.
 * Precedence follows SQL: {@code NOT} &gt; {@code AND} &gt; {@code XOR}
 * &gt; {@code OR}.
 *
 * <p>The result is a tree of {@link Group groups} whose children are either
 * further groups or {@link Literal literal} property references. Every {@link
 * Group} holds a single shared connector and an explicit {@code parenthesized}
 * flag that records whether the source text wrapped it in parentheses.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.persistence.annotation.GroupExpression
 * @see Node
 * @since 5.0
 */
final class GroupExpressionParser {

  private final String expression;

  private int pos;

  private GroupExpressionParser(String expression) {
    this.expression = expression;
  }

  /**
   * Parse the given expression into an AST.
   *
   * @param expression the expression text, never {@code null}
   * @return the root node of the parsed tree
   * @throws IllegalEntityException if the expression cannot be parsed
   */
  static Node parse(String expression) {
    GroupExpressionParser parser = new GroupExpressionParser(expression);
    Node node = parser.parseExpression();
    parser.skipWhitespace();
    if (!parser.atEnd()) {
      throw parser.error("Unexpected token");
    }
    return node;
  }

  // expr := or
  // or   := xor ( OR xor )*
  // xor  := and ( XOR and )*
  // and  := not ( AND not )*
  // not  := NOT* primary
  // primary := '(' expr ')' | IDENTIFIER

  private Node parseExpression() {
    return parseOr();
  }

  private Node parseOr() {
    Node left = parseXor();
    List<Node> nodes = null;
    while (matchWord("OR") || match('|', '|')) {
      if (nodes == null) {
        nodes = new ArrayList<>();
        nodes.add(left);
      }
      nodes.add(parseXor());
    }
    if (nodes == null) {
      return left;
    }
    return new Group(LogicalOperator.OR, nodes, false);
  }

  private Node parseXor() {
    Node left = parseAnd();
    List<Node> nodes = null;
    while (matchWord("XOR")) {
      if (nodes == null) {
        nodes = new ArrayList<>();
        nodes.add(left);
      }
      nodes.add(parseAnd());
    }
    if (nodes == null) {
      return left;
    }
    return new Group(LogicalOperator.XOR, nodes, false);
  }

  private Node parseAnd() {
    Node left = parseNot();
    List<Node> nodes = null;
    while (matchWord("AND") || match('&', '&')) {
      if (nodes == null) {
        nodes = new ArrayList<>();
        nodes.add(left);
      }
      nodes.add(parseNot());
    }
    if (nodes == null) {
      return left;
    }
    return new Group(LogicalOperator.AND, nodes, false);
  }

  private Node parseNot() {
    if (matchWord("NOT")) {
      return new Not(parseNot());
    }
    return parsePrimary();
  }

  private Node parsePrimary() {
    skipWhitespace();
    if (consume('(')) {
      Node inner = parseExpression();
      skipWhitespace();
      if (!consume(')')) {
        throw error("Expected ')'");
      }
      if (inner instanceof Group group) {
        return new Group(group.connector(), group.children(), true);
      }
      return new Group(LogicalOperator.AND, List.of(inner), true);
    }
    String name = parseIdentifier();
    if (name == null) {
      throw error("Expected a property name");
    }
    return new Literal(name);
  }

  private @Nullable String parseIdentifier() {
    skipWhitespace();
    if (atEnd()) {
      return null;
    }
    int start = pos;
    while (!atEnd()) {
      char c = expression.charAt(pos);
      if (!Character.isJavaIdentifierPart(c)) {
        break;
      }
      pos++;
    }
    if (pos == start) {
      return null;
    }
    return expression.substring(start, pos);
  }

  private boolean matchWord(String word) {
    skipWhitespace();
    if (expression.regionMatches(true, pos, word, 0, word.length())) {
      int end = pos + word.length();
      if (end == expression.length() || !isIdentifierPart(expression.charAt(end))) {
        pos = end;
        return true;
      }
    }
    return false;
  }

  private boolean isIdentifierPart(char c) {
    return Character.isJavaIdentifierPart(c);
  }

  private boolean consume(char expected) {
    skipWhitespace();
    if (!atEnd() && expression.charAt(pos) == expected) {
      pos++;
      return true;
    }
    return false;
  }

  private boolean match(char first, char second) {
    skipWhitespace();
    if (pos + 1 < expression.length()
            && expression.charAt(pos) == first
            && expression.charAt(pos + 1) == second) {
      pos += 2;
      return true;
    }
    return false;
  }

  private void skipWhitespace() {
    while (!atEnd() && Character.isWhitespace(expression.charAt(pos))) {
      pos++;
    }
  }

  private boolean atEnd() {
    return pos >= expression.length();
  }

  private IllegalEntityException error(String message) {
    return new IllegalEntityException(
            "Invalid @GroupExpression \"" + expression + "\" at position " + pos
                    + ": " + message + "\n" + caretLine());
  }

  private String caretLine() {
    int lineStart = expression.lastIndexOf('\n', pos - 1) + 1;
    int lineEnd = expression.indexOf('\n', pos);
    if (lineEnd == -1) {
      lineEnd = expression.length();
    }
    String line = expression.substring(lineStart, lineEnd);
    return "    " + line + "\n    " + " ".repeat(Math.max(0, pos - lineStart)) + "^";
  }

  /**
   * An element of a parsed {@code @GroupExpression}: either a property
   * reference or a nested, parenthesized group.
   *
   * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
   * @since 5.0
   */
  sealed interface Node permits Literal, Group, Not {
  }

  /**
   * A leaf of the AST referring to a single example property by name.
   *
   * @param name the property name
   * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
   * @since 5.0
   */
  record Literal(String name) implements Node {
  }

  /**
   * A unary {@code NOT} applied to a nested node.
   *
   * @param operand the node to negate
   * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
   * @since 5.0
   */
  record Not(Node operand) implements Node {
  }

  /**
   * An internal AST node: a group of child nodes joined by one connector.
   *
   * <p>The {@code parenthesized} flag records whether the source text wrapped
   * the group in parentheses; {@link Group#connector()} is the shared
   * connector joining the children.
   *
   * @param connector the connector joining the children
   * @param children the ordered children, never empty
   * @param parenthesized whether the source text parenthesized the group
   * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
   * @since 5.0
   */
  record Group(LogicalOperator connector, List<Node> children, boolean parenthesized) implements Node {

    Group {
      children = List.copyOf(children);
    }
  }

}
