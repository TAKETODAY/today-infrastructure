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
import java.util.List;
import java.util.Map;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.PropertyAccessor;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.standard.SpelExpression;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.SimpleEvaluationContext;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;
import cn.taketoday.expression.spel.testresources.Inventor;
import cn.taketoday.expression.spel.testresources.Person;
import cn.taketoday.expression.spel.testresources.RecordPerson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for property access.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Joyce Zhan
 * @author Sam Brannen
 */
public class PropertyAccessTests extends AbstractExpressionTests {

  @Test
  void simpleAccess01() {
    evaluate("name", "Nikola Tesla", String.class);
  }

  @Test
  void simpleAccess02() {
    evaluate("placeOfBirth.city", "SmilJan", String.class);
  }

  @Test
  void simpleAccess03() {
    evaluate("stringArrayOfThreeItems.length", "3", Integer.class);
  }

  @Test
  void nonExistentPropertiesAndMethods() {
    // madeup does not exist as a property
    evaluateAndCheckError("madeup", SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, 0);

    // name is ok but foobar does not exist:
    evaluateAndCheckError("name.foobar", SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, 5);
  }

  /**
   * The standard reflection resolver cannot find properties on null objects but some
   * supplied resolver might be able to - so null shouldn't crash the reflection resolver.
   */
  @Test
  void accessingOnNullObject() {
    SpelExpression expr = (SpelExpression) parser.parseExpression("madeup");
    EvaluationContext context = new StandardEvaluationContext(null);
    assertThatExceptionOfType(SpelEvaluationException.class)
            .isThrownBy(() -> expr.getValue(context))
            .extracting(SpelEvaluationException::getMessageCode).isEqualTo(SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE_ON_NULL);
    assertThat(expr.isWritable(context)).isFalse();
    assertThatExceptionOfType(SpelEvaluationException.class)
            .isThrownBy(() -> expr.setValue(context, "abc"))
            .extracting(SpelEvaluationException::getMessageCode).isEqualTo(SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE_ON_NULL);
  }

  @Test
    // Adding a new property accessor just for a particular type
  void addingSpecificPropertyAccessor() {
    SpelExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext ctx = new StandardEvaluationContext();

    // Even though this property accessor is added after the reflection one, it specifically
    // names the String class as the type it is interested in so is chosen in preference to
    // any 'default' ones
    ctx.addPropertyAccessor(new StringyPropertyAccessor());
    Expression expr = parser.parseRaw("new String('hello').flibbles");
    Integer i = expr.getValue(ctx, Integer.class);
    assertThat((int) i).isEqualTo(7);

    // The reflection one will be used for other properties...
    expr = parser.parseRaw("new String('hello').CASE_INSENSITIVE_ORDER");
    Object o = expr.getValue(ctx);
    assertThat(o).isNotNull();

    SpelExpression flibbleexpr = parser.parseRaw("new String('hello').flibbles");
    flibbleexpr.setValue(ctx, 99);
    i = flibbleexpr.getValue(ctx, Integer.class);
    assertThat((int) i).isEqualTo(99);

    // Cannot set it to a string value
    assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
            flibbleexpr.setValue(ctx, "not allowed"));
    // message will be: EL1063E:(pos 20): A problem occurred whilst attempting to set the property
    // 'flibbles': 'Cannot set flibbles to an object of type 'class java.lang.String''
    // System.out.println(e.getMessage());
  }

  @Test
  void addingAndRemovingAccessors() {
    StandardEvaluationContext ctx = new StandardEvaluationContext();

    // reflective property accessor is the only one by default
    assertThat(ctx.getPropertyAccessors()).hasSize(1);

    StringyPropertyAccessor spa = new StringyPropertyAccessor();
    ctx.addPropertyAccessor(spa);
    assertThat(ctx.getPropertyAccessors()).hasSize(2);

    List<PropertyAccessor> copy = new ArrayList<>(ctx.getPropertyAccessors());
    assertThat(ctx.removePropertyAccessor(spa)).isTrue();
    assertThat(ctx.removePropertyAccessor(spa)).isFalse();
    assertThat(ctx.getPropertyAccessors()).hasSize(1);

    ctx.setPropertyAccessors(copy);
    assertThat(ctx.getPropertyAccessors()).hasSize(2);
  }

  @Test
  void accessingPropertyOfClass() {
    Expression expression = parser.parseExpression("name");
    Object value = expression.getValue(new StandardEvaluationContext(String.class));
    assertThat(value).isEqualTo("java.lang.String");
  }

  @Test
  void shouldAlwaysUsePropertyAccessorFromEvaluationContext() {
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("name");

    StandardEvaluationContext context = new StandardEvaluationContext();
    context.addPropertyAccessor(new ConfigurablePropertyAccessor(Collections.singletonMap("name", "Ollie")));
    assertThat(expression.getValue(context)).isEqualTo("Ollie");

    context = new StandardEvaluationContext();
    context.addPropertyAccessor(new ConfigurablePropertyAccessor(Collections.singletonMap("name", "Jens")));
    assertThat(expression.getValue(context)).isEqualTo("Jens");
  }

  @Test
  void standardGetClassAccess() {
    assertThat(parser.parseExpression("'a'.class.name").getValue()).isEqualTo(String.class.getName());
  }

  @Test
  void noGetClassAccess() {
    EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() ->
            parser.parseExpression("'a'.class.name").getValue(context));
  }

  @Test
  void propertyReadOnly() {
    EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();

    Expression expr = parser.parseExpression("name");
    Person target = new Person("p1");
    assertThat(expr.getValue(context, target)).isEqualTo("p1");
    target.setName("p2");
    assertThat(expr.getValue(context, target)).isEqualTo("p2");

    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() ->
            parser.parseExpression("name='p3'").getValue(context, target));
  }

  @Test
  void propertyReadOnlyWithRecordStyle() {
    EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();

    Expression expr = parser.parseExpression("name");
    RecordPerson target1 = new RecordPerson("p1");
    assertThat(expr.getValue(context, target1)).isEqualTo("p1");
    RecordPerson target2 = new RecordPerson("p2");
    assertThat(expr.getValue(context, target2)).isEqualTo("p2");

    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() ->
            parser.parseExpression("name='p3'").getValue(context, target2));
  }

  @Test
  void propertyReadWrite() {
    EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();

    Expression expr = parser.parseExpression("name");
    Person target = new Person("p1");
    assertThat(expr.getValue(context, target)).isEqualTo("p1");
    target.setName("p2");
    assertThat(expr.getValue(context, target)).isEqualTo("p2");

    parser.parseExpression("name='p3'").getValue(context, target);
    assertThat(target.getName()).isEqualTo("p3");
    assertThat(expr.getValue(context, target)).isEqualTo("p3");

    expr.setValue(context, target, "p4");
    assertThat(target.getName()).isEqualTo("p4");
    assertThat(expr.getValue(context, target)).isEqualTo("p4");
  }

  @Test
  void propertyReadWriteWithRootObject() {
    Person rootObject = new Person("p1");
    EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().withRootObject(rootObject).build();
    assertThat(context.getRootObject().getValue()).isSameAs(rootObject);

    Expression expr = parser.parseExpression("name");
    assertThat(expr.getValue(context)).isEqualTo("p1");
    rootObject.setName("p2");
    assertThat(expr.getValue(context)).isEqualTo("p2");

    parser.parseExpression("name='p3'").getValue(context, rootObject);
    assertThat(rootObject.getName()).isEqualTo("p3");
    assertThat(expr.getValue(context)).isEqualTo("p3");

    expr.setValue(context, "p4");
    assertThat(rootObject.getName()).isEqualTo("p4");
    assertThat(expr.getValue(context)).isEqualTo("p4");
  }

  @Test
  void propertyAccessWithoutMethodResolver() {
    EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
    Person target = new Person("p1");
    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() ->
            parser.parseExpression("name.substring(1)").getValue(context, target));
  }

  @Test
  void propertyAccessWithInstanceMethodResolver() {
    EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().withInstanceMethods().build();
    Person target = new Person("p1");
    assertThat(parser.parseExpression("name.substring(1)").getValue(context, target)).isEqualTo("1");
  }

  @Test
  void propertyAccessWithInstanceMethodResolverAndTypedRootObject() {
    Person rootObject = new Person("p1");
    EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().
            withInstanceMethods().withTypedRootObject(rootObject, TypeDescriptor.valueOf(Object.class)).build();

    assertThat(parser.parseExpression("name.substring(1)").getValue(context)).isEqualTo("1");
    assertThat(context.getRootObject().getValue()).isSameAs(rootObject);
    assertThat(context.getRootObject().getTypeDescriptor().getType()).isSameAs(Object.class);
  }

  @Test
  void propertyAccessWithArrayIndexOutOfBounds() {
    EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
    Expression expression = parser.parseExpression("stringArrayOfThreeItems[3]");
    assertThatExceptionOfType(SpelEvaluationException.class)
            .isThrownBy(() -> expression.getValue(context, new Inventor()))
            .extracting(SpelEvaluationException::getMessageCode).isEqualTo(SpelMessage.ARRAY_INDEX_OUT_OF_BOUNDS);
  }

  // This can resolve the property 'flibbles' on any String (very useful...)
  private static class StringyPropertyAccessor implements PropertyAccessor {

    int flibbles = 7;

    @Override
    public Class<?>[] getSpecificTargetClasses() {
      return new Class<?>[] { String.class };
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
      if (!(target instanceof String)) {
        throw new RuntimeException("Assertion Failed! target should be String");
      }
      return (name.equals("flibbles"));
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
      if (!(target instanceof String)) {
        throw new RuntimeException("Assertion Failed! target should be String");
      }
      return (name.equals("flibbles"));
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
      if (!name.equals("flibbles")) {
        throw new RuntimeException("Assertion Failed! name should be flibbles");
      }
      return new TypedValue(flibbles);
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
      if (!name.equals("flibbles")) {
        throw new RuntimeException("Assertion Failed! name should be flibbles");
      }
      try {
        flibbles = (Integer) context.getTypeConverter().convertValue(newValue,
                TypeDescriptor.fromObject(newValue), TypeDescriptor.valueOf(Integer.class));
      }
      catch (EvaluationException ex) {
        throw new AccessException("Cannot set flibbles to an object of type '" + newValue.getClass() + "'");
      }
    }
  }

  private static class ConfigurablePropertyAccessor implements PropertyAccessor {

    private final Map<String, Object> values;

    ConfigurablePropertyAccessor(Map<String, Object> values) {
      this.values = values;
    }

    @Override
    public Class<?>[] getSpecificTargetClasses() {
      return null;
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) {
      return true;
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) {
      return new TypedValue(this.values.get(name));
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) {
      return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) {
    }
  }

}
