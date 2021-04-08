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

import cn.taketoday.context.annotation.Env;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.conversion.support.DefaultConversionService;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.ConfigurableBeanFactory;
import cn.taketoday.context.factory.ValueExpressionContext;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.ExpressionFactory;
import cn.taketoday.expression.ExpressionManager;
import cn.taketoday.expression.ExpressionProcessor;
import cn.taketoday.expression.StandardExpressionContext;

/**
 * Expression Evaluator
 *
 * @author TODAY 2021/4/8 19:42
 * @since 3.0
 */
public class ExpressionEvaluator {
  private static final Logger log = LoggerFactory.getLogger(ExpressionEvaluator.class);

  private ConversionService conversionService = DefaultConversionService.getSharedInstance();

  private ExpressionProcessor expressionProcessor;

  private Properties variables;
  private boolean throwIfPropertyNotFound = false;
  private ApplicationContext context;

  public ExpressionEvaluator() {
    this(System.getProperties());
  }

  public ExpressionEvaluator(Properties variables) {
    setVariables(variables);
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
    final Environment environment = context.getEnvironment();
    this.variables = environment.getProperties();
    this.expressionProcessor = expressionProcessor != null ? expressionProcessor : environment.getExpressionProcessor();
  }

  public Object evaluate(String expression) {
    return expressionProcessor.eval(expression);
  }

  public <T> T evaluate(final String expression, final Class<T> expectedType) {
    return evaluate(expression, expectedType, variables);
  }

  public <T> T evaluate(final String expression, final Class<T> expectedType, final Map<Object, Object> variables) {
    if (expression.contains(Constant.PLACE_HOLDER_PREFIX)) {
      final String replaced = resolvePlaceholder(variables, expression);
      return conversionService.convert(replaced, expectedType);
    }
    if (expression.contains(Constant.EL_PREFIX)) {
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
          final String expression, final ExpressionContext context, final Class<T> expectedType) {

    if (expression.contains(Constant.PLACE_HOLDER_PREFIX)) {
      final String replaced = resolvePlaceholder(variables, expression);
      return conversionService.convert(replaced, expectedType);
    }
    if (expression.contains(Constant.EL_PREFIX)) {
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
  public <T> T evaluate(final Env value, final Class<T> expectedType) {
    final T resolveValue = evaluate(
            Constant.PLACE_HOLDER_PREFIX + value.value() + Constant.PLACE_HOLDER_SUFFIX, expectedType
    );
    if (resolveValue != null) {
      return resolveValue;
    }
    if (value.required()) {
      throw new ConfigurationException("Can't resolve property: [" + value.value() + "]");
    }

    final String defaultValue = value.defaultValue();
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
  public <T> T evaluate(final Value value, final Class<T> expectedType) {
    final T resolveValue = evaluate(value.value(), expectedType);
    if (resolveValue != null) {
      return resolveValue;
    }
    if (value.required()) {
      throw new ConfigurationException("Can't resolve expression: [" + value.value() + "]");
    }
    final String defaultValue = value.defaultValue();
    if (StringUtils.isEmpty(defaultValue)) {
      return null;
    }
    return evaluate(defaultValue, expectedType);
  }

  public String resolvePlaceholder(final Map<Object, Object> properties, String input) {
    return resolvePlaceholder(properties, input, throwIfPropertyNotFound);
  }

  /**
   * Resolve placeholder s
   *
   * @param properties
   *         {@link Properties} variables source
   * @param input
   *         Input expression
   * @param throwIfPropertyNotFound
   *         If there doesn't exist the key throw {@link Exception}
   *
   * @return A resolved string
   *
   * @throws ConfigurationException
   *         If not exist target property
   */
  public String resolvePlaceholder(
          final Map<Object, Object> properties, String input, final boolean throwIfPropertyNotFound) {
    if (input == null || input.length() <= 3) { // #{} > 3
      return input;
    }
    int prefixIndex;
    int suffixIndex;

    final StringBuilder builder = new StringBuilder();
    while ((prefixIndex = input.indexOf(Constant.PLACE_HOLDER_PREFIX)) > -1 //
            && (suffixIndex = input.indexOf(Constant.PLACE_HOLDER_SUFFIX)) > -1) {

      builder.append(input, 0, prefixIndex);

      final String key = input.substring(prefixIndex + 2, suffixIndex);

      final Object property = properties.get(key);
      if (property == null) {
        if (throwIfPropertyNotFound) {
          throw new ConfigurationException("Properties -> [" + key + "] , must specify a value.");
        }
        log.debug("There is no property for key: [{}]", key);
        return null;
      }
      // find
      builder.append(resolvePlaceholder(properties, (property instanceof String) ? (String) property : null, throwIfPropertyNotFound));
      input = input.substring(suffixIndex + 1);
    }
    if (builder.length() == 0) {
      return input;
    }
    return builder.append(input).toString();
  }

  public void setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  public ConversionService getConversionService() {
    return conversionService;
  }

  public void setThrowIfPropertyNotFound(boolean throwIfPropertyNotFound) {
    this.throwIfPropertyNotFound = throwIfPropertyNotFound;
  }

  public Properties getVariables() {
    return variables;
  }

  public void setVariables(Properties variables) {
    Assert.notNull(variables, "variables must not be null");
    this.variables = variables;
  }

  public void setExpressionProcessor(ExpressionProcessor expressionProcessor) {
    this.expressionProcessor = expressionProcessor;
  }

  public ExpressionProcessor getExpressionProcessor() {
    return expressionProcessor;
  }

  private ExpressionProcessor obtainProcessor() {
    if (expressionProcessor == null) {
      final ExpressionFactory exprFactory = ExpressionFactory.getSharedInstance();
      ApplicationContext context = this.context;
      if (context == null) {
        context = ContextUtils.getLastStartupContext();
        log.info("Using global ApplicationContext {}", context);
      }
      StandardExpressionContext globalContext;
      if (context == null) {
        log.info("There isn't a global ApplicationContext");
        globalContext = new StandardExpressionContext(exprFactory);
      }
      else {
        final BeanFactory beanFactory = context.getBeanFactory();
        globalContext = new ValueExpressionContext(exprFactory, (ConfigurableBeanFactory) beanFactory);
      }

      globalContext.defineBean(Constant.ENV, variables);
      expressionProcessor = new ExpressionProcessor(new ExpressionManager(globalContext, exprFactory));
    }
    return expressionProcessor;
  }

}
