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

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.ExpressionParser;
import cn.taketoday.expression.PropertyAccessor;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testing variations on map access.
 *
 * @author Andy Clement
 */
public class MapAccessTests extends AbstractExpressionTests {

  @Test
  public void testSimpleMapAccess01() {
    evaluate("testMap.get('monday')", "montag", String.class);
  }

  @Test
  public void testMapAccessThroughIndexer() {
    evaluate("testMap['monday']", "montag", String.class);
  }

  @Test
  public void testCustomMapAccessor() throws Exception {
    ExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext ctx = TestScenarioCreator.getTestEvaluationContext();
    ctx.addPropertyAccessor(new MapAccessor());

    Expression expr = parser.parseExpression("testMap.monday");
    Object value = expr.getValue(ctx, String.class);
    assertThat(value).isEqualTo("montag");
  }

  @Test
  public void testVariableMapAccess() throws Exception {
    ExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext ctx = TestScenarioCreator.getTestEvaluationContext();
    ctx.setVariable("day", "saturday");

    Expression expr = parser.parseExpression("testMap[#day]");
    Object value = expr.getValue(ctx, String.class);
    assertThat(value).isEqualTo("samstag");
  }

  @Test
  public void testGetValue() {
    Map<String, String> props1 = new HashMap<>();
    props1.put("key1", "value1");
    props1.put("key2", "value2");
    props1.put("key3", "value3");

    Object bean = new TestBean("name1", new TestBean("name2", null, "Description 2", 15, props1), "description 1", 6, props1);

    ExpressionParser parser = new SpelExpressionParser();
    Expression expr = parser.parseExpression("testBean.properties['key2']");
    assertThat(expr.getValue(bean)).isEqualTo("value2");
  }

  @Test
  public void testGetValueFromRootMap() {
    Map<String, String> map = new HashMap<>();
    map.put("key", "value");

    ExpressionParser spelExpressionParser = new SpelExpressionParser();
    Expression expr = spelExpressionParser.parseExpression("#root['key']");
    assertThat(expr.getValue(map)).isEqualTo("value");
  }

  public static class TestBean {

    private String name;
    private TestBean testBean;
    private String description;
    private Integer priority;
    private Map<String, String> properties;

    public TestBean(String name, TestBean testBean, String description, Integer priority, Map<String, String> props) {
      this.name = name;
      this.testBean = testBean;
      this.description = description;
      this.priority = priority;
      this.properties = props;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public TestBean getTestBean() {
      return testBean;
    }

    public void setTestBean(TestBean testBean) {
      this.testBean = testBean;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Integer getPriority() {
      return priority;
    }

    public void setPriority(Integer priority) {
      this.priority = priority;
    }

    public Map<String, String> getProperties() {
      return properties;
    }

    public void setProperties(Map<String, String> properties) {
      this.properties = properties;
    }
  }

  public static class MapAccessor implements PropertyAccessor {

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
      return (((Map<?, ?>) target).containsKey(name));
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
      return new TypedValue(((Map<?, ?>) target).get(name));
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
      return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
      ((Map<Object, Object>) target).put(name, newValue);
    }

    @Override
    public Class<?>[] getSpecificTargetClasses() {
      return new Class<?>[] { Map.class };
    }
  }

}
