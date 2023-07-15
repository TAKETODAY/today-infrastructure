/*
 * Copyright 2017 - 2023 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.TypeConverter;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Expression evaluation where the TypeConverter plugged in is the
 * {@link cn.taketoday.core.conversion.support.GenericConversionService}.
 *
 * @author Andy Clement
 * @author Dave Syer
 */
public class ExpressionWithConversionTests extends AbstractExpressionTests {

  private static final List<String> listOfString = new ArrayList<>();
  private static TypeDescriptor typeDescriptorForListOfString = null;
  private static final List<Integer> listOfInteger = new ArrayList<>();
  private static TypeDescriptor typeDescriptorForListOfInteger = null;

  static {
    listOfString.add("1");
    listOfString.add("2");
    listOfString.add("3");
    listOfInteger.add(4);
    listOfInteger.add(5);
    listOfInteger.add(6);
  }

  @BeforeEach
  public void setUp() throws Exception {
    ExpressionWithConversionTests.typeDescriptorForListOfString = new TypeDescriptor(ExpressionWithConversionTests.class.getDeclaredField("listOfString"));
    ExpressionWithConversionTests.typeDescriptorForListOfInteger = new TypeDescriptor(ExpressionWithConversionTests.class.getDeclaredField("listOfInteger"));
  }

  /**
   * Test the service can convert what we are about to use in the expression evaluation tests.
   */
  @Test
  public void testConversionsAvailable() throws Exception {
    TypeConvertorUsingConversionService tcs = new TypeConvertorUsingConversionService();

    // ArrayList containing List<Integer> to List<String>
    Class<?> clazz = typeDescriptorForListOfString.getElementDescriptor().getType();
    assertThat(clazz).isEqualTo(String.class);
    List<?> l = (List<?>) tcs.convertValue(listOfInteger, TypeDescriptor.forObject(listOfInteger), typeDescriptorForListOfString);
    assertThat(l).isNotNull();

    // ArrayList containing List<String> to List<Integer>
    clazz = typeDescriptorForListOfInteger.getElementDescriptor().getType();
    assertThat(clazz).isEqualTo(Integer.class);

    l = (List<?>) tcs.convertValue(listOfString, TypeDescriptor.forObject(listOfString), typeDescriptorForListOfString);
    assertThat(l).isNotNull();
  }

  @Test
  public void testSetParameterizedList() throws Exception {
    StandardEvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
    Expression e = parser.parseExpression("listOfInteger.size()");
    assertThat(e.getValue(context, Integer.class).intValue()).isEqualTo(0);
    context.setTypeConverter(new TypeConvertorUsingConversionService());
    // Assign a List<String> to the List<Integer> field - the component elements should be converted
    parser.parseExpression("listOfInteger").setValue(context, listOfString);
    // size now 3
    assertThat(e.getValue(context, Integer.class).intValue()).isEqualTo(3);
    Class<?> clazz = parser.parseExpression("listOfInteger[1].getClass()").getValue(context, Class.class); // element type correctly Integer
    assertThat(clazz).isEqualTo(Integer.class);
  }

  @Test
  public void testCoercionToCollectionOfPrimitive() throws Exception {

    class TestTarget {
      @SuppressWarnings("unused")
      public int sum(Collection<Integer> numbers) {
        int total = 0;
        for (int i : numbers) {
          total += i;
        }
        return total;
      }
    }

    StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

    TypeDescriptor collectionType = new TypeDescriptor(new MethodParameter(TestTarget.class.getDeclaredMethod(
            "sum", Collection.class), 0));
    // The type conversion is possible
    assertThat(evaluationContext.getTypeConverter()
            .canConvert(TypeDescriptor.valueOf(String.class), collectionType)).isTrue();
    // ... and it can be done successfully
    assertThat(evaluationContext.getTypeConverter().convertValue("1,2,3,4", TypeDescriptor.valueOf(String.class), collectionType).toString()).isEqualTo("[1, 2, 3, 4]");

    evaluationContext.setVariable("target", new TestTarget());

    // OK up to here, so the evaluation should be fine...
    // ... but this fails
    int result = (Integer) parser.parseExpression("#target.sum(#root)").getValue(evaluationContext, "1,2,3,4");
    assertThat(result).as("Wrong result: " + result).isEqualTo(10);

  }

  @Test
  public void testConvert() {
    Foo root = new Foo("bar");
    Collection<String> foos = Collections.singletonList("baz");

    StandardEvaluationContext context = new StandardEvaluationContext(root);

    // property access
    Expression expression = parser.parseExpression("foos");
    expression.setValue(context, foos);
    Foo baz = root.getFoos().iterator().next();
    assertThat(baz.value).isEqualTo("baz");

    // method call
    expression = parser.parseExpression("setFoos(#foos)");
    context.setVariable("foos", foos);
    expression.getValue(context);
    baz = root.getFoos().iterator().next();
    assertThat(baz.value).isEqualTo("baz");

    // method call with result from method call
    expression = parser.parseExpression("setFoos(getFoosAsStrings())");
    expression.getValue(context);
    baz = root.getFoos().iterator().next();
    assertThat(baz.value).isEqualTo("baz");

    // method call with result from method call
    expression = parser.parseExpression("setFoos(getFoosAsObjects())");
    expression.getValue(context);
    baz = root.getFoos().iterator().next();
    assertThat(baz.value).isEqualTo("baz");
  }

  /**
   * Type converter that uses the core conversion service.
   */
  private static class TypeConvertorUsingConversionService implements TypeConverter {

    private final ConversionService service = new DefaultConversionService();

    @Override
    public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return this.service.canConvert(sourceType, targetType);
    }

    @Override
    public Object convertValue(Object value, TypeDescriptor sourceType, TypeDescriptor targetType) throws EvaluationException {
      return this.service.convert(value, sourceType, targetType);
    }
  }

  public static class Foo {

    public final String value;

    private Collection<Foo> foos;

    public Foo(String value) {
      this.value = value;
    }

    public void setFoos(Collection<Foo> foos) {
      this.foos = foos;
    }

    public Collection<Foo> getFoos() {
      return this.foos;
    }

    public Collection<String> getFoosAsStrings() {
      return Collections.singletonList("baz");
    }

    public Collection<?> getFoosAsObjects() {
      return Collections.singletonList("baz");
    }
  }

}
