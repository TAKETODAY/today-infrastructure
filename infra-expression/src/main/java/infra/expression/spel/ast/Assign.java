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

import infra.expression.EvaluationException;
import infra.expression.TypedValue;
import infra.expression.spel.ExpressionState;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.SpelMessage;

/**
 * Represents assignment. An alternative to calling {@code setValue}
 * for an expression which indicates an assign statement.
 *
 * <p>Example: 'someNumberProperty=42'
 *
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Assign extends SpelNodeImpl {

  public Assign(int startPos, int endPos, SpelNodeImpl... operands) {
    super(startPos, endPos, operands);
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    if (!state.getEvaluationContext().isAssignmentEnabled()) {
      throw new SpelEvaluationException(getStartPosition(), SpelMessage.NOT_ASSIGNABLE, toStringAST());
    }
    return this.children[0].setValueInternal(state, () -> this.children[1].getValueInternal(state));
  }

  @Override
  public String toStringAST() {
    return getChild(0).toStringAST() + "=" + getChild(1).toStringAST();
  }

}
