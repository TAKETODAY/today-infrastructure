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

package infra.context.expression;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.expression.AccessException;
import infra.expression.Expression;
import infra.expression.TypedValue;
import infra.expression.spel.standard.SpelCompiler;
import infra.expression.spel.standard.SpelExpressionParser;
import infra.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 21:33
 */
class MapAccessorTests {

  @Test
  void compilationSupport() {
    Map<String, Object> testMap = getSimpleTestMap();
    StandardEvaluationContext sec = new StandardEvaluationContext();
    sec.addPropertyAccessor(new MapAccessor());
    SpelExpressionParser sep = new SpelExpressionParser();

    // basic
    Expression ex = sep.parseExpression("foo");
    assertThat(ex.getValue(sec, testMap)).isEqualTo("bar");
    assertThat(SpelCompiler.compile(ex)).isTrue();
    assertThat(ex.getValue(sec, testMap)).isEqualTo("bar");

    // compound expression
    ex = sep.parseExpression("foo.toUpperCase()");
    assertThat(ex.getValue(sec, testMap)).isEqualTo("BAR");
    assertThat(SpelCompiler.compile(ex)).isTrue();
    assertThat(ex.getValue(sec, testMap)).isEqualTo("BAR");

    // nested map
    Map<String, Map<String, Object>> nestedMap = getNestedTestMap();
    ex = sep.parseExpression("aaa.foo.toUpperCase()");
    assertThat(ex.getValue(sec, nestedMap)).isEqualTo("BAR");
    assertThat(SpelCompiler.compile(ex)).isTrue();
    assertThat(ex.getValue(sec, nestedMap)).isEqualTo("BAR");

    // avoiding inserting checkcast because first part of expression returns a Map
    ex = sep.parseExpression("getMap().foo");
    MapGetter mapGetter = new MapGetter();
    assertThat(ex.getValue(sec, mapGetter)).isEqualTo("bar");
    assertThat(SpelCompiler.compile(ex)).isTrue();
    assertThat(ex.getValue(sec, mapGetter)).isEqualTo("bar");

    // basic isWritable
    ex = sep.parseExpression("foo");
    assertThat(ex.isWritable(sec, testMap)).isTrue();

    // basic write
    ex = sep.parseExpression("foo2");
    ex.setValue(sec, testMap, "bar2");
    assertThat(ex.getValue(sec, testMap)).isEqualTo("bar2");
    assertThat(SpelCompiler.compile(ex)).isTrue();
    assertThat(ex.getValue(sec, testMap)).isEqualTo("bar2");
  }

  @Test
  void canWrite() throws Exception {
    StandardEvaluationContext context = new StandardEvaluationContext();
    Map<String, Object> testMap = getSimpleTestMap();

    MapAccessor mapAccessor = new MapAccessor();
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
    Map<String, Object> testMap = getSimpleTestMap();
    StandardEvaluationContext sec = new StandardEvaluationContext();
    SpelExpressionParser sep = new SpelExpressionParser();
    Expression ex = sep.parseExpression("foo");

    assertThat(ex.isWritable(sec, testMap)).isFalse();

    sec.setPropertyAccessors(List.of(new MapAccessor(true)));
    assertThat(ex.isWritable(sec, testMap)).isTrue();

    sec.setPropertyAccessors(List.of(new MapAccessor(false)));
    assertThat(ex.isWritable(sec, testMap)).isFalse();
  }

  @Test
  void readNonExistentKeyThrowsMapAccessException() {
    MapAccessor accessor = new MapAccessor();
    Map<String, Object> map = new HashMap<>();

    assertThatExceptionOfType(AccessException.class)
            .isThrownBy(() -> accessor.read(null, map, "nonexistent"))
            .withMessage("Map does not contain a value for key 'nonexistent'");
  }

  @Test
  void readWithNullTargetThrowsException() {
    MapAccessor accessor = new MapAccessor();

    assertThatIllegalStateException()
            .isThrownBy(() -> accessor.read(null, null, "key"))
            .withMessage("Target must be of type Map");
  }

  @Test
  void writeWithNullTargetThrowsException() {
    MapAccessor accessor = new MapAccessor();

    assertThatIllegalStateException()
            .isThrownBy(() -> accessor.write(null, null, "key", "value"))
            .withMessage("Target must be of type Map");
  }

  @Test
  void readNullValueFromMap() throws AccessException {
    MapAccessor accessor = new MapAccessor();
    Map<String, Object> map = new HashMap<>();
    map.put("nullKey", null);

    TypedValue result = accessor.read(null, map, "nullKey");
    assertThat(result.getValue()).isNull();
  }

  @Test
  void canReadWithNonMapTarget() throws AccessException {
    MapAccessor accessor = new MapAccessor();
    Object nonMapTarget = new Object();

    assertThat(accessor.canRead(null, nonMapTarget, "key")).isFalse();
  }

  @Test
  void writeToUnmodifiableMap() {
    MapAccessor accessor = new MapAccessor();
    Map<String, Object> map = Map.of("key", "value");

    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> accessor.write(null, map, "newKey", "newValue"));
  }

  @Test
  void propertyTypeIsObject() {
    MapAccessor accessor = new MapAccessor();
    assertThat(accessor.getPropertyType()).isEqualTo(Object.class);
  }

  @Test
  void isCompilableReturnsTrue() {
    MapAccessor accessor = new MapAccessor();
    assertThat(accessor.isCompilable()).isTrue();
  }

  @Test
  void canReadWithNullKey() throws AccessException {
    MapAccessor accessor = new MapAccessor();
    Map<String, Object> map = new HashMap<>();
    map.put(null, "value");

    assertThat(accessor.canRead(null, map, null)).isTrue();
  }

  @Test
  void writeAndReadComplexObject() throws AccessException {
    MapAccessor accessor = new MapAccessor();
    Map<String, Object> map = new HashMap<>();
    List<String> complexValue = List.of("one", "two");

    accessor.write(null, map, "complex", complexValue);
    TypedValue result = accessor.read(null, map, "complex");

    assertThat(result.getValue()).isEqualTo(complexValue);
  }

  @Test
  void specificTargetClassesReturnsMapClass() {
    MapAccessor accessor = new MapAccessor();
    Class<?>[] targetClasses = accessor.getSpecificTargetClasses();
    assertThat(targetClasses).containsExactly(Map.class);
  }

  @Test
  void readMapWithMultipleEntries() throws AccessException {
    MapAccessor accessor = new MapAccessor();
    Map<String, Object> map = new HashMap<>();
    map.put("key1", "value1");
    map.put("key2", 123);
    map.put("key3", true);

    assertThat(accessor.read(null, map, "key1").getValue()).isEqualTo("value1");
    assertThat(accessor.read(null, map, "key2").getValue()).isEqualTo(123);
    assertThat(accessor.read(null, map, "key3").getValue()).isEqualTo(true);
  }

  @Test
  void writeOnlyMapAccessor() throws AccessException {
    MapAccessor accessor = new MapAccessor(false);
    Map<String, Object> map = new HashMap<>();
    map.put("test", "value");

    assertThat(accessor.canRead(null, map, "test")).isTrue();
    assertThat(accessor.canWrite(null, map, "test")).isFalse();
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
