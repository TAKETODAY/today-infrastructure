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

package cn.taketoday.context.expression;

import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanExpressionException;
import cn.taketoday.beans.factory.config.BeanExpressionContext;
import cn.taketoday.beans.factory.config.BeanExpressionResolver;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.ExpressionParser;
import cn.taketoday.expression.ParserContext;
import cn.taketoday.expression.spel.SpelParserConfiguration;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;
import cn.taketoday.expression.spel.support.StandardTypeConverter;
import cn.taketoday.expression.spel.support.StandardTypeLocator;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Standard implementation of the {@link BeanExpressionResolver} interface,
 * parsing and evaluating EL using {@code cn.taketoday.expression} module.
 *
 * <p>All beans in the containing {@code BeanFactory} are made available as
 * predefined variables with their common bean name, including standard context
 * beans such as "environment", "systemProperties" and "systemEnvironment".
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanExpressionContext#getBeanFactory()
 * @see ExpressionParser
 * @see SpelExpressionParser
 * @see StandardEvaluationContext
 * @since 4.0 2021/12/25 15:01
 */
public class StandardBeanExpressionResolver implements BeanExpressionResolver, ParserContext {

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
    this.expressionParser = new SpelExpressionParser(new SpelParserConfiguration(null, beanClassLoader));
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
        sec.setBeanResolver(new BeanFactoryResolver(evalContext.getBeanFactory()));
        sec.setTypeLocator(new StandardTypeLocator(evalContext.getBeanFactory().getBeanClassLoader()));
        sec.setTypeConverter(new StandardTypeConverter(() -> {
          ConversionService cs = evalContext.getBeanFactory().getConversionService();
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

}
