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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import infra.core.MethodParameter;
import infra.core.TypeDescriptor;
import infra.core.conversion.support.GenericConversionService;
import infra.expression.Expression;
import infra.expression.TypeConverter;
import infra.expression.spel.support.StandardEvaluationContext;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Expression evaluation where the TypeConverter plugged in is the
 * {@link GenericConversionService}.
 *
 * @author Andy Clement
 * @author Dave Syer
 */
class ExpressionWithConversionTests extends AbstractExpressionTests {

  private static final List<String> listOfString = List.of("1", "2", "3");
  private static final List<Integer> listOfInteger = List.of(4, 5, 6);

  private static final TypeDescriptor typeDescriptorForListOfString =
          new TypeDescriptor(ReflectionUtils.findField(ExpressionWithConversionTests.class, "listOfString"));
  private static final TypeDescriptor typeDescriptorForListOfInteger =
          new TypeDescriptor(ReflectionUtils.findField(ExpressionWithConversionTests.class, "listOfInteger"));

  /**
   * Test the service can convert what we are about to use in the expression evaluation tests.
   */
  @BeforeAll
  @SuppressWarnings("unchecked")
  static void verifyConversionsAreSupportedByStandardTypeConverter() {
    StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
    TypeConverter typeConverter = evaluationContext.getTypeConverter();

    // List<Integer> to List<String>
    assertThat(typeDescriptorForListOfString.getElementDescriptor().getType())
            .isEqualTo(String.class);
    List<String> strings = (List<String>) typeConverter.convertValue(listOfInteger,
            typeDescriptorForListOfInteger, typeDescriptorForListOfString);
    assertThat(strings).containsExactly("4", "5", "6");

    // List<String> to List<Integer>
    assertThat(typeDescriptorForListOfInteger.getElementDescriptor().getType())
            .isEqualTo(Integer.class);
    List<Integer> integers = (List<Integer>) typeConverter.convertValue(listOfString,
            typeDescriptorForListOfString, typeDescriptorForListOfInteger);
    assertThat(integers).containsExactly(1, 2, 3);
  }

  @Test
  void setParameterizedList() {
    StandardEvaluationContext context = TestScenarioCreator.getTestEvaluationContext();

    Expression e = parser.parseExpression("listOfInteger.size()");
    assertThat(e.getValue(context, Integer.class)).isZero();

    // Assign a List<String> to the List<Integer> field - the component elements should be converted
    parser.parseExpression("listOfInteger").setValue(context, listOfString);
    // size now 3
    assertThat(e.getValue(context, Integer.class)).isEqualTo(3);
    // element type correctly Integer
    Class<?> clazz = parser.parseExpression("listOfInteger[1].getClass()").getValue(context, Class.class);
    assertThat(clazz).isEqualTo(Integer.class);
  }

  @Test
  void coercionToCollectionOfPrimitive() throws Exception {

    class TestTarget {
      @SuppressWarnings("unused")
      public int sum(Collection<Integer> numbers) {
        return numbers.stream().reduce(0, (a, b) -> a + b);
      }
    }

    StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
    TypeConverter typeConverter = evaluationContext.getTypeConverter();

    TypeDescriptor collectionType = new TypeDescriptor(new MethodParameter(TestTarget.class.getDeclaredMethod(
            "sum", Collection.class), 0));
    // The type conversion is possible
    assertThat(typeConverter.canConvert(TypeDescriptor.valueOf(String.class), collectionType)).isTrue();
    // ... and it can be done successfully
    assertThat(typeConverter.convertValue("1,2,3,4", TypeDescriptor.valueOf(String.class), collectionType))
            .hasToString("[1, 2, 3, 4]");

    evaluationContext.setVariable("target", new TestTarget());

    // OK up to here, so the evaluation should be fine...
    int sum = parser.parseExpression("#target.sum(#root)").getValue(evaluationContext, "1,2,3,4", int.class);
    assertThat(sum).isEqualTo(10);
  }

  @Test
  void convert() {
    Foo root = new Foo("bar");
    Collection<String> foos = Set.of("baz");

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
    assertThat(baz.value).isEqualTo("quux");
  }

  @Test
  void convertOptionalToContainedTargetForMethodInvocations() {
    StandardEvaluationContext context = new StandardEvaluationContext(new JediService());

    // Verify findByName('Yoda') returns an Optional.
    Expression expression = parser.parseExpression("findByName('Yoda') instanceof T(java.util.Optional)");
    assertThat(expression.getValue(context, Boolean.class)).isTrue();

    // Verify we can pass a Jedi directly to greet().
    expression = parser.parseExpression("greet(findByName('Yoda').get())");
    assertThat(expression.getValue(context, String.class)).isEqualTo("Hello, Yoda");

    // Verify that an Optional<Jedi> will be unwrapped to a Jedi to pass to greet().
    expression = parser.parseExpression("greet(findByName('Yoda'))");
    assertThat(expression.getValue(context, String.class)).isEqualTo("Hello, Yoda");

    // Verify that an empty Optional will be converted to null to pass to greet().
    expression = parser.parseExpression("greet(findByName(''))");
    assertThat(expression.getValue(context, String.class)).isEqualTo("Hello, null");
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
      return Set.of("baz");
    }

    public Collection<?> getFoosAsObjects() {
      return Set.of("quux");
    }
  }

  record Jedi(String name) {
  }

  static class JediService {

    public Optional<Jedi> findByName(String name) {
      if (name.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(new Jedi(name));
    }

    public String greet(@Nullable Jedi jedi) {
      return "Hello, " + (jedi != null ? jedi.name() : null);
    }
  }

}
