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

/**
 * Expression language AST node that represents a real literal.
 *
 * @author Andy Clement
 * @since 4.0
 */
public class RealLiteral extends Literal {

  private final TypedValue value;

  public RealLiteral(String payload, int startPos, int endPos, double value) {
    super(payload, startPos, endPos);
    this.value = new TypedValue(value);
    this.exitTypeDescriptor = "D";
  }

  @Override
  public TypedValue getLiteralValue() {
    return this.value;
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
