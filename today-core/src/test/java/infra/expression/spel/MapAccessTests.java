/*
 * Copyright 2017 - 2025 the original author or authors.
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

import java.util.Map;

import infra.expression.EvaluationContext;
import infra.expression.TypedValue;
import infra.expression.spel.standard.SpelExpressionParser;
import infra.expression.spel.support.MapAccessor;

import static infra.expression.spel.SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Testing variations on map access.
 *
 * @author Andy Clement
 */
public class MapAccessTests extends AbstractExpressionTests {

  @Test
  void directMapAccess() {
    evaluate("testMap.get('monday')", "montag", String.class);
  }

  @Test
  void mapAccessThroughIndexer() {
    evaluate("testMap['monday']", "montag", String.class);
  }

  @Test
  void mapAccessThroughIndexerForNonexistentKey() {
    evaluate("testMap['bogus']", null, String.class);
  }

  @Test
  void variableMapAccess() {
    var parser = new SpelExpressionParser();
    var ctx = TestScenarioCreator.getTestEvaluationContext();
    ctx.setVariable("day", "saturday");

    var expr = parser.parseExpression("testMap[#day]");
    assertThat(expr.getValue(ctx, String.class)).isEqualTo("samstag");
  }

  @Test
  void mapAccessOnRoot() {
    var map = Map.of("key", "value");
    var parser = new SpelExpressionParser();
    var expr = parser.parseExpression("#root['key']");

    assertThat(expr.getValue(map)).isEqualTo("value");
  }

  @Test
  void mapAccessOnProperty() {
    var properties = Map.of("key", "value");
    var bean = new TestBean(null, new TestBean(properties, null));

    var parser = new SpelExpressionParser();
    var expr = parser.parseExpression("nestedBean.properties['key']");
    assertThat(expr.getValue(bean)).isEqualTo("value");
  }

  @Test
  void mapAccessor() {
    var parser = new SpelExpressionParser();
    var ctx = TestScenarioCreator.getTestEvaluationContext();
    ctx.addPropertyAccessor(new MapAccessor());

    var expr1 = parser.parseExpression("testMap.monday");
    assertThat(expr1.getValue(ctx, String.class)).isEqualTo("montag");

    var expr2 = parser.parseExpression("testMap.bogus");
    assertThatExceptionOfType(SpelEvaluationException.class)
            .isThrownBy(() -> expr2.getValue(ctx, String.class))
            .satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(PROPERTY_OR_FIELD_NOT_READABLE));
  }

  @Test
  void nullAwareMapAccessor() {
    var parser = new SpelExpressionParser();
    var ctx = TestScenarioCreator.getTestEvaluationContext();
    ctx.addPropertyAccessor(new NullAwareMapAccessor());

    var expr = parser.parseExpression("testMap.monday");
    assertThat(expr.getValue(ctx, String.class)).isEqualTo("montag");

    // Unlike MapAccessor, NullAwareMapAccessor returns null for a nonexistent key.
    expr = parser.parseExpression("testMap.bogus");
    assertThat(expr.getValue(ctx, String.class)).isNull();
  }

  record TestBean(Map<String, String> properties, TestBean nestedBean) {
  }

  /**
   * In contrast to the standard {@link MapAccessor}, {@code NullAwareMapAccessor}
   * reports that it can read any map (ignoring whether the map actually contains
   * an entry for the given key) and returns {@code null} for a nonexistent key.
   */
  private static class NullAwareMapAccessor extends MapAccessor {

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) {
      return (target instanceof Map);
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) {
      return new TypedValue(((Map<?, ?>) target).get(name));
    }
  }

}
