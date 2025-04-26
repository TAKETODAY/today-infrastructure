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

import infra.beans.factory.BeanExpressionException;
import infra.beans.factory.config.BeanExpressionContext;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.core.conversion.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/2 15:05
 */
class StandardBeanExpressionResolverTests {

  @Test
  void evaluateEmptyExpressionReturnsOriginalValue() {
    StandardBeanExpressionResolver resolver = new StandardBeanExpressionResolver();
    BeanExpressionContext context = mock(BeanExpressionContext.class);

    assertThat((String) resolver.evaluate("", context)).isEmpty();
    assertThat(resolver.evaluate(null, context)).isNull();
  }

  @Test
  void evaluateSimpleExpression() {
    StandardBeanExpressionResolver resolver = new StandardBeanExpressionResolver();
    ConfigurableBeanFactory beanFactory = mock(ConfigurableBeanFactory.class);
    BeanExpressionContext context = new BeanExpressionContext(beanFactory, null);

    Object result = resolver.evaluate("#{1 + 1}", context);
    assertThat(result).isEqualTo(2);
  }

  @Test
  void evaluateExpressionWithCustomPrefixAndSuffix() {
    StandardBeanExpressionResolver resolver = new StandardBeanExpressionResolver();
    resolver.setExpressionPrefix("${");
    resolver.setExpressionSuffix("}");
    ConfigurableBeanFactory beanFactory = mock(ConfigurableBeanFactory.class);
    BeanExpressionContext context = new BeanExpressionContext(beanFactory, null);

    Object result = resolver.evaluate("${1 + 2}", context);
    assertThat(result).isEqualTo(3);
  }

  @Test
  void setEmptyPrefixThrowsException() {
    StandardBeanExpressionResolver resolver = new StandardBeanExpressionResolver();

    assertThatIllegalArgumentException()
            .isThrownBy(() -> resolver.setExpressionPrefix(""))
            .withMessage("Expression prefix must not be empty");
  }

  @Test
  void setEmptySuffixThrowsException() {
    StandardBeanExpressionResolver resolver = new StandardBeanExpressionResolver();

    assertThatIllegalArgumentException()
            .isThrownBy(() -> resolver.setExpressionSuffix(""))
            .withMessage("Expression suffix must not be empty");
  }

  @Test
  void setNullParserThrowsException() {
    StandardBeanExpressionResolver resolver = new StandardBeanExpressionResolver();

    assertThatIllegalArgumentException()
            .isThrownBy(() -> resolver.setExpressionParser(null))
            .withMessage("ExpressionParser is required");
  }

  @Test
  void invalidExpressionThrowsBeanExpressionException() {
    StandardBeanExpressionResolver resolver = new StandardBeanExpressionResolver();
    BeanExpressionContext context = mock(BeanExpressionContext.class);

    assertThatExceptionOfType(BeanExpressionException.class)
            .isThrownBy(() -> resolver.evaluate("#{invalid", context))
            .withMessageContaining("Expression parsing failed");
  }

  @Test
  void expressionCacheReusesParsedExpressions() {
    StandardBeanExpressionResolver resolver = new StandardBeanExpressionResolver();

    ConfigurableBeanFactory beanFactory = mock(ConfigurableBeanFactory.class);
    BeanExpressionContext context = new BeanExpressionContext(beanFactory, null);

    Object result1 = resolver.evaluate("#{1 + 1}", context);
    Object result2 = resolver.evaluate("#{1 + 1}", context);

    assertThat(result1).isEqualTo(result2);
  }

  @Test
  void evaluateWithMaxExpressionLength() {
    System.setProperty(StandardBeanExpressionResolver.MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME, "5");
    StandardBeanExpressionResolver resolver = new StandardBeanExpressionResolver(getClass().getClassLoader());
    BeanExpressionContext context = mock(BeanExpressionContext.class);

    assertThatExceptionOfType(BeanExpressionException.class)
            .isThrownBy(() -> resolver.evaluate("#{'toolong'}", context));

    System.clearProperty(StandardBeanExpressionResolver.MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME);
  }

  @Test
  void evaluateWithCustomTypeConverter() {
    StandardBeanExpressionResolver resolver = new StandardBeanExpressionResolver();
    ConfigurableBeanFactory beanFactory = mock(ConfigurableBeanFactory.class);
    BeanExpressionContext context = new BeanExpressionContext(beanFactory, null);

    ConversionService conversionService = mock(ConversionService.class);

    when(beanFactory.getConversionService()).thenReturn(conversionService);
    when(conversionService.canConvert(Integer.class, String.class)).thenReturn(true);
    when(conversionService.convert(1, String.class)).thenReturn("1");

    Object result = resolver.evaluate("#{'1'}", context);
    assertThat(result).isEqualTo("1");
  }

  @Test
  void evaluateWithInvalidMaxExpressionLength() {
    System.setProperty(StandardBeanExpressionResolver.MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME, "invalid");

    assertThatIllegalArgumentException()
            .isThrownBy(() -> new StandardBeanExpressionResolver(getClass().getClassLoader()))
            .withMessageContaining("Failed to parse value for system property");

    System.clearProperty(StandardBeanExpressionResolver.MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME);
  }

  @Test
  void evaluateWithNegativeMaxExpressionLength() {
    System.setProperty(StandardBeanExpressionResolver.MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME, "-1");

    assertThatIllegalArgumentException()
            .isThrownBy(() -> new StandardBeanExpressionResolver(getClass().getClassLoader()))
            .withMessageContaining("must be positive");

    System.clearProperty(StandardBeanExpressionResolver.MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME);
  }

  @Test
  void evaluateExpressionWithBeanReference() {
    StandardBeanExpressionResolver resolver = new StandardBeanExpressionResolver();
    TestBean testBean = new TestBean();

    ConfigurableBeanFactory beanFactory = mock(ConfigurableBeanFactory.class);
    BeanExpressionContext context = new BeanExpressionContext(beanFactory, null);

    when(beanFactory.containsBean("testBean")).thenReturn(true);
    when(beanFactory.getBean("testBean")).thenReturn(testBean);

    Object result = resolver.evaluate("#{testBean}", context);
    assertThat(result).isSameAs(testBean);
  }

  @Test
  void parserContextBehavior() {
    StandardBeanExpressionResolver resolver = new StandardBeanExpressionResolver();

    assertThat(resolver.isTemplate()).isTrue();
    assertThat(resolver.getExpressionPrefix()).isEqualTo("#{");
    assertThat(resolver.getExpressionSuffix()).isEqualTo("}");
  }

  private static class TestBean {
  }

}