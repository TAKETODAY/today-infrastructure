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

package infra.context.expression;

import java.util.concurrent.ConcurrentHashMap;

import infra.beans.BeansException;
import infra.beans.factory.BeanExpressionException;
import infra.beans.factory.config.BeanExpressionContext;
import infra.beans.factory.config.BeanExpressionResolver;
import infra.core.conversion.ConversionService;
import infra.expression.Expression;
import infra.expression.ExpressionParser;
import infra.expression.ParserContext;
import infra.expression.spel.SpelParserConfiguration;
import infra.expression.spel.standard.SpelExpressionParser;
import infra.expression.spel.support.StandardEvaluationContext;
import infra.expression.spel.support.StandardTypeConverter;
import infra.expression.spel.support.StandardTypeLocator;
import infra.format.support.ApplicationConversionService;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.lang.TodayStrategies;
import infra.util.StringUtils;

/**
 * Standard implementation of the {@link BeanExpressionResolver} interface,
 * parsing and evaluating EL using {@code infra.expression} module.
 *
 * <p>All beans in the containing {@code BeanFactory} are made available as
 * predefined variables with their common bean name, including standard context
 * beans such as "environment", "systemProperties" and "systemEnvironment".
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanExpressionContext#beanFactory
 * @see ExpressionParser
 * @see SpelExpressionParser
 * @see StandardEvaluationContext
 * @since 4.0 2021/12/25 15:01
 */
public class StandardBeanExpressionResolver implements BeanExpressionResolver, ParserContext {

  /**
   * System property to configure the maximum length for SpEL expressions: {@value}.
   * <p>Can also be configured via the {@link TodayStrategies} mechanism.
   *
   * @see SpelParserConfiguration#getMaximumExpressionLength()
   */
  public static final String MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME = "spel.context.max-length";

  /** Default expression prefix: "#{". */
  public static final String DEFAULT_EXPRESSION_PREFIX = "#{";

  /** Default expression suffix: "}". */
  public static final String DEFAULT_EXPRESSION_SUFFIX = "}";

  private String expressionPrefix = DEFAULT_EXPRESSION_PREFIX;

  private String expressionSuffix = DEFAULT_EXPRESSION_SUFFIX;

  private ExpressionParser expressionParser;

  private final ConcurrentHashMap<String, Expression> expressionCache = new ConcurrentHashMap<>(256);

  private final ConcurrentHashMap<BeanExpressionContext, StandardEvaluationContext> evaluationCache = new ConcurrentHashMap<>(8);

  /**
   * Create a new {@code StandardBeanExpressionResolver} with default settings.
   */
  public StandardBeanExpressionResolver() {
    this.expressionParser = SpelExpressionParser.INSTANCE;
  }

  /**
   * Create a new {@code StandardBeanExpressionResolver} with the given bean class loader,
   * using it as the basis for expression compilation.
   *
   * @param beanClassLoader the factory's bean class loader
   */
  public StandardBeanExpressionResolver(@Nullable ClassLoader beanClassLoader) {
    SpelParserConfiguration parserConfig = new SpelParserConfiguration(
            null, beanClassLoader, false, false, Integer.MAX_VALUE, retrieveMaxExpressionLength());
    this.expressionParser = new SpelExpressionParser(parserConfig);
  }

  /**
   * Set the prefix that an expression string starts with.
   * The default is "#{".
   *
   * @see #DEFAULT_EXPRESSION_PREFIX
   */
  public void setExpressionPrefix(String expressionPrefix) {
    Assert.hasText(expressionPrefix, "Expression prefix must not be empty");
    this.expressionPrefix = expressionPrefix;
  }

  /**
   * Set the suffix that an expression string ends with.
   * The default is "}".
   *
   * @see #DEFAULT_EXPRESSION_SUFFIX
   */
  public void setExpressionSuffix(String expressionSuffix) {
    Assert.hasText(expressionSuffix, "Expression suffix must not be empty");
    this.expressionSuffix = expressionSuffix;
  }

  /**
   * Specify the EL parser to use for expression parsing.
   * <p>Default is a {@link SpelExpressionParser},
   * compatible with standard Unified EL style expression syntax.
   */
  public void setExpressionParser(ExpressionParser expressionParser) {
    Assert.notNull(expressionParser, "ExpressionParser is required");
    this.expressionParser = expressionParser;
  }

  @Override
  public boolean isTemplate() {
    return true;
  }

  @Override
  public String getExpressionPrefix() {
    return expressionPrefix;
  }

  @Override
  public String getExpressionSuffix() {
    return expressionSuffix;
  }

  @Override
  @Nullable
  public Object evaluate(@Nullable String value, BeanExpressionContext evalContext) throws BeansException {
    if (StringUtils.isEmpty(value)) {
      return value;
    }
    try {
      Expression expr = expressionCache.get(value);
      if (expr == null) {
        expr = expressionParser.parseExpression(value, this);
        expressionCache.put(value, expr);
      }
      StandardEvaluationContext sec = evaluationCache.get(evalContext);
      if (sec == null) {
        sec = new StandardEvaluationContext(evalContext);
        sec.addPropertyAccessor(new BeanExpressionContextAccessor());
        sec.addPropertyAccessor(new BeanFactoryAccessor());
        sec.addPropertyAccessor(new MapAccessor());
        sec.addPropertyAccessor(new EnvironmentAccessor());
        sec.setBeanResolver(new BeanFactoryResolver(evalContext.beanFactory));
        sec.setTypeLocator(new StandardTypeLocator(evalContext.beanFactory.getBeanClassLoader()));
        sec.setTypeConverter(new StandardTypeConverter(() -> {
          ConversionService cs = evalContext.beanFactory.getConversionService();
          return cs != null ? cs : ApplicationConversionService.getSharedInstance();
        }));
        customizeEvaluationContext(sec);
        evaluationCache.put(evalContext, sec);
      }
      return expr.getValue(sec);
    }
    catch (Throwable ex) {
      throw new BeanExpressionException("Expression parsing failed", ex);
    }
  }

  /**
   * Template method for customizing the expression evaluation context.
   * <p>The default implementation is empty.
   */
  protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {

  }

  private static int retrieveMaxExpressionLength() {
    String value = TodayStrategies.getProperty(MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME);
    if (StringUtils.isBlank(value)) {
      return SpelParserConfiguration.DEFAULT_MAX_EXPRESSION_LENGTH;
    }

    try {
      int maxLength = Integer.parseInt(value.trim());
      if (maxLength < 1) {
        throw new IllegalArgumentException("Value [%d] for system property [%s] must be positive"
                .formatted(maxLength, MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME));
      }
      return maxLength;
    }
    catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Failed to parse value for system property [%s]: %s"
              .formatted(MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME, ex.getMessage()), ex);
    }
  }

}
