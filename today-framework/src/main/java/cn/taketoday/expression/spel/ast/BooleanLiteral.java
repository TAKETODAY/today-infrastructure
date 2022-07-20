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
import cn.taketoday.core.bytecode.core.CodeFlow;
import cn.taketoday.expression.spel.support.BooleanTypedValue;

/**
 * Represents the literal values {@code TRUE} and {@code FALSE}.
 *
 * @author Andy Clement
 * @since 4.0
 */
public class BooleanLiteral extends Literal {

  private final BooleanTypedValue value;

  public BooleanLiteral(String payload, int startPos, int endPos, boolean value) {
    super(payload, startPos, endPos);
    this.value = BooleanTypedValue.forValue(value);
    this.exitTypeDescriptor = "Z";
  }

  @Override
  public BooleanTypedValue getLiteralValue() {
    return this.value;
  }

  @Override
  public boolean isCompilable() {
    return true;
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    if (this.value == BooleanTypedValue.TRUE) {
      mv.visitLdcInsn(1);
    }
    else {
      mv.visitLdcInsn(0);
    }
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
