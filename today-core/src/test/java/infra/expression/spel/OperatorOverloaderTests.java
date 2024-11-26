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

package infra.expression.spel;

import org.junit.jupiter.api.Test;

import infra.expression.EvaluationException;
import infra.expression.Operation;
import infra.expression.OperatorOverloader;
import infra.expression.spel.SpelMessage;
import infra.expression.spel.standard.SpelExpression;
import infra.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test providing operator support
 *
 * @author Andy Clement
 */
public class OperatorOverloaderTests extends AbstractExpressionTests {

  @Test
  public void testSimpleOperations() throws Exception {
    // no built in support for this:
    evaluateAndCheckError("'abc'-true", SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);

    StandardEvaluationContext eContext = TestScenarioCreator.getTestEvaluationContext();
    eContext.setOperatorOverloader(new StringAndBooleanAddition());

    SpelExpression expr = (SpelExpression) parser.parseExpression("'abc'+true");
    assertThat(expr.getValue(eContext)).isEqualTo("abctrue");

    expr = (SpelExpression) parser.parseExpression("'abc'-true");
    assertThat(expr.getValue(eContext)).isEqualTo("abc");

    expr = (SpelExpression) parser.parseExpression("'abc'+null");
    assertThat(expr.getValue(eContext)).isEqualTo("abcnull");
  }

  static class StringAndBooleanAddition implements OperatorOverloader {

    @Override
    public Object operate(Operation operation, Object leftOperand, Object rightOperand) throws EvaluationException {
      if (operation == Operation.ADD) {
        return leftOperand + ((Boolean) rightOperand).toString();
      }
      else {
        return leftOperand;
      }
    }

    @Override
    public boolean overridesOperation(Operation operation, Object leftOperand, Object rightOperand) throws EvaluationException {
			return leftOperand instanceof String && rightOperand instanceof Boolean;

		}
  }

}
