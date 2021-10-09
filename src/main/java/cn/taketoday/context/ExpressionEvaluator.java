/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.ValueExpressionContext;
import cn.taketoday.core.Assert;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.env.PropertiesPropertyResolver;
import cn.taketoday.core.env.PropertiesPropertySource;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.PropertySourcesPropertyResolver;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.ExpressionFactory;
import cn.taketoday.expression.ExpressionManager;
import cn.taketoday.expression.ExpressionProcessor;
import cn.taketoday.expression.StandardExpressionContext;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
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
  public static final String ENV = "env";
  public static final String EL_PREFIX = "${";
  public static final String PLACE_HOLDER_PREFIX = "#{";
  public static final char PLACE_HOLDER_SUFFIX = '}';

  private static final Logger log = LoggerFactory.getLogger(ExpressionEvaluator.class);

  private ConversionService conversionService = DefaultConversionService.getSharedInstance();

  private ExpressionProcessor expressionProcessor;

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

  public ExpressionEvaluator(ApplicationContext context) {
    this(context, context.getBean(ExpressionProcessor.class));
  }

  public ExpressionEvaluator(ApplicationContext context, ExpressionProcessor expressionProcessor) {
    Assert.notNull(context, "ApplicationContext must not be null");
    this.context = context;
    this.expressionProcessor = expressionProcessor;
    this.variablesResolver = context.getEnvironment();
  }

  public Object evaluate(String expression) {
    return obtainProcessor().eval(expression);
  }

  public <T> T evaluate(String expression, Class<T> expectedType) {
    return evaluate(expression, expectedType, this);
  }

  public <T> T evaluate(String expression, Class<T> expectedType, Map<String, String> variables) {
    return evaluate(expression, expectedType, variables::get);
  }

  public <T> T evaluate(String expression, Class<T> expectedType, PlaceholderResolver resolver) {
    if (expression.contains(PLACE_HOLDER_PREFIX)) {
      String replaced = resolvePlaceholders(expression, resolver, throwIfPropertyNotFound);
      return conversionService.convert(replaced, expectedType);
    }
    if (expression.contains(EL_PREFIX)) {
      try {
        return obtainProcessor().getValue(expression, expectedType);
      }
      catch (ExpressionException e) {
        throw new ConfigurationException(e);
      }
    }
    return conversionService.convert(expression, expectedType);
  }

  public <T> T evaluate(
          String expression, ExpressionContext context, Class<T> expectedType) {
    if (expression.contains(PLACE_HOLDER_PREFIX)) {
      String replaced = resolvePlaceholders(expression, this, throwIfPropertyNotFound);
      return conversionService.convert(replaced, expectedType);
    }
    if (expression.contains(EL_PREFIX)) {
      try {
        return obtainProcessor().getValue(expression, context, expectedType);
      }
      catch (ExpressionException e) {
        throw new ConfigurationException(e);
      }
    }
    return conversionService.convert(expression, expectedType);
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
   * @throws ConfigurationException
   *         Can't resolve expression
   * @since 2.1.6
   */
  public <T> T evaluate(Env value, Class<T> expectedType) {
    T resolveValue = evaluate(
            PLACE_HOLDER_PREFIX + value.value() + PLACE_HOLDER_SUFFIX, expectedType
    );
    if (resolveValue != null) {
      return resolveValue;
    }
    if (value.required()) {
      throw new ConfigurationException("Can't resolve property: [" + value.value() + "]");
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
   * @throws ConfigurationException
   *         Can't resolve expression
   * @since 2.1.6
   */
  public <T> T evaluate(Value value, Class<T> expectedType) {
    T resolveValue = evaluate(value.value(), expectedType);
    if (resolveValue != null) {
      return resolveValue;
    }
    if (value.required()) {
      throw new ConfigurationException("Can't resolve expression: [" + value.value() + "]");
    }
    String defaultValue = value.defaultValue();
    if (StringUtils.isEmpty(defaultValue)) {
      return null;
    }
    return evaluate(defaultValue, expectedType);
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
    PropertyPlaceholderHandler placeholderHandler =
            throwIfPropertyNotFound ? PropertyPlaceholderHandler.strict : PropertyPlaceholderHandler.defaults;
    return placeholderHandler.replacePlaceholders(input, resolver);
  }

  /**
   * @since 4.0
   */
  @Nullable
  @Override
  public String resolvePlaceholder(String placeholderName) {
    return variablesResolver.getProperty(placeholderName);
  }

  public void setConversionService(ConversionService conversionService) {
    Assert.notNull(conversionService, "ConversionService must not be null");
    this.conversionService = conversionService;
  }

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

  public void setContext(ApplicationContext context) {
    this.context = context;
  }

  public ApplicationContext getContext() {
    return context;
  }

  private ExpressionProcessor obtainProcessor() {
    if (expressionProcessor == null) {
      ExpressionFactory exprFactory = ExpressionFactory.getSharedInstance();
      ApplicationContext context = this.context;
      if (context == null) {
        context = ContextUtils.getLastStartupContext();
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
        BeanFactory beanFactory = context.getBeanFactory();
        globalContext = new ValueExpressionContext(exprFactory, (ConfigurableBeanFactory) beanFactory);
      }

      globalContext.defineBean(ENV, variablesResolver);
      expressionProcessor = new ExpressionProcessor(new ExpressionManager(globalContext, exprFactory));
    }
    return expressionProcessor;
  }

}
