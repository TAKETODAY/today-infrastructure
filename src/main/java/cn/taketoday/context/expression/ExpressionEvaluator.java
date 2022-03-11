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

import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.env.EnvironmentCapable;
import cn.taketoday.core.env.PropertiesPropertyResolver;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.expression.BeanNameExpressionResolver;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.ExpressionFactory;
import cn.taketoday.expression.ExpressionManager;
import cn.taketoday.expression.ExpressionProcessor;
import cn.taketoday.expression.StandardExpressionContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
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
  public static final String EXPRESSION_FACTORY_NAME = "expressionFactory";
  public static final String EXPRESSION_MANAGER_NAME = "expressionManager";
  public static final String EXPRESSION_PROCESSOR_NAME = "expressionProcessor";
  public static final String EXPRESSION_CONTEXT_NAME = "valueExpressionContext";

  // @since 4.0
  private static volatile ExpressionEvaluator sharedInstance;

  public static final String ENV = "env";
  public static final String EL_PREFIX = "#{";

  @Nullable
  private ConversionService conversionService;

  private ExpressionProcessor expressionProcessor;

  @Nullable
  private BeanFactory beanFactory;

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

  /**
   * Create a ExpressionEvaluator with EnvironmentCapable
   * use Environment as variablesResolver
   *
   * @param environmentCapable environmentCapable
   */
  public ExpressionEvaluator(@NonNull EnvironmentCapable environmentCapable) {
    Assert.notNull(environmentCapable, "EnvironmentCapable must not be null");
    this.variablesResolver = environmentCapable.getEnvironment();
  }

  public ExpressionEvaluator(EnvironmentCapable capable, ExpressionProcessor expressionProcessor) {
    this(capable);
    this.expressionProcessor = expressionProcessor;
  }

  public Object evaluate(String expression) {
    return obtainProcessor().eval(expression);
  }

  public <T> T evaluate(String expression, Class<T> expectedType) {
    expression = resolvePlaceholders(expression, throwIfPropertyNotFound);
    if (expression.contains(EL_PREFIX)) {
      try {
        Object evaluated = obtainProcessor().getValue(expression, null);
        return convertIfNecessary(evaluated, expectedType);
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
        Object evaluated = obtainProcessor().getValue(expression, null);
        return convertIfNecessary(evaluated, expectedType);
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
        Object evaluated = obtainProcessor().getValue(expression, context, null);
        return convertIfNecessary(evaluated, expectedType);
      }
      catch (ExpressionException e) {
        throw new ExpressionEvaluationException(e);
      }
    }
    return convertIfNecessary(expression, expectedType);
  }

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
    ConversionService conversionService = getConversionService();
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

  @Nullable
  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public void setBeanFactory(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  public ExpressionProcessor obtainProcessor() {
    if (expressionProcessor == null) {
      ExpressionProcessor processor = null;
      if (beanFactory != null) {
        processor = beanFactory.getBean(EXPRESSION_PROCESSOR_NAME, ExpressionProcessor.class);
        if (processor == null && beanFactory instanceof ConfigurableBeanFactory configurable) {
          register(configurable, variablesResolver);
          processor = beanFactory.getBean(EXPRESSION_PROCESSOR_NAME, ExpressionProcessor.class);
        }
      }
      if (processor == null) {
        processor = ExpressionProcessor.getSharedInstance();
      }
      this.expressionProcessor = processor;
    }
    return expressionProcessor;
  }

  /**
   * @since 4.0
   */
  public ExpressionContext getParentExpressionContext() {
    return obtainProcessor().getManager().getContext();
  }

  // static

  public static void register(ConfigurableBeanFactory beanFactory, PropertyResolver variablesResolver) {
    if (!beanFactory.containsLocalBean(EXPRESSION_PROCESSOR_NAME)) {
      // create shared elProcessor to singletons
      ExpressionFactory exprFactory = ExpressionFactory.getSharedInstance();
      StandardExpressionContext elContext = new StandardExpressionContext(exprFactory);
      elContext.setVariable(ExpressionEvaluator.ENV, variablesResolver); // @since 2.1.6
      elContext.addResolver(new BeanNameExpressionResolver(beanFactory));
      elContext.addResolver(new EnvironmentExpressionResolver());
      elContext.addResolver(new StandardTypeConverter());

      ExpressionManager elManager = new ExpressionManager(elContext, exprFactory);
      ExpressionProcessor elProcessor = new ExpressionProcessor(elManager);

      registerSingleton(beanFactory, EXPRESSION_CONTEXT_NAME, elContext);
      registerSingleton(beanFactory, EXPRESSION_MANAGER_NAME, elManager);
      registerSingleton(beanFactory, EXPRESSION_FACTORY_NAME, exprFactory);
      registerSingleton(beanFactory, EXPRESSION_PROCESSOR_NAME, elProcessor);
    }
  }

  private static void registerSingleton(ConfigurableBeanFactory beanFactory, String name, Object obj) {
    if (!beanFactory.containsLocalBean(name)) {
      beanFactory.registerSingleton(name, obj);
    }
  }

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
