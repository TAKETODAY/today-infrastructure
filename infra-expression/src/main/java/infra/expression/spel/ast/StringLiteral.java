/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.expression.spel.ast;

import infra.bytecode.MethodVisitor;
import infra.expression.spel.CodeFlow;
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
