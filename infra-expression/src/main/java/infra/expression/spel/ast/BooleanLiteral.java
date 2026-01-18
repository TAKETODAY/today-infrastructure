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
import infra.expression.spel.support.BooleanTypedValue;

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
