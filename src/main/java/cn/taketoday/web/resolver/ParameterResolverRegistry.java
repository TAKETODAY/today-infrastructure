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
import java.util.function.Predicate;

import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.beans.support.DataBinder;
import cn.taketoday.context.Env;
import cn.taketoday.context.ExpressionEvaluator;
import cn.taketoday.context.Props;
import cn.taketoday.context.Value;
import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.core.Assert;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.ConversionServiceAware;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.MessageBodyConverter;
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
import cn.taketoday.web.view.RedirectModelManager;

import static cn.taketoday.context.ContextUtils.resolveProps;
import static cn.taketoday.web.resolver.ConverterParameterResolver.convert;

/**
 * ParameterResolvingStrategy registry
 *
 * @author TODAY 2019-07-07 23:24
 * @see ParameterResolvingStrategy
 * @since 3.0
 */
public class ParameterResolverRegistry
        extends WebApplicationContextSupport implements ArraySizeTrimmer {
  private final ArrayList<ParameterResolvingStrategy> resolvers = new ArrayList<>(36);

  /**
   * @since 3.0.1
   */
  private MessageBodyConverter messageBodyConverter;
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

  /**
   * @since 4.0
   */
  private ConversionService conversionService;

  /**
   * add resolvers or resolving-strategies
   *
   * @param resolver
   *         resolvers or resolving-strategies
   */
  public void addResolvingStrategies(ParameterResolvingStrategy... resolver) {
    Collections.addAll(resolvers, resolver);
    trimToSize();
    sort();
  }

  /**
   * add resolvers or resolving-strategies
   *
   * @param resolvers
   *         resolvers or resolving-strategies
   */
  public void addResolvingStrategies(List<ParameterResolvingStrategy> resolvers) {
    this.resolvers.addAll(resolvers);
    trimToSize();
    sort();
  }

  /**
   * set or clear resolvers
   *
   * @param resolver
   *         can be null
   */
  public void setResolvingStrategies(@Nullable List<ParameterResolvingStrategy> resolver) {
    resolvers.clear();
    if (CollectionUtils.isNotEmpty(resolver)) {
      resolvers.addAll(resolver);
      trimToSize();
      sort();
    }
  }

  /**
   * get resolving-strategies
   */
  public List<ParameterResolvingStrategy> getResolvingStrategies() {
    return resolvers;
  }

  /**
   * Find a suitable {@link ParameterResolvingStrategy} for given {@link MethodParameter}
   *
   * @param parameter
   *         MethodParameter
   *
   * @return a suitable {@link ParameterResolvingStrategy},
   * if returns {@code null} no suitable  {@link ParameterResolvingStrategy}
   */
  public ParameterResolvingStrategy getResolvingStrategy(final MethodParameter parameter) {
    for (final ParameterResolvingStrategy resolver : getResolvingStrategies()) {
      if (resolver.supports(parameter)) {
        return resolver;
      }
    }
    return null;
  }

  /**
   * sort resolvers or resolving-strategies
   */
  public void sort() {
    AnnotationAwareOrderComparator.sort(resolvers);
  }

  /**
   * Get correspond parameter resolver, If there isn't a suitable resolver will be
   * throws {@link ParameterResolverNotFoundException}
   *
   * @return A suitable {@link ParameterResolvingStrategy}
   *
   * @throws ParameterResolverNotFoundException
   *         If there isn't a suitable resolver
   */
  public ParameterResolvingStrategy obtainResolvingStrategy(final MethodParameter parameter) {
    final ParameterResolvingStrategy resolver = getResolvingStrategy(parameter);
    if (resolver == null) {
      throw new ParameterResolverNotFoundException(
              parameter,
              "There isn't have a parameter resolver to resolve parameter: ["
                      + parameter.getParameterClass() + "] called: ["
                      + parameter.getName() + "] on " + parameter.getHandlerMethod());
    }
    return resolver;
  }

  /**
   * register default {@link ParameterResolvingStrategy}s
   */
  public void registerDefaultParameterResolvers() {
    log.info("Registering default parameter-resolvers");

    // Use ConverterParameterResolver to resolve primitive types
    // --------------------------------------------------------------------------
    final List<ParameterResolvingStrategy> resolvers = getResolvingStrategies();

    resolvers.add(convert(String.class, s -> s));
    resolvers.add(convert(new OR(Long.class, long.class), Long::parseLong));
    resolvers.add(convert(new OR(Integer.class, int.class), Integer::parseInt));
    resolvers.add(convert(new OR(Short.class, short.class), Short::parseShort));
    resolvers.add(convert(new OR(Float.class, float.class), Float::parseFloat));
    resolvers.add(convert(new OR(Double.class, double.class), Double::parseDouble));
    resolvers.add(convert(new OR(Boolean.class, boolean.class), Boolean::parseBoolean));

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
    CookieParameterResolver.register(resolvers);

    // For multipart
    // -------------------------------------------
    MultipartConfiguration multipartConfig = getMultipartConfig();
    if (multipartConfig == null) {
      multipartConfig = context.getBean(MultipartConfiguration.class);
      if (multipartConfig == null) { // @since 4.0
        multipartConfig = createMultipartConfig();
        setMultipartConfig(multipartConfig);
      }
    }
    Assert.state(multipartConfig != null, "MultipartConfiguration Can't be null");

    DefaultMultipartResolver.register(resolvers, multipartConfig);

    // Header
    resolvers.add(new HeaderParameterResolver());
    RedirectModelManager modelManager = getRedirectModelManager();
    if (modelManager == null) {
      modelManager = context.getBean(RedirectModelManager.class);
    }

    if (modelManager == null) {
      log.info("RedirectModel disabled");
    }
    // @since 3.0
    configureDataBinder(resolvers);

    resolvers.add(new ModelParameterResolver(modelManager));
    resolvers.add(new SimpleArrayParameterResolver());
    resolvers.add(new StreamParameterResolver());
    MessageBodyConverter messageBodyConverter = getMessageConverter();
    if (messageBodyConverter == null) {
      messageBodyConverter = context.getBean(MessageBodyConverter.class);
    }
    resolvers.add(new RequestBodyParameterResolver(messageBodyConverter));
    resolvers.add(new ThrowableHandlerParameterResolver());

    // Date API support @since 3.0
    resolvers.add(new DateParameterResolver());
    resolvers.add(new LocalDateParameterResolver());
    resolvers.add(new LocalTimeParameterResolver());
    resolvers.add(new LocalDateTimeParameterResolver());

    // apply conversionService @since 4.0
    applyConversionService(conversionService, resolvers);

    // ordering
    sort();
  }

  /**
   * create default MultipartConfiguration
   *
   * @since 4.0
   */
  protected MultipartConfiguration createMultipartConfig() {
    return new MultipartConfiguration();
  }

  /**
   * config ParameterResolver using {@link DataBinder}
   */
  public void configureDataBinder(List<ParameterResolvingStrategy> resolvers) {
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
      DataBinderParameterResolver resolver = new DataBinderParameterResolver(this);
      resolvers.add(resolver);
    }
  }

  /**
   * @since 4.0
   */
  public boolean removeIf(Predicate<ParameterResolvingStrategy> filter) {
    return resolvers.removeIf(filter);
  }

  public boolean contains(Class<?> resolverClass) {
    return contains(resolverClass, resolvers);
  }

  private boolean contains(Class<?> resolverClass, List<ParameterResolvingStrategy> resolvers) {
    for (final ParameterResolvingStrategy resolver : resolvers) {
      if (resolverClass == resolver.getClass()) {
        return true;
      }
    }
    return false;
  }

  //

  public void setMessageConverter(MessageBodyConverter messageBodyConverter) {
    this.messageBodyConverter = messageBodyConverter;
  }

  public MessageBodyConverter getMessageConverter() {
    return messageBodyConverter;
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

  /**
   * @since 4.0
   */
  public void setConversionService(ConversionService conversionService) {
    Assert.notNull(conversionService, "conversionService must not be null");
    this.conversionService = conversionService;
  }

  /**
   * apply conversionService to resolvers
   *
   * @since 4.0
   */
  public void applyConversionService(@Nullable ConversionService conversionService) {
    setConversionService(conversionService);
    applyConversionService(conversionService, resolvers);
  }

  private void applyConversionService(
          @Nullable ConversionService conversionService, List<ParameterResolvingStrategy> resolvers) {
    if (conversionService != null) {
      for (final ParameterResolvingStrategy resolver : resolvers) {
        if (resolver instanceof ConversionServiceAware) {
          ((ConversionServiceAware) resolver).setConversionService(conversionService);
        }
      }
    }
  }

  /**
   * @since 4.0
   */
  public ConversionService getConversionService() {
    return conversionService;
  }

  /**
   * @since 4.0
   */
  @Override
  public void trimToSize() {
    resolvers.trimToSize();
  }

  // ParameterResolver

  static final class OR implements ParameterResolvingStrategy.SupportsFunction {
    final Class<?> one;
    final Class<?> two;

    OR(Class<?> one, Class<?> two) {
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
      final Object bean = BeanUtils.newInstance(parameter.getParameterClass(), context);
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

  static final class HandlerMethodParameterResolver implements ParameterResolvingStrategy {
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
