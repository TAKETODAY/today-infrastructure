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

import infra.expression.EvaluationContext;
import infra.expression.EvaluationException;
import infra.expression.common.LiteralExpression;
import infra.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Andy Clement
 */
public class LiteralExpressionTests {

  @Test
  public void testGetValue() throws Exception {
    LiteralExpression lEx = new LiteralExpression("somevalue");
    assertThat(lEx.getValue()).isInstanceOf(String.class).isEqualTo("somevalue");
    assertThat(lEx.getValue(String.class)).isInstanceOf(String.class).isEqualTo("somevalue");
    EvaluationContext ctx = new StandardEvaluationContext();
    assertThat(lEx.getValue(ctx)).isInstanceOf(String.class).isEqualTo("somevalue");
    assertThat(lEx.getValue(ctx, String.class)).isInstanceOf(String.class).isEqualTo("somevalue");
    assertThat(lEx.getValue(new Rooty())).isInstanceOf(String.class).isEqualTo("somevalue");
    assertThat(lEx.getValue(new Rooty(), String.class)).isInstanceOf(String.class).isEqualTo("somevalue");
    assertThat(lEx.getValue(ctx, new Rooty())).isInstanceOf(String.class).isEqualTo("somevalue");
    assertThat(lEx.getValue(ctx, new Rooty(), String.class)).isInstanceOf(String.class).isEqualTo("somevalue");
    assertThat(lEx.getExpressionString()).isEqualTo("somevalue");
    assertThat(lEx.isWritable(new StandardEvaluationContext())).isFalse();
    assertThat(lEx.isWritable(new Rooty())).isFalse();
    assertThat(lEx.isWritable(new StandardEvaluationContext(), new Rooty())).isFalse();
  }

  static class Rooty { }

  @Test
  public void testSetValue() {
    assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
                    new LiteralExpression("somevalue").setValue(new StandardEvaluationContext(), "flibble"))
            .satisfies(ex -> assertThat(ex.getExpressionString()).isEqualTo("somevalue"));
    assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
                    new LiteralExpression("somevalue").setValue(new Rooty(), "flibble"))
            .satisfies(ex -> assertThat(ex.getExpressionString()).isEqualTo("somevalue"));
    assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
                    new LiteralExpression("somevalue").setValue(new StandardEvaluationContext(), new Rooty(), "flibble"))
            .satisfies(ex -> assertThat(ex.getExpressionString()).isEqualTo("somevalue"));
  }

  @Test
  public void testGetValueType() throws Exception {
    LiteralExpression lEx = new LiteralExpression("somevalue");
    assertThat(lEx.getValueType()).isEqualTo(String.class);
    assertThat(lEx.getValueType(new StandardEvaluationContext())).isEqualTo(String.class);
    assertThat(lEx.getValueType(new Rooty())).isEqualTo(String.class);
    assertThat(lEx.getValueType(new StandardEvaluationContext(), new Rooty())).isEqualTo(String.class);
    assertThat(lEx.getValueTypeDescriptor().getType()).isEqualTo(String.class);
    assertThat(lEx.getValueTypeDescriptor(new StandardEvaluationContext()).getType()).isEqualTo(String.class);
    assertThat(lEx.getValueTypeDescriptor(new Rooty()).getType()).isEqualTo(String.class);
    assertThat(lEx.getValueTypeDescriptor(new StandardEvaluationContext(), new Rooty()).getType()).isEqualTo(String.class);
  }

}
