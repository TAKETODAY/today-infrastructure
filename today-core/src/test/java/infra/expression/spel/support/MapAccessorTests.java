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

package infra.expression.spel.support;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.expression.AccessException;
import infra.expression.spel.standard.SpelCompiler;
import infra.expression.spel.standard.SpelExpressionParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 21:33
 */
class MapAccessorTests {

  private final StandardEvaluationContext context = new StandardEvaluationContext();

  @Test
  void compilationSupport() {
    context.addPropertyAccessor(new MapAccessor());

    var parser = new SpelExpressionParser();
    var testMap = getSimpleTestMap();
    var nestedMap = getNestedTestMap();

    // basic
    var expression = parser.parseExpression("foo");
    assertThat(expression.getValue(context, testMap)).isEqualTo("bar");
    assertThat(SpelCompiler.compile(expression)).isTrue();
    assertThat(expression.getValue(context, testMap)).isEqualTo("bar");

    // compound expression
    expression = parser.parseExpression("foo.toUpperCase()");
    assertThat(expression.getValue(context, testMap)).isEqualTo("BAR");
    assertThat(SpelCompiler.compile(expression)).isTrue();
    assertThat(expression.getValue(context, testMap)).isEqualTo("BAR");

    // nested map
    expression = parser.parseExpression("aaa.foo.toUpperCase()");
    assertThat(expression.getValue(context, nestedMap)).isEqualTo("BAR");
    assertThat(SpelCompiler.compile(expression)).isTrue();
    assertThat(expression.getValue(context, nestedMap)).isEqualTo("BAR");

    // avoiding inserting checkcast because first part of expression returns a Map
    expression = parser.parseExpression("getMap().foo");
    MapGetter mapGetter = new MapGetter();
    assertThat(expression.getValue(context, mapGetter)).isEqualTo("bar");
    assertThat(SpelCompiler.compile(expression)).isTrue();
    assertThat(expression.getValue(context, mapGetter)).isEqualTo("bar");

    // basic isWritable
    expression = parser.parseExpression("foo");
    assertThat(expression.isWritable(context, testMap)).isTrue();

    // basic write
    expression = parser.parseExpression("foo2");
    expression.setValue(context, testMap, "bar2");
    assertThat(expression.getValue(context, testMap)).isEqualTo("bar2");
    assertThat(SpelCompiler.compile(expression)).isTrue();
    assertThat(expression.getValue(context, testMap)).isEqualTo("bar2");
  }

  @Test
  void canReadForNonMap() throws AccessException {
    var mapAccessor = new MapAccessor();

    assertThat(mapAccessor.canRead(context, new Object(), "foo")).isFalse();
  }

  @Test
  void canReadAndReadForExistingKeys() throws AccessException {
    var mapAccessor = new MapAccessor();
    var map = new HashMap<>();
    map.put("foo", null);
    map.put("bar", "baz");

    assertThat(mapAccessor.canRead(context, map, "foo")).isTrue();
    assertThat(mapAccessor.canRead(context, map, "bar")).isTrue();

    assertThat(mapAccessor.read(context, map, "foo").getValue()).isNull();
    assertThat(mapAccessor.read(context, map, "bar").getValue()).isEqualTo("baz");
  }

  @Test
  void canReadAndReadForNonexistentKeys() throws AccessException {
    var mapAccessor = new MapAccessor();
    var map = Map.of();

    assertThat(mapAccessor.canRead(context, map, "XXX")).isFalse();

    assertThatExceptionOfType(AccessException.class)
            .isThrownBy(() -> mapAccessor.read(context, map, "XXX").getValue())
            .withMessage("Map does not contain a value for key 'XXX'");
  }

  @Test
  void canWrite() throws AccessException {
    var mapAccessor = new MapAccessor();
    var testMap = getSimpleTestMap();

    assertThat(mapAccessor.canWrite(context, new Object(), "foo")).isFalse();
    assertThat(mapAccessor.canWrite(context, testMap, "foo")).isTrue();
    // Cannot actually write to an immutable Map, but MapAccessor cannot easily check for that.
    assertThat(mapAccessor.canWrite(context, Map.of(), "x")).isTrue();

    mapAccessor = new MapAccessor(false);
    assertThat(mapAccessor.canWrite(context, new Object(), "foo")).isFalse();
    assertThat(mapAccessor.canWrite(context, testMap, "foo")).isFalse();
  }

  @Test
  void isWritable() {
    var testMap = getSimpleTestMap();
    var parser = new SpelExpressionParser();
    var expression = parser.parseExpression("foo");

    assertThat(expression.isWritable(context, testMap)).isFalse();

    context.setPropertyAccessors(List.of(new MapAccessor(true)));
    assertThat(expression.isWritable(context, testMap)).isTrue();

    context.setPropertyAccessors(List.of(new MapAccessor(false)));
    assertThat(expression.isWritable(context, testMap)).isFalse();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static class MapGetter {
    Map map = new HashMap<>();

    public MapGetter() {
      this.map.put("foo", "bar");
    }

    public Map getMap() {
      return this.map;
    }
  }

  private static Map<String, Object> getSimpleTestMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("foo", "bar");
    return map;
  }

  private static Map<String, Map<String, Object>> getNestedTestMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("foo", "bar");
    Map<String, Map<String, Object>> map2 = new HashMap<>();
    map2.put("aaa", map);
    return map2;
  }

}
