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

package infra.expression.spel.ast;

import infra.bytecode.MethodVisitor;
import infra.bytecode.core.CodeFlow;
import infra.expression.TypedValue;
import infra.util.StringUtils;

/**
 * Expression language AST node that represents a string literal.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 4.0
 */
public class StringLiteral extends Literal {

  private final TypedValue value;

  public StringLiteral(String payload, int startPos, int endPos, String value) {
    super(payload, startPos, endPos);

    // The original enclosing quote character for the string literal: ' or ".
    char quoteCharacter = value.charAt(0);

    // Remove enclosing quotes
    String valueWithinQuotes = value.substring(1, value.length() - 1);

    // Replace escaped internal quote characters
    if (quoteCharacter == '\'') {
      valueWithinQuotes = StringUtils.replace(valueWithinQuotes, "''", "'");
    }
    else {
      valueWithinQuotes = StringUtils.replace(valueWithinQuotes, "\"\"", "\"");
    }

    this.value = new TypedValue(valueWithinQuotes);
    this.exitTypeDescriptor = "Ljava/lang/String";
  }

  @Override
  public TypedValue getLiteralValue() {
    return this.value;
  }

  @Override
  public String toString() {
    String ast = String.valueOf(getLiteralValue().getValue());
    ast = StringUtils.replace(ast, "'", "''");
    return "'" + ast + "'";
  }

  @Override
  public boolean isCompilable() {
    return true;
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    mv.visitLdcInsn(this.value.getValue());
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
