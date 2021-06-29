/**
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
package cn.taketoday.web.resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import cn.taketoday.context.ExpressionEvaluator;
import cn.taketoday.context.annotation.Env;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.factory.DataBinder;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.annotation.RequestAttribute;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.resolver.date.DateParameterResolver;
import cn.taketoday.web.resolver.date.LocalDateParameterResolver;
import cn.taketoday.web.resolver.date.LocalDateTimeParameterResolver;
import cn.taketoday.web.resolver.date.LocalTimeParameterResolver;
import cn.taketoday.web.ui.RedirectModelManager;
import cn.taketoday.web.view.MessageConverter;

import static cn.taketoday.context.utils.ContextUtils.resolveProps;
import static cn.taketoday.web.resolver.ConverterParameterResolver.convert;

/**
 * @author TODAY 2019-07-07 23:24
 * @since 3.0
 */
public class ParameterResolvers extends WebApplicationContextSupport {
  private final ArrayList<ParameterResolver> resolvers = new ArrayList<>(36);

  /**
   * @since 3.0.1
   */
  private MessageConverter messageConverter;
  /**
   * @since 3.0.1
   */
  private RedirectModelManager redirectModelManager;
  /**
   * @since 3.0.1
   */
  private MultipartConfiguration multipartConfig;
  /**
   * @since 3.0.1
   */
  private ExpressionEvaluator expressionEvaluator;

  public void addResolver(ParameterResolver... resolver) {
    Collections.addAll(resolvers, resolver);
    sort();
  }

  public void addResolver(List<ParameterResolver> resolvers) {
    this.resolvers.addAll(resolvers);
    sort();
  }

  public void setResolver(List<ParameterResolver> resolver) {
    resolvers.clear();
    resolvers.addAll(resolver);
    sort();
  }

  public List<ParameterResolver> getResolvers() {
    return resolvers;
  }

  /**
   * Find a suitable {@link ParameterResolver} for given {@link MethodParameter}
   *
   * @param parameter
   *         MethodParameter
   *
   * @return a suitable {@link ParameterResolver},
   * if returns {@code null} no suitable  {@link ParameterResolver}
   */
  public ParameterResolver getResolver(final MethodParameter parameter) {
    for (final ParameterResolver resolver : getResolvers()) {
      if (resolver.supports(parameter)) {
        return resolver;
      }
    }
    return null;
  }

  public void sort() {
    OrderUtils.reversedSort(resolvers);
  }

  /**
   * Get correspond parameter resolver, If there isn't a suitable resolver will be
   * throw {@link IllegalStateException}
   *
   * @return A suitable {@link ParameterResolver}
   *
   * @throws IllegalStateException
   *         If there isn't a suitable resolver
   */
  public ParameterResolver obtainResolver(final MethodParameter parameter) {
    final ParameterResolver resolver = getResolver(parameter);
    Assert.state(resolver != null,
                 () -> "There isn't have a parameter resolver to resolve parameter: ["
                         + parameter.getParameterClass() + "] called: ["
                         + parameter.getName() + "] on " + parameter.getHandlerMethod());
    return resolver;
  }

  /**
   * register default {@link ParameterResolver}s
   */
  public void registerDefaultParameterResolvers() {
    // Use ConverterParameterResolver to resolve primitive types
    // --------------------------------------------------------------------------
    final List<ParameterResolver> resolvers = getResolvers();

    resolvers.add(convert(String.class, s -> s));
    resolvers.add(convert(new SupportsFunction0(Long.class, long.class), Long::parseLong));
    resolvers.add(convert(new SupportsFunction0(Integer.class, int.class), Integer::parseInt));
    resolvers.add(convert(new SupportsFunction0(Short.class, short.class), Short::parseShort));
    resolvers.add(convert(new SupportsFunction0(Float.class, float.class), Float::parseFloat));
    resolvers.add(convert(new SupportsFunction0(Double.class, double.class), Double::parseDouble));
    resolvers.add(convert(new SupportsFunction0(Boolean.class, boolean.class), Boolean::parseBoolean));

    // For some useful context annotations
    // --------------------------------------------

    final WebApplicationContext context = obtainApplicationContext();
    ExpressionEvaluator expressionEvaluator = getExpressionEvaluator();
    if (expressionEvaluator == null) {
      expressionEvaluator = new ExpressionEvaluator(context);
    }

    resolvers.add(new RequestAttributeParameterResolver());
    resolvers.add(new EnvParameterResolver(expressionEvaluator));
    resolvers.add(new ValueParameterResolver(expressionEvaluator));
    resolvers.add(new PropsParameterResolver(context));
    resolvers.add(new AutowiredParameterResolver(context));

    // HandlerMethod
    resolvers.add(new HandlerMethodParameterResolver());

    // For cookies
    // ------------------------------------------
    CookieParameterResolver.registerParameterResolver(resolvers);

    // For multipart
    // -------------------------------------------
    MultipartConfiguration multipartConfig = getMultipartConfig();
    if (multipartConfig == null) {
      multipartConfig = context.getBean(MultipartConfiguration.class);
    }
    Assert.state(multipartConfig != null, "MultipartConfiguration Can't be null");

    DefaultMultipartResolver.registerParameterResolver(resolvers, multipartConfig);

    // Header
    resolvers.add(new HeaderParameterResolver());
    RedirectModelManager modelManager = getRedirectModelManager();
    if (modelManager == null) {
      modelManager = context.getBean(RedirectModelManager.class);
    }

    if (modelManager == null) {
      log.info("RedirectModel disabled");
      modelManager = RedirectModelManager.NOP;
    }
    // @since 3.0
    configureDataBinder(resolvers);

    resolvers.add(new ModelParameterResolver(modelManager));
    resolvers.add(new SimpleArrayParameterResolver());
    resolvers.add(new StreamParameterResolver());
    MessageConverter messageConverter = getMessageConverter();
    if (messageConverter == null) {
      messageConverter = context.getBean(MessageConverter.class);
    }
    resolvers.add(new RequestBodyParameterResolver(messageConverter));
    resolvers.add(new ThrowableHandlerParameterResolver());

    // Date API support @since 3.0
    resolvers.add(new DateParameterResolver());
    resolvers.add(new LocalDateParameterResolver());
    resolvers.add(new LocalTimeParameterResolver());
    resolvers.add(new LocalDateTimeParameterResolver());

    // ordering
    sort();
  }

  /**
   * config ParameterResolver using {@link DataBinder}
   */
  public void configureDataBinder(List<ParameterResolver> resolvers) {
    final WebApplicationContext context = obtainApplicationContext();
    if (!contains(DataBinderMapParameterResolver.class, resolvers)
            && !context.containsBeanDefinition(DataBinderMapParameterResolver.class)) {
      resolvers.add(new DataBinderMapParameterResolver());
    }
    // resolve array of beans
    if (!contains(DataBinderArrayParameterResolver.class, resolvers)
            && !context.containsBeanDefinition(DataBinderArrayParameterResolver.class)) {
      resolvers.add(new DataBinderArrayParameterResolver());
    }
    // resolve a collection of beans
    if (!contains(DataBinderCollectionParameterResolver.class, resolvers)
            && !context.containsBeanDefinition(DataBinderCollectionParameterResolver.class)) {
      resolvers.add(new DataBinderCollectionParameterResolver());
    }
    // resolve bean
    if (!contains(DataBinderParameterResolver.class, resolvers)
            && !context.containsBeanDefinition(DataBinderParameterResolver.class)) {
      resolvers.add(new DataBinderParameterResolver());
    }
  }

  public boolean contains(Class<?> resolverClass) {
    return contains(resolverClass, resolvers);
  }

  private boolean contains(Class<?> resolverClass, List<ParameterResolver> resolvers) {
    for (final ParameterResolver resolver : resolvers) {
      if (resolverClass == resolver.getClass()) {
        return true;
      }
    }
    return false;
  }

  //

  public void setMessageConverter(MessageConverter messageConverter) {
    this.messageConverter = messageConverter;
  }

  public MessageConverter getMessageConverter() {
    return messageConverter;
  }

  public void setRedirectModelManager(RedirectModelManager redirectModelManager) {
    this.redirectModelManager = redirectModelManager;
  }

  public RedirectModelManager getRedirectModelManager() {
    return redirectModelManager;
  }

  public void setMultipartConfig(MultipartConfiguration multipartConfig) {
    this.multipartConfig = multipartConfig;
  }

  public MultipartConfiguration getMultipartConfig() {
    return multipartConfig;
  }

  public void setExpressionEvaluator(ExpressionEvaluator expressionEvaluator) {
    this.expressionEvaluator = expressionEvaluator;
  }

  public ExpressionEvaluator getExpressionEvaluator() {
    return expressionEvaluator;
  }

  // ParameterResolver

  static final class SupportsFunction0 implements ParameterResolver.SupportsFunction {
    final Class<?> one;
    final Class<?> two;

    SupportsFunction0(Class<?> one, Class<?> two) {
      this.one = one;
      this.two = two;
    }

    @Override
    public boolean supports(MethodParameter parameter) {
      return parameter.is(one) || parameter.is(two);
    }
  }

  // AnnotationParameterResolver

  static final class PropsParameterResolver extends AnnotationParameterResolver<Props> {
    final Properties properties;
    final WebApplicationContext context;

    PropsParameterResolver(WebApplicationContext context) {
      super(Props.class);
      this.context = context;
      this.properties = context.getEnvironment().getProperties();
    }

    @Override
    protected Object resolveInternal(Props target, RequestContext ctx, MethodParameter parameter) {
      final Object bean = ClassUtils.newInstance(parameter.getParameterClass(), context);
      return resolveProps(target, bean, properties);
    }
  }

  static final class ValueParameterResolver extends AnnotationParameterResolver<Value> {
    final ExpressionEvaluator expressionEvaluator;

    ValueParameterResolver(ExpressionEvaluator expressionEvaluator) {
      super(Value.class);
      this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    protected Object resolveInternal(Value target, RequestContext context, MethodParameter parameter) {
      return expressionEvaluator.evaluate(target, parameter.getParameterClass());
    }
  }

  static final class EnvParameterResolver extends AnnotationParameterResolver<Env> {
    final ExpressionEvaluator expressionEvaluator;

    EnvParameterResolver(ExpressionEvaluator expressionEvaluator) {
      super(Env.class);
      this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    protected Object resolveInternal(Env target, RequestContext context, MethodParameter parameter) {
      return expressionEvaluator.evaluate(target, parameter.getParameterClass());
    }
  }

  static final class RequestAttributeParameterResolver extends AnnotationParameterResolver<RequestAttribute> {
    RequestAttributeParameterResolver() {
      super(RequestAttribute.class);
    }

    @Override
    public Object resolveParameter(RequestContext context, MethodParameter parameter) throws Throwable {
      return context.getAttribute(parameter.getName());
    }
  }

  static final class HandlerMethodParameterResolver implements ParameterResolver {
    @Override
    public boolean supports(MethodParameter parameter) {
      return parameter.is(HandlerMethod.class);
    }

    @Override
    public Object resolveParameter(RequestContext context, MethodParameter parameter) throws Throwable {
      return parameter.getHandlerMethod();
    }
  }

}
