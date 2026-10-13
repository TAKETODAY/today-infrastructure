/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.expression.spel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import infra.core.conversion.support.DefaultConversionService;
import infra.expression.spel.standard.SpelExpression;
import infra.expression.spel.standard.SpelExpressionParser;
import infra.expression.spel.support.StandardEvaluationContext;
import infra.expression.spel.support.StandardTypeConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Adapted string concatenation compilation cases from SpelCompilationCoverageTests.
 */
class StringConcatenationCompilationTests {

  @ParameterizedTest
  @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
          "'hello' + 42|hello42", "'hello' + 42L|hello42", "'hello' + 3.14f|hello3.14",
          "'hello' + 3.14d|hello3.14", "'hello' + true|hellotrue", "42 + ' world'|42 world",
          "1 + 2 + 'a'|3a", "'a' + (1 + 2)|a3", "'a' + (1.5 + 2) + 'b'|a3.5b",
          "'a' + +3|a3", "'hello' + 3 + ' spring'|hello3 spring"
  })
  void literalAndNestedNumericOperands(String source, String expected) {
    SpelExpression expression = new SpelExpressionParser().parseRaw(source);
    assertThat(expression.getValue()).isEqualTo(expected);
    assertThat(expression.compileExpression()).isTrue();
    assertThat(expression.getValue()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
          "'value: ' + primitiveChar|value: c", "'value: ' + primitiveByte|value: 8",
          "primitiveShort + ' :value'|16 :value", "primitiveInt + primitiveInt + ' items'|84 items",
          "'value: ' + boxedInteger|value: 42", "boxedDouble + ' :value'|3.5 :value",
          "'value: ' + localDate|value: 2026-10-09", "'value: ' + bigDecimal|value: 1.50",
          "'value: ' + map['key']|value: 42"
  })
  void typedOperands(String source, String expected) {
    var operands = new Operands();
    var expression = new SpelExpressionParser().parseRaw(source);
    assertThat(expression.getValue(operands)).isEqualTo(expected);
    assertThat(expression.compileExpression()).isTrue();
    assertThat(expression.getValue(operands)).isEqualTo(expected);
  }

  @Test
  void nullOperandRequiresObservedConversion() {
    var operands = new Operands();
    operands.boxedInteger = null;
    var expression = new SpelExpressionParser().parseRaw("'value: ' + boxedInteger");
    assertThat(expression.getValue(operands)).isEqualTo("value: null");
    assertThat(expression.compileExpression()).isFalse();
    operands.boxedInteger = 42;
    assertThat(expression.getValue(operands)).isEqualTo("value: 42");
    assertThat(expression.compileExpression()).isTrue();
    assertThat(expression.getValue(operands)).isEqualTo("value: 42");
    operands.boxedInteger = null;
    assertThat(expression.getValue(operands)).isEqualTo("value: null");
  }

  @Test
  void differingConversionPreventsCompilation() {
    var service = new DefaultConversionService();
    service.addConverter(Integer.class, String.class, value -> "int-" + value);
    var context = new StandardEvaluationContext();
    context.setTypeConverter(new StandardTypeConverter(service));
    var expression = new SpelExpressionParser().parseRaw("'prefix-' + 42");
    assertThat(expression.getValue(context)).isEqualTo("prefix-int-42");
    assertThat(expression.compileExpression()).isFalse();
  }

  @Test
  void changedRuntimeTypeFailsOrFallsBackInMixedMode() {
    var operands = new Operands();
    var expression = new SpelExpressionParser().parseRaw("'[' + object + ']'");
    assertThat(expression.getValue(operands)).isEqualTo("[42]");
    assertThat(expression.compileExpression()).isTrue();
    operands.object = null;
    assertThat(expression.getValue(operands)).isEqualTo("[null]");
    operands.object = List.of(1, 2);
    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() -> expression.getValue(operands))
            .withCauseInstanceOf(IllegalStateException.class);
    var mixed = new SpelExpressionParser(SpelParserConfiguration.builder().compilerMode(SpelCompilerMode.MIXED).build())
            .parseRaw("'[' + object + ']'");
    operands.object = 42;
    assertThat(mixed.getValue(operands)).isEqualTo("[42]");
    assertThat(mixed.compileExpression()).isTrue();
    operands.object = List.of(1, 2);
    assertThat(mixed.getValue(operands)).isEqualTo("[1,2]");
  }

  public static class Operands {
    public int primitiveInt = 42;
    public char primitiveChar = 'c';
    public byte primitiveByte = 8;
    public short primitiveShort = 16;
    public Integer boxedInteger = 42;
    public Double boxedDouble = 3.5;
    public Object object = 42;
    public LocalDate localDate = LocalDate.of(2026, 10, 9);
    public BigDecimal bigDecimal = new BigDecimal("1.50");
    public Map<String, Object> map = Map.of("key", 42);
    public Mood mood = Mood.HAPPY;
    public Level level = Level.LOW;
    @Grouped
    public Integer groupedNumber = 42;
    public Label label = new Label();
    public Charset charset = StandardCharsets.UTF_8;
    public List<Integer> list;
  }

  public static class Label {
    @Override
    public String toString() { return "label"; }
  }

  @Test
  void referenceTypesAndNullTransitions() {
    var operands = new Operands();
    for (String property : List.of("boxedDouble", "localDate", "bigDecimal")) {
      var expression = new SpelExpressionParser().parseRaw("'value: ' + " + property);
      String expected = switch (property) {
        case "boxedDouble" -> "value: 3.5";
        case "localDate" -> "value: 2026-10-09";
        default -> "value: 1.50";
      };
      assertThat(expression.getValue(operands)).isEqualTo(expected);
      assertThat(expression.compileExpression()).isTrue();
      assertThat(expression.getValue(operands)).isEqualTo(expected);
      try {
        Operands.class.getField(property).set(operands, null);
      }
      catch (ReflectiveOperationException ex) {
        throw new AssertionError(ex);
      }
      assertThat(expression.getValue(operands)).isEqualTo("value: null");
    }
    var expression = new SpelExpressionParser().parseRaw("label + ' :value'");
    assertThat(expression.getValue(operands)).isEqualTo("label :value");
    assertThat(expression.compileExpression()).isTrue();
    assertThat(expression.getValue(operands)).isEqualTo("label :value");
    expression = new SpelExpressionParser().parseRaw("'value: ' + null");
    assertThat(expression.getValue()).isEqualTo("value: null");
    assertThat(expression.compileExpression()).isFalse();
    operands.object = null;
    expression = new SpelExpressionParser().parseRaw("'value: ' + object");
    assertThat(expression.getValue(operands)).isEqualTo("value: null");
    assertThat(expression.compileExpression()).isFalse();
    operands.object = List.of(1, 2);
    assertThat(expression.getValue(operands)).isEqualTo("value: 1,2");
    assertThat(expression.compileExpression()).isFalse();
    var mixed = new SpelExpressionParser(SpelParserConfiguration.builder().compilerMode(SpelCompilerMode.MIXED).build())
            .parseRaw("object + ' :value'");
    operands.object = 42;
    assertThat(mixed.getValue(operands)).isEqualTo("42 :value");
    assertThat(mixed.compileExpression()).isTrue();
    operands.object = List.of(1, 2);
    assertThat(mixed.getValue(operands)).isEqualTo("1,2 :value");
  }

  public enum Mood {
    HAPPY,
    SAD {
      @Override
      public String toString() { return "sad"; }
    },
    CALM {
      @Override
      boolean isCalm() { return true; }
    };
    boolean isCalm() { return false; }
  }

  public enum Level {
    LOW, HIGH;
    @Override
    public String toString() { return this == HIGH ? "high" : name(); }
  }

  @java.lang.annotation.Target(java.lang.annotation.ElementType.FIELD)
  @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
  @interface Grouped { }

  /**
   * Formats an Integer annotated with Grouped using grouping separators.
   */
  private static class GroupedNumberConverter implements infra.core.conversion.ConditionalGenericConverter {
    @Override
    public java.util.Set<ConvertiblePair> getConvertibleTypes() {
      return java.util.Set.of(new ConvertiblePair(Integer.class, String.class));
    }
    @Override
    public boolean matches(infra.core.TypeDescriptor source, infra.core.TypeDescriptor target) {
      return source.hasAnnotation(Grouped.class);
    }
    @Override
    public Object convert(Object source, infra.core.TypeDescriptor sourceType, infra.core.TypeDescriptor targetType) {
      return String.format(java.util.Locale.US, "%,d", source);
    }
  }

  @Test
  void annotatedOperandPreventsValueDependentFormattingFromBeingCompiled() {
    var service = new DefaultConversionService();
    service.addConverter(new GroupedNumberConverter());
    var operands = new Operands();
    var context = new StandardEvaluationContext(operands);
    context.setTypeConverter(new StandardTypeConverter(service));
    var expression = new SpelExpressionParser().parseRaw("'value: ' + groupedNumber");
    assertThat(expression.getValue(context)).isEqualTo("value: 42");
    assertThat(expression.compileExpression()).isFalse();
    operands.groupedNumber = 1_234_567;
    assertThat(expression.getValue(context)).isEqualTo("value: 1,234,567");
  }

  @Test
  void enumTypesAndDifferingToStringPreventUnsafeCompilation() {
    var operands = new Operands();
    var expression = new SpelExpressionParser().parseRaw("'value: ' + mood");
    assertThat(expression.getValue(operands)).isEqualTo("value: HAPPY");
    assertThat(expression.compileExpression()).isTrue();
    assertThat(expression.getValue(operands)).isEqualTo("value: HAPPY");
    operands.mood = Mood.SAD;
    var compiled = expression;
    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() -> compiled.getValue(operands))
            .withCauseInstanceOf(IllegalStateException.class);
    expression = new SpelExpressionParser().parseRaw("'value: ' + mood");
    assertThat(expression.getValue(operands)).isEqualTo("value: SAD");
    assertThat(expression.compileExpression()).isFalse();
    operands.mood = Mood.CALM;
    expression = new SpelExpressionParser().parseRaw("'value: ' + mood");
    assertThat(expression.getValue(operands)).isEqualTo("value: CALM");
    assertThat(expression.compileExpression()).isFalse();
  }

  public static class InvocationCountingToString {
    int invocations;
    @Override
    public String toString() { this.invocations++; return "operand"; }
  }

  private static class CustomTypeConverter extends StandardTypeConverter {
    CustomTypeConverter(infra.core.conversion.ConversionService service) { super(service); }
  }

  @Test
  void changedConverterAndValueDependentConversionPreventCompilation() {
    var operands = new Operands();
    var service = new DefaultConversionService();
    service.addConverter(Integer.class, String.class, value -> "int-" + value);
    var custom = new StandardEvaluationContext(operands);
    custom.setTypeConverter(new CustomTypeConverter(service));
    var expression = new SpelExpressionParser().parseRaw("'value: ' + primitiveInt");
    assertThat(expression.getValue(new StandardEvaluationContext(operands))).isEqualTo("value: 42");
    assertThat(expression.getValue(custom)).isEqualTo("value: int-42");
    assertThat(expression.compileExpression()).isFalse();
    var mixedParser = new SpelExpressionParser(SpelParserConfiguration.builder().compilerMode(SpelCompilerMode.MIXED).build());
    custom.setTypeConverter(new StandardTypeConverter(service));
    expression = mixedParser.parseRaw("'value: ' + primitiveInt");
    assertThat(expression.getValue(new StandardEvaluationContext(operands))).isEqualTo("value: 42");
    assertThat(expression.getValue(custom)).isEqualTo("value: int-42");
    assertThat(expression.compileExpression()).isFalse();
    expression = mixedParser.parseRaw("'level: ' + level");
    assertThat(expression.getValue(operands)).isEqualTo("level: LOW");
    operands.level = Level.HIGH;
    assertThat(expression.getValue(operands)).isEqualTo("level: HIGH");
    assertThat(expression.compileExpression()).isFalse();
  }

  @Test
  void conversionComparisonOnlyOccursWhenNecessary() {
    var service = new DefaultConversionService();
    service.addConverter(InvocationCountingToString.class, String.class, Object::toString);
    var operand = new InvocationCountingToString();
    for (SpelCompilerMode mode : List.of(SpelCompilerMode.OFF, SpelCompilerMode.MIXED)) {
      operand.invocations = 0;
      var expression = new SpelExpressionParser(SpelParserConfiguration.builder().compilerMode(mode).build())
              .parseRaw("'value: ' + #operand");
      for (int i = 0; i < 3; i++) {
        var context = new StandardEvaluationContext();
        context.setTypeConverter(new StandardTypeConverter(service));
        context.setVariable("operand", operand);
        assertThat(expression.getValue(context)).isEqualTo("value: operand");
      }
      assertThat(operand.invocations).isEqualTo(mode == SpelCompilerMode.OFF ? 4 : 6);
    }
    operand.invocations = 0;
    var expression = new SpelExpressionParser(SpelParserConfiguration.builder().compilerMode(SpelCompilerMode.MIXED).build())
            .parseRaw("'value: ' + #operand");
    for (int i = 0; i < 3; i++) {
      var context = infra.expression.spel.support.SimpleEvaluationContext.forReadOnlyDataBinding()
              .withConversionService(service).build();
      context.setVariable("operand", operand);
      assertThat(expression.getValue(context)).isEqualTo("value: operand");
    }
    assertThat(operand.invocations).isEqualTo(4);
  }

  @Test
  void inaccessibleAndDifferingReferenceConversionsCannotCompile() {
    var operands = new Operands();
    var expression = new SpelExpressionParser().parseRaw("'value: ' + list");
    assertThat(expression.getValue(operands)).isEqualTo("value: null");
    assertThat(expression.compileExpression()).isFalse();
    operands.list = List.of(1, 2);
    assertThat(expression.getValue(operands)).isEqualTo("value: 1,2");
    assertThat(expression.compileExpression()).isFalse();

    assertThat(operands.charset.getClass().getPackageName()).isEqualTo("sun.nio.cs");
    expression = new SpelExpressionParser().parseRaw("'value: ' + charset");
    assertThat(expression.getValue(operands)).isEqualTo("value: UTF-8");
    assertThat(expression.compileExpression()).isFalse();
  }

  @Test
  void lastObservedTypeAndStringOperandAreUsed() {
    var operands = new Operands();
    var expression = new SpelExpressionParser().parseRaw("'[' + object + ']'");
    assertThat(expression.getValue(operands)).isEqualTo("[42]");
    operands.object = 42L;
    assertThat(expression.getValue(operands)).isEqualTo("[42]");
    assertThat(expression.compileExpression()).isTrue();
    assertThat(expression.getValue(operands)).isEqualTo("[42]");
    expression = new SpelExpressionParser().parseRaw("'[' + object + ']'");
    assertThat(expression.getValue(operands)).isEqualTo("[42]");
    operands.object = "text";
    assertThat(expression.getValue(operands)).isEqualTo("[text]");
    assertThat(expression.compileExpression()).isTrue();
    assertThat(expression.getValue(operands)).isEqualTo("[text]");

    expression = new SpelExpressionParser().parseRaw("'value: ' + object");
    assertThat(expression.getValue(operands)).isEqualTo("value: text");
    assertThat(expression.compileExpression()).isTrue();
    assertThat(expression.getValue(operands)).isEqualTo("value: text");
  }

  @Test
  void variablesAndMethodReturningIntCompile() {
    var context = new StandardEvaluationContext();
    context.setVariable("num", 42);
    var expression = new SpelExpressionParser().parseRaw("'value: ' + #num");
    assertThat(expression.getValue(context)).isEqualTo("value: 42");
    assertThat(expression.compileExpression()).isTrue();
    assertThat(expression.getValue(context)).isEqualTo("value: 42");
    context.setVariable("first", "Jane");
    context.setVariable("last", "Smith");
    expression = new SpelExpressionParser().parseRaw("(#first + ' ' + #last).toUpperCase() + ' (' + (#first + ' ' + #last).length() + ')'");
    assertThat(expression.getValue(context)).isEqualTo("JANE SMITH (10)");
    assertThat(expression.compileExpression()).isTrue();
    assertThat(expression.getValue(context)).isEqualTo("JANE SMITH (10)");
  }
}
