/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.expression;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextHolder;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.env.PropertiesPropertyResolver;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.ExpressionFactory;
import cn.taketoday.expression.ExpressionManager;
import cn.taketoday.expression.ExpressionProcessor;
import cn.taketoday.expression.StandardExpressionContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Env;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Value;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.PlaceholderResolver;
import cn.taketoday.util.PropertyPlaceholderHandler;
import cn.taketoday.util.StringUtils;

/**
 * Expression Evaluator
 *
 * @author TODAY 2021/4/8 19:42
 * @since 3.0
 */
public class ExpressionEvaluator implements PlaceholderResolver {
  // @since 4.0
  private static volatile ExpressionEvaluator sharedInstance;

  public static final String ENV = "env";
  public static final String EL_PREFIX = "#{";

  private static final Logger log = LoggerFactory.getLogger(ExpressionEvaluator.class);

  @Nullable
  private ConversionService conversionService;

  private ExpressionProcessor expressionProcessor;

  @Nullable
  private ApplicationContext context;

  private boolean throwIfPropertyNotFound = false;

  private final PropertyResolver variablesResolver;

  public ExpressionEvaluator() {
    this.variablesResolver = new StandardEnvironment();
  }

  public ExpressionEvaluator(PropertyResolver variablesResolver) {
    this.variablesResolver = variablesResolver;
  }

  public ExpressionEvaluator(Properties variables) {
    this.variablesResolver = new PropertiesPropertyResolver(variables);
  }

  public ExpressionEvaluator(ExpressionProcessor expressionProcessor) {
    this(expressionProcessor, System.getProperties());
  }

  public ExpressionEvaluator(ExpressionProcessor expressionProcessor, Properties variables) {
    this(variables);
    Assert.notNull(expressionProcessor, "ExpressionProcessor must not be null");
    this.expressionProcessor = expressionProcessor;
  }

  /**
   * Create a ExpressionEvaluator with ApplicationContext
   * use Environment as variablesResolver
   *
   * @param context
   *         ApplicationContext must not be null
   */
  public ExpressionEvaluator(@NonNull ApplicationContext context) {
    Assert.notNull(context, "ApplicationContext must not be null");
    this.context = context;
    this.variablesResolver = context.getEnvironment();
  }

  public ExpressionEvaluator(ApplicationContext context, ExpressionProcessor expressionProcessor) {
    this(context);
    this.expressionProcessor = expressionProcessor;
  }

  public Object evaluate(String expression) {
    return obtainProcessor().eval(expression);
  }

  public <T> T evaluate(String expression, Class<T> expectedType) {
    expression = resolvePlaceholders(expression, throwIfPropertyNotFound);
    if (expression.contains(EL_PREFIX)) {
      try {
        expression = obtainProcessor().getValue(expression, null);
      }
      catch (ExpressionException e) {
        throw new ExpressionEvaluationException(e);
      }
    }
    return convertIfNecessary(expression, expectedType);
  }

  public <T> T evaluate(String expression, Class<T> expectedType, Map<String, String> variables) {
    return evaluate(expression, expectedType, variables::get);
  }

  public <T> T evaluate(String expression, Class<T> expectedType, PlaceholderResolver resolver) {
    expression = resolvePlaceholders(expression, resolver, throwIfPropertyNotFound);
    if (expression.contains(EL_PREFIX)) {
      try {
        expression = obtainProcessor().getValue(expression, null);
      }
      catch (ExpressionException e) {
        throw new ExpressionEvaluationException(e);
      }
    }
    return convertIfNecessary(expression, expectedType);
  }

  /**
   * replace Placeholders first and evaluate EL
   */
  public <T> T evaluate(
          String expression, ExpressionContext context, Class<T> expectedType) {
    expression = resolvePlaceholders(expression, throwIfPropertyNotFound);
    if (expression.contains(EL_PREFIX)) {
      try {
        expression = obtainProcessor().getValue(expression, context, null);
      }
      catch (ExpressionException e) {
        throw new ExpressionEvaluationException(e);
      }
    }
    return convertIfNecessary(expression, expectedType);
  }

  /**
   * Resolve {@link Env} {@link Annotation}
   *
   * @param value
   *         {@link Env} {@link Annotation}
   * @param expectedType
   *         expected value type
   *
   * @return A resolved value object
   *
   * @throws ExpressionEvaluationException
   *         Can't resolve expression
   * @since 2.1.6
   */
  public <T> T resolvePlaceholders(Env value, Class<T> expectedType) {
    String replaced = resolvePlaceholders(value.value(), throwIfPropertyNotFound);
    if (replaced != null) {
      return convertIfNecessary(replaced, expectedType);
    }
    if (value.required()) {
      throw new ExpressionEvaluationException("Can't resolve property: [" + value.value() + "]");
    }

    String defaultValue = value.defaultValue();
    if (StringUtils.isEmpty(defaultValue)) {
      return null;
    }
    return evaluate(defaultValue, expectedType);
  }

  /**
   * Resolve {@link Value} {@link Annotation}
   *
   * @param value
   *         {@link Value} {@link Annotation}
   * @param expectedType
   *         expected value type
   *
   * @return A resolved value object
   *
   * @throws ExpressionEvaluationException
   *         Can't resolve expression
   * @since 2.1.6
   */
  public <T> T evaluate(Value value, Class<T> expectedType) {
    T resolveValue = evaluate(value.value(), expectedType);
    if (resolveValue != null) {
      return resolveValue;
    }
    if (value.required()) {
      throw new ExpressionEvaluationException("Can't resolve expression: [" + value.value() + "]");
    }
    String defaultValue = value.defaultValue();
    if (StringUtils.isEmpty(defaultValue)) {
      return null;
    }
    return evaluate(defaultValue, expectedType);
  }

  /**
   * this method is
   *
   * @param expr
   * @param expectedType
   * @param <T>
   *
   * @return
   */
  @Nullable
  public <T> T evaluate(ExpressionInfo expr, Class<T> expectedType) {
    if (expr.isPlaceholderOnly()) {
      // find in env
      String property = variablesResolver.getProperty(expr.getExpression());
      if (property != null) {
        return convertIfNecessary(property, expectedType);
      }
      if (expr.isRequired()) {
        throw new ExpressionEvaluationException(
                "Can't resolve property: [" + expr.getExpression() + "] in PropertyResolver: " + variablesResolver);
      }
      else {
        // try evaluate default-value-expr
        String defaultValueExpr = expr.getDefaultValue();
        if (StringUtils.isNotEmpty(defaultValueExpr)) {
          try {
            return evaluate(defaultValueExpr, expectedType);
          }
          catch (ExpressionEvaluationException e) {
            // required check
            if (expr.isRequired()) {
              throw e;
            }
            else {
              // not required returns null
              return null;
            }
          }
        }
        return null;
      }
    }
    else {
      try {
        return evaluate(expr.getExpression(), expectedType);
      }
      catch (ExpressionEvaluationException e) {
        // required check
        if (expr.isRequired()) {
          throw e;
        }
        else {
          // not required evaluate default
          String defaultValueExpr = expr.getDefaultValue();
          if (StringUtils.isEmpty(defaultValueExpr)) {
            return null;
          }
          return evaluate(defaultValueExpr, expectedType);
        }
      }
    }
  }

  //---------------------------------------------------------------------
  // resolvePlaceholders
  //---------------------------------------------------------------------

  public String resolvePlaceholders(String input, Map<String, String> properties) {
    return resolvePlaceholders(input, properties, throwIfPropertyNotFound);
  }

  public String resolvePlaceholders(
          String input, Map<String, String> properties, boolean throwIfPropertyNotFound) {
    return resolvePlaceholders(input, properties::get, throwIfPropertyNotFound);
  }

  /**
   * @since 4.0
   */
  public String resolvePlaceholders(
          String input, PlaceholderResolver resolver, boolean throwIfPropertyNotFound) {
    PropertyPlaceholderHandler placeholderHandler = PropertyPlaceholderHandler.shared(!throwIfPropertyNotFound);
    return placeholderHandler.replacePlaceholders(input, resolver);
  }

  public String resolvePlaceholders(String input) {
    return resolvePlaceholders(input, throwIfPropertyNotFound);
  }

  public String resolvePlaceholders(
          String input, boolean throwIfPropertyNotFound) {
    if (throwIfPropertyNotFound) {
      return variablesResolver.resolveRequiredPlaceholders(input);
    }
    return variablesResolver.resolvePlaceholders(input);
  }

  /**
   * @since 4.0
   */
  @Nullable
  @Override
  public String resolvePlaceholder(String placeholderName) {
    return variablesResolver.getProperty(placeholderName);
  }

  /**
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  private <T> T convertIfNecessary(Object value, Class<T> requiredType) {
    if (requiredType.isInstance(value)) {
      return (T) value;
    }
    if (conversionService == null) {
      conversionService = DefaultConversionService.getSharedInstance();
    }
    return conversionService.convert(value, requiredType);
  }

  public void setConversionService(@Nullable ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Nullable
  public ConversionService getConversionService() {
    return conversionService;
  }

  public void setThrowIfPropertyNotFound(boolean throwIfPropertyNotFound) {
    this.throwIfPropertyNotFound = throwIfPropertyNotFound;
  }

  public void setExpressionProcessor(ExpressionProcessor expressionProcessor) {
    this.expressionProcessor = expressionProcessor;
  }

  public ExpressionProcessor getExpressionProcessor() {
    return expressionProcessor;
  }

  public void setContext(@Nullable ApplicationContext context) {
    this.context = context;
  }

  @Nullable
  public ApplicationContext getContext() {
    return context;
  }

  @NonNull
  private ExpressionProcessor obtainProcessor() {
    if (expressionProcessor == null) {
      ExpressionFactory exprFactory = ExpressionFactory.getSharedInstance();
      ApplicationContext context = this.context;
      if (context == null) {
        context = ApplicationContextHolder.getLastStartupContext();
        if (context == null) {
          log.info("There isn't a global ApplicationContext");
        }
        else {
          log.info("Using global ApplicationContext {}", context);
        }
      }
      StandardExpressionContext globalContext;
      if (context == null) {
        globalContext = new StandardExpressionContext(exprFactory);
      }
      else {
        ExpressionProcessor processor = context.getBean(ExpressionProcessor.class);
        if (processor != null) {
          this.expressionProcessor = processor;
          return processor;
        }
        globalContext = new ValueExpressionContext(exprFactory, context.getBeanFactory());
      }

      globalContext.defineBean(ENV, variablesResolver);
      this.expressionProcessor = new ExpressionProcessor(new ExpressionManager(globalContext, exprFactory));
    }
    return expressionProcessor;
  }

  // static

  /**
   * @since 4.0
   */
  public static ExpressionEvaluator getSharedInstance() {
    ExpressionEvaluator evaluator = sharedInstance;
    if (evaluator == null) {
      synchronized(ExpressionEvaluator.class) {
        evaluator = sharedInstance;
        if (evaluator == null) {
          evaluator = new ExpressionEvaluator();
          sharedInstance = evaluator;
        }
      }
    }
    return evaluator;
  }

}
