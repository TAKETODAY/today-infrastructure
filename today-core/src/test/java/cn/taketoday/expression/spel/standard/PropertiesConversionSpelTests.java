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

package cn.taketoday.expression.spel.standard;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import cn.taketoday.expression.Expression;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 */
public class PropertiesConversionSpelTests {

  private static final SpelExpressionParser parser = new SpelExpressionParser();

  @Test
  void props() {
    Properties props = new Properties();
    props.setProperty("x", "1");
    props.setProperty("y", "2");
    props.setProperty("z", "3");
    Expression expression = parser.parseExpression("foo(#props)");
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setVariable("props", props);
    String result = expression.getValue(context, new TestBean(), String.class);
    assertThat(result).isEqualTo("123");
  }

  @Test
  void mapWithAllStringValues() {
    Map<String, Object> map = new HashMap<>();
    map.put("x", "1");
    map.put("y", "2");
    map.put("z", "3");
    Expression expression = parser.parseExpression("foo(#props)");
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setVariable("props", map);
    String result = expression.getValue(context, new TestBean(), String.class);
    assertThat(result).isEqualTo("123");
  }

  @Test
  void mapWithNonStringValue() {
    Map<String, Object> map = new HashMap<>();
    map.put("x", "1");
    map.put("y", 2);
    map.put("z", "3");
    map.put("a", new UUID(1, 1));
    Expression expression = parser.parseExpression("foo(#props)");
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setVariable("props", map);
    String result = expression.getValue(context, new TestBean(), String.class);
    assertThat(result).isEqualTo("1null3");
  }

  @Test
  void customMapWithNonStringValue() {
    CustomMap map = new CustomMap();
    map.put("x", "1");
    map.put("y", 2);
    map.put("z", "3");
    Expression expression = parser.parseExpression("foo(#props)");
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setVariable("props", map);
    String result = expression.getValue(context, new TestBean(), String.class);
    assertThat(result).isEqualTo("1null3");
  }

  private static class TestBean {

    @SuppressWarnings("unused")
    public String foo(Properties props) {
      return props.getProperty("x") + props.getProperty("y") + props.getProperty("z");
    }
  }

  @SuppressWarnings("serial")
  private static class CustomMap extends HashMap<String, Object> {
  }

}
