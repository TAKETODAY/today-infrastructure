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

package cn.taketoday.expression.spel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.expression.spel.ast.InlineMap;
import cn.taketoday.expression.spel.standard.SpelExpression;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test usage of inline maps.
 *
 * @author Andy Clement
 * @since 4.1
 */
public class MapTests extends AbstractExpressionTests {

  // if the list is full of literals then it will be of the type unmodifiableClass
  // rather than HashMap (or similar)
  Class<?> unmodifiableClass = Collections.unmodifiableMap(new LinkedHashMap<>()).getClass();

  @Test
  public void testInlineMapCreation01() {
    evaluate("{'a':1, 'b':2, 'c':3, 'd':4, 'e':5}", "{a=1, b=2, c=3, d=4, e=5}", unmodifiableClass);
    evaluate("{'a':1}", "{a=1}", unmodifiableClass);
  }

  @Test
  public void testInlineMapCreation02() {
    evaluate("{'abc':'def', 'uvw':'xyz'}", "{abc=def, uvw=xyz}", unmodifiableClass);
  }

  @Test
  public void testInlineMapCreation03() {
    evaluate("{:}", "{}", unmodifiableClass);
  }

  @Test
  public void testInlineMapCreation04() {
    evaluate("{'key':'abc'=='xyz'}", "{key=false}", LinkedHashMap.class);
    evaluate("{key:'abc'=='xyz'}", "{key=false}", LinkedHashMap.class);
    evaluate("{key:'abc'=='xyz',key2:true}[key]", "false", Boolean.class);
    evaluate("{key:'abc'=='xyz',key2:true}.get('key2')", "true", Boolean.class);
    evaluate("{key:'abc'=='xyz',key2:true}['key2']", "true", Boolean.class);
  }

  @Test
  public void testInlineMapAndNesting() {
    evaluate("{a:{a:1,b:2,c:3},b:{d:4,e:5,f:6}}", "{a={a=1, b=2, c=3}, b={d=4, e=5, f=6}}", unmodifiableClass);
    evaluate("{a:{x:1,y:'2',z:3},b:{u:4,v:{'a','b'},w:5,x:6}}", "{a={x=1, y=2, z=3}, b={u=4, v=[a, b], w=5, x=6}}", unmodifiableClass);
    evaluate("{a:{1,2,3},b:{4,5,6}}", "{a=[1, 2, 3], b=[4, 5, 6]}", unmodifiableClass);
  }

  @Test
  public void testInlineMapWithFunkyKeys() {
    evaluate("{#root.name:true}", "{Nikola Tesla=true}", LinkedHashMap.class);
  }

  @Test
  public void testInlineMapError() {
    parseAndCheckError("{key:'abc'", SpelMessage.OOD);
  }

  @Test
  public void testRelOperatorsIs02() {
    evaluate("{a:1, b:2, c:3, d:4, e:5} instanceof T(java.util.Map)", "true", Boolean.class);
  }

  @Test
  public void testInlineMapAndProjectionSelection() {
    evaluate("{a:1,b:2,c:3,d:4,e:5,f:6}.![value>3]", "[false, false, false, true, true, true]", ArrayList.class);
    evaluate("{a:1,b:2,c:3,d:4,e:5,f:6}.?[value>3]", "{d=4, e=5, f=6}", HashMap.class);
    evaluate("{a:1,b:2,c:3,d:4,e:5,f:6,g:7,h:8,i:9,j:10}.?[value%2==0]", "{b=2, d=4, f=6, h=8, j=10}", HashMap.class);
    // TODO this looks like a serious issue (but not a new one): the context object against which arguments are evaluated seems wrong:
//		evaluate("{a:1,b:2,c:3,d:4,e:5,f:6,g:7,h:8,i:9,j:10}.?[isEven(value) == 'y']", "[2, 4, 6, 8, 10]", ArrayList.class);
  }

  @Test
  public void testSetConstruction01() {
    evaluate("new java.util.HashMap().putAll({a:'a',b:'b',c:'c'})", null, Object.class);
  }

  @Test
  public void testConstantRepresentation1() {
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
  }

  private void checkConstantMap(String expressionText, boolean expectedToBeConstant) {
    SpelExpressionParser parser = new SpelExpressionParser();
    SpelExpression expression = (SpelExpression) parser.parseExpression(expressionText);
    SpelNode node = expression.getAST();
    boolean condition = node instanceof InlineMap;
    assertThat(condition).isTrue();
    InlineMap inlineMap = (InlineMap) node;
    if (expectedToBeConstant) {
      assertThat(inlineMap.isConstant()).isTrue();
    }
    else {
      assertThat(inlineMap.isConstant()).isFalse();
    }
  }

  @Test
  public void testInlineMapWriting() {
    // list should be unmodifiable
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            evaluate("{a:1, b:2, c:3, d:4, e:5}[a]=6", "[a:1,b: 2,c: 3,d: 4,e: 5]", unmodifiableClass));
  }

  @Test
  public void testMapKeysThatAreAlsoSpELKeywords() {
    SpelExpressionParser parser = new SpelExpressionParser();
    SpelExpression expression = null;
    Object o = null;

    // expression = (SpelExpression) parser.parseExpression("foo['NEW']");
    // o = expression.getValue(new MapHolder());
    // assertEquals("VALUE",o);

    expression = (SpelExpression) parser.parseExpression("foo[T]");
    o = expression.getValue(new MapHolder());
    assertThat(o).isEqualTo("TV");

    expression = (SpelExpression) parser.parseExpression("foo[t]");
    o = expression.getValue(new MapHolder());
    assertThat(o).isEqualTo("tv");

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

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static class MapHolder {

    public Map foo;

    public MapHolder() {
      foo = new HashMap();
      foo.put("NEW", "VALUE");
      foo.put("new", "value");
      foo.put("T", "TV");
      foo.put("t", "tv");
      foo.put("abc.def", "value");
      foo.put("VALUE", "37");
      foo.put("value", "38");
      foo.put("TV", "new");
    }
  }

}
