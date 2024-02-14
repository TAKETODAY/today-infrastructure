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

package cn.taketoday.expression.spel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.expression.Expression;
import cn.taketoday.expression.spel.ast.InlineMap;
import cn.taketoday.expression.spel.standard.SpelExpression;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test usage of inline maps.
 *
 * @author Andy Clement
 * @since 4.0
 */
public class MapTests extends AbstractExpressionTests {

  // if the list is full of literals then it will be of the type unmodifiableMapClass
  // rather than HashMap (or similar)
  private static final Class<?> unmodifiableMapClass = Collections.unmodifiableMap(Map.of()).getClass();

  @Test
  void inlineMapCreationForLiterals() {
    evaluate("{'a':1, 'b':2, 'c':3, 'd':4, 'e':5}", "{a=1, b=2, c=3, d=4, e=5}", unmodifiableMapClass);
    evaluate("{'a':1}", "{a=1}", unmodifiableMapClass);
    evaluate("{'abc':'def', 'uvw':'xyz'}", "{abc=def, uvw=xyz}", unmodifiableMapClass);
    evaluate("{:}", "{}", unmodifiableMapClass);
  }

  @Test
  void inlineMapCreationForNonLiterals() {
    evaluate("{'key':'abc'=='xyz'}", "{key=false}", LinkedHashMap.class);
    evaluate("{key:'abc'=='xyz'}", "{key=false}", LinkedHashMap.class);
    evaluate("{key:'abc'=='xyz',key2:true}[key]", "false", Boolean.class);
    evaluate("{key:'abc'=='xyz',key2:true}.get('key2')", "true", Boolean.class);
    evaluate("{key:'abc'=='xyz',key2:true}['key2']", "true", Boolean.class);
  }

  @Test
  void inlineMapAndNesting() {
    evaluate("{a:{a:1,b:2,c:3},b:{d:4,e:5,f:6}}", "{a={a=1, b=2, c=3}, b={d=4, e=5, f=6}}", unmodifiableMapClass);
    evaluate("{a:{x:1,y:'2',z:3},b:{u:4,v:{'a','b'},w:5,x:6}}", "{a={x=1, y=2, z=3}, b={u=4, v=[a, b], w=5, x=6}}", unmodifiableMapClass);
    evaluate("{a:{1,2,3},b:{4,5,6}}", "{a=[1, 2, 3], b=[4, 5, 6]}", unmodifiableMapClass);
  }

  @Test
  void inlineMapWithFunkyKeys() {
    evaluate("{#root.name:true}", "{Nikola Tesla=true}", LinkedHashMap.class);
  }

  @Test
  void inlineMapSyntaxError() {
    parseAndCheckError("{key:'abc'", SpelMessage.OOD);
  }

  @Test
  void inelineMapIsInstanceOfMap() {
    evaluate("{a:1, b:2} instanceof T(java.util.Map)", "true", Boolean.class);
  }

  @Test
  void inlineMapProjection() {
    evaluate("{a:1,b:2,c:3,d:4}.![value > 2]", "[false, false, true, true]", ArrayList.class);
    evaluate("{a:1,b:2,c:3,d:4}.![value % 2 == 0]", "[false, true, false, true]", ArrayList.class);
    evaluate("{a:1,b:2,c:3,d:4}.![#isEven(value) == 'y']", "[false, true, false, true]", ArrayList.class);
    evaluate("{'a':'y','b':'n','c':'y'}.![value == 'y' ? key : null]", "[a, null, c]", ArrayList.class);
  }

  @Test
  void inlineMapSelection() {
    evaluate("{a:1,b:2,c:3,d:4}.?[value > 2]", "{c=3, d=4}", HashMap.class);
    evaluate("{a:1,b:2,c:3,d:4,e:5,f:6}.?[value % 2 == 0]", "{b=2, d=4, f=6}", HashMap.class);
    evaluate("{a:1,b:2,c:3,d:4,e:5,f:6}.?[#isEven(value) == 'y']", "{b=2, d=4, f=6}", HashMap.class);
  }

  @Test
  void mapConstruction() {
    evaluate("new java.util.HashMap().putAll({a:'a',b:'b'})", null, Object.class);
  }

  @Test
  void constantMaps() {
    checkConstantMap("{f:{'a','b','c'}}", true);
    checkConstantMap("{'a':1,'b':2,'c':3,'d':4,'e':5}", true);
    checkConstantMap("{aaa:'abc'}", true);
    checkConstantMap("{:}", true);
    checkConstantMap("{a:#a,b:2,c:3}", false);
    checkConstantMap("{a:1,b:2,c:Integer.valueOf(4)}", false);
    checkConstantMap("{a:1,b:2,c:{#a}}", false);
    checkConstantMap("{#root.name:true}", false);
    checkConstantMap("{a:1,b:2,c:{d:true,e:false}}", true);
    checkConstantMap("{a:1,b:2,c:{d:{1,2,3},e:{4,5,6},f:{'a','b','c'}}}", true);
    checkConstantMap("{aaa:{a:#a,b:2,c:3}}", false);
    checkConstantMap("{a:{k:#d}}", false); // nested InlineMap
    checkConstantMap("{@bean:@bean}", false);
  }

  private void checkConstantMap(String expressionText, boolean expectedToBeConstant) {
    SpelExpressionParser parser = new SpelExpressionParser();
    SpelExpression expression = (SpelExpression) parser.parseExpression(expressionText);
    SpelNode node = expression.getAST();
    assertThat(node).isInstanceOfSatisfying(InlineMap.class, inlineMap -> {
      if (expectedToBeConstant) {
        assertThat(inlineMap.isConstant()).isTrue();
      }
      else {
        assertThat(inlineMap.isConstant()).isFalse();
      }
    });
  }

  @Test
  void inlineMapIsUnmodifiable() {
    Expression expr = parser.parseExpression("{a:1}[a] = 6");
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> expr.getValue(context));
  }

  @Test
  void mapKeysThatAreAlsoSpELKeywords() {
    SpelExpressionParser parser = new SpelExpressionParser();
    SpelExpression expression = null;
    Object o = null;

    expression = (SpelExpression) parser.parseExpression("foo[T]");
    o = expression.getValue(new MapHolder());
    assertThat(o).isEqualTo("TV");

    expression = (SpelExpression) parser.parseExpression("foo[t]");
    o = expression.getValue(new MapHolder());
    assertThat(o).isEqualTo("tv");

    expression = (SpelExpression) parser.parseExpression("foo['NEW']");
    o = expression.getValue(new MapHolder());
    assertThat(o).isEqualTo("VALUE");

    expression = (SpelExpression) parser.parseExpression("foo[NEW]");
    o = expression.getValue(new MapHolder());
    assertThat(o).isEqualTo("VALUE");

    expression = (SpelExpression) parser.parseExpression("foo[new]");
    o = expression.getValue(new MapHolder());
    assertThat(o).isEqualTo("value");

    expression = (SpelExpression) parser.parseExpression("foo['abc.def']");
    o = expression.getValue(new MapHolder());
    assertThat(o).isEqualTo("value");

    expression = (SpelExpression) parser.parseExpression("foo[foo[NEW]]");
    o = expression.getValue(new MapHolder());
    assertThat(o).isEqualTo("37");

    expression = (SpelExpression) parser.parseExpression("foo[foo[new]]");
    o = expression.getValue(new MapHolder());
    assertThat(o).isEqualTo("38");

    expression = (SpelExpression) parser.parseExpression("foo[foo[foo[T]]]");
    o = expression.getValue(new MapHolder());
    assertThat(o).isEqualTo("value");
  }

  @SuppressWarnings({ "rawtypes" })
  static class MapHolder {

    public Map foo = Map.of(
            "NEW", "VALUE",
            "new", "value",
            "T", "TV",
            "t", "tv",
            "abc.def", "value",
            "VALUE", "37",
            "value", "38",
            "TV", "new"
    );
  }

}
