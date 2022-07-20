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

package cn.taketoday.expression.spel.ast;

import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.core.bytecode.core.CodeFlow;
import cn.taketoday.lang.Assert;

/**
 * Expression language AST node that represents an integer literal.
 *
 * @author Andy Clement
 * @since 4.0
 */
public class IntLiteral extends Literal {

  private final TypedValue value;

  public IntLiteral(String payload, int startPos, int endPos, int value) {
    super(payload, startPos, endPos);
    this.value = new TypedValue(value);
    this.exitTypeDescriptor = "I";
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
    Integer intValue = (Integer) this.value.getValue();
    Assert.state(intValue != null, "No int value");
    if (intValue == -1) {
      // Not sure we can get here because -1 is OpMinus
      mv.visitInsn(ICONST_M1);
    }
    else if (intValue >= 0 && intValue < 6) {
      mv.visitInsn(ICONST_0 + intValue);
    }
    else {
      mv.visitLdcInsn(intValue);
    }
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
