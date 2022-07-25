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

package cn.taketoday.context.expression;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.expression.Expression;
import cn.taketoday.expression.spel.standard.SpelCompiler;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 21:33
 */
class MapAccessorTests {

  @Test
  public void mapAccessorCompilable() {
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

  public static class MapGetter {
    Map<String, Object> map = new HashMap<>();

    public MapGetter() {
      map.put("foo", "bar");
    }

    @SuppressWarnings("rawtypes")
    public Map getMap() {
      return map;
    }
  }

  public Map<String, Object> getSimpleTestMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("foo", "bar");
    return map;
  }

  public Map<String, Map<String, Object>> getNestedTestMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("foo", "bar");
    Map<String, Map<String, Object>> map2 = new HashMap<>();
    map2.put("aaa", map);
    return map2;
  }

}
