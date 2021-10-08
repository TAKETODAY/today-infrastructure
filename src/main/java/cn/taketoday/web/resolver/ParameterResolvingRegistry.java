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
package cn.taketoday.web.resolver;

import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.support.BeanFactoryAwareBeanInstantiator;
import cn.taketoday.beans.support.DataBinder;
import cn.taketoday.context.Env;
import cn.taketoday.context.ExpressionEvaluator;
import cn.taketoday.context.Props;
import cn.taketoday.context.Value;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.core.Assert;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.ConversionServiceAware;
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

import static cn.taketoday.web.resolver.ConverterParameterResolver.from;

/**
 * ParameterResolvingStrategy registry
 *
 * @author TODAY 2019-07-07 23:24
 * @see ParameterResolvingStrategy
 * @since 3.0
 */
public class ParameterResolvingRegistry
        extends WebApplicationContextSupport implements ArraySizeTrimmer {

  private final ParameterResolvingStrategies defaultStrategies = new ParameterResolvingStrategies(36);
  private final ParameterResolvingStrategies customizedStrategies = new ParameterResolvingStrategies();

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
   * get default resolving-strategies
   */
  public ParameterResolvingStrategies getDefaultStrategies() {
    return defaultStrategies;
  }

  /**
   * get customized resolving-strategies
   *
   * @since 4.0
   */
  public ParameterResolvingStrategies getCustomizedStrategies() {
    return customizedStrategies;
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
  @Nullable
  protected ParameterResolvingStrategy lookupStrategy(
          MethodParameter parameter, Iterable<ParameterResolvingStrategy> strategies) {
    for (final ParameterResolvingStrategy resolver : strategies) {
      if (resolver.supportsParameter(parameter)) {
        return resolver;
      }
    }
    return null;
  }

  /**
   * find parameter resolving strategy
   * <p>
   * find in {@code customizedResolvers} and when find in {@code defaultResolvers}
   *
   * @param parameter
   *         parameter value to be resolve
   *
   * @return A suitable {@link ParameterResolvingStrategy}
   */
  @Nullable
  public ParameterResolvingStrategy findStrategy(final MethodParameter parameter) {
    ParameterResolvingStrategy resolvingStrategy = lookupStrategy(parameter, customizedStrategies);
    if (resolvingStrategy == null) {
      resolvingStrategy = lookupStrategy(parameter, defaultStrategies);
    }
    return resolvingStrategy;
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
    final ParameterResolvingStrategy resolver = findStrategy(parameter);
    if (resolver == null) {
      throw new ParameterResolverNotFoundException(
              parameter,
              "There isn't have a parameter resolver to resolve parameter: ["
                      + parameter.getParameterClass() + "] called: ["
                      + parameter.getName() + "] on " + parameter.getHandlerMethod());
    }
    return resolver;
  }

  public void registerDefaultParameterResolvers() {
    registerDefaults(defaultStrategies);
  }

  /**
   * register default {@link ParameterResolvingStrategy}s
   */
  public void registerDefaults(ParameterResolvingStrategies strategies) {
    log.info("Registering default parameter-resolvers to {}", strategies);

    // Use ConverterParameterResolver to resolve primitive types
    // --------------------------------------------------------------------------

    strategies.add(
            from(String.class, s -> s),
            from(new OR(Long.class, long.class), Long::parseLong),
            from(new OR(Integer.class, int.class), Integer::parseInt),
            from(new OR(Short.class, short.class), Short::parseShort),
            from(new OR(Float.class, float.class), Float::parseFloat),
            from(new OR(Double.class, double.class), Double::parseDouble),
            from(new OR(Boolean.class, boolean.class), Boolean::parseBoolean)
    );

    // For some useful context annotations
    // --------------------------------------------

    WebApplicationContext context = obtainApplicationContext();
    ExpressionEvaluator expressionEvaluator = getExpressionEvaluator();
    if (expressionEvaluator == null) {
      expressionEvaluator = new ExpressionEvaluator(context);
    }

    strategies.add(new RequestAttributeParameterResolver(),
                   new EnvParameterResolver(expressionEvaluator),
                   new ValueParameterResolver(expressionEvaluator),
                   new PropsParameterResolver(context),
                   new AutowiredParameterResolver(context),

                   // HandlerMethod
                   new HandlerMethodParameterResolver());

    // For cookies
    // ------------------------------------------

    CookieParameterResolver.register(strategies);

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

    DefaultMultipartResolver.register(strategies, multipartConfig);

    // Header
    strategies.add(new RequestHeaderParameterResolver());
    RedirectModelManager modelManager = getRedirectModelManager();
    if (modelManager == null) {
      modelManager = context.getBean(RedirectModelManager.class);
    }

    if (modelManager == null) {
      log.info("RedirectModel disabled");
    }
    // @since 3.0
    configureDataBinder(strategies);

    strategies.add(new ModelParameterResolver(modelManager));
    strategies.add(new StreamParameterResolver());
    MessageBodyConverter messageBodyConverter = getMessageConverter();
    if (messageBodyConverter == null) {
      messageBodyConverter = context.getBean(MessageBodyConverter.class);
      // autoDetect
      if (messageBodyConverter == null) {
        messageBodyConverter = MessageBodyConverter.autoDetect();
      }
    }
    Assert.state(messageBodyConverter != null, "No MessageConverter in this web application");

    strategies.add(new RequestBodyParameterResolver(messageBodyConverter));
    strategies.add(new ThrowableHandlerParameterResolver());

    // Date API support @since 3.0
    strategies.add(new DateParameterResolver());
    strategies.add(new LocalDateParameterResolver());
    strategies.add(new LocalTimeParameterResolver());
    strategies.add(new LocalDateTimeParameterResolver());

    strategies.add(new SimpleArrayParameterResolver());

    // apply conversionService @since 4.0
    applyConversionService(conversionService, strategies);

    // trim size
    strategies.trimToSize();
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
  public void configureDataBinder(ParameterResolvingStrategies strategies) {
    BeanDefinitionRegistry registry = obtainApplicationContext().unwrapFactory(
            BeanDefinitionRegistry.class);

    if (!strategies.contains(DataBinderMapParameterResolver.class)
            && !registry.containsBeanDefinition(DataBinderMapParameterResolver.class)) {
      strategies.add(new DataBinderMapParameterResolver());
    }
    // resolve array of beans
    if (!contains(DataBinderArrayParameterResolver.class)
            && !registry.containsBeanDefinition(DataBinderArrayParameterResolver.class)) {
      strategies.add(new DataBinderArrayParameterResolver());
    }
    // resolve a collection of beans
    if (!strategies.contains(DataBinderCollectionParameterResolver.class)
            && !registry.containsBeanDefinition(DataBinderCollectionParameterResolver.class)) {
      strategies.add(new DataBinderCollectionParameterResolver());
    }
    // resolve bean
    if (!strategies.contains(DataBinderParameterResolver.class)
            && !registry.containsBeanDefinition(DataBinderParameterResolver.class)) {
      DataBinderParameterResolver resolver = new DataBinderParameterResolver(this);
      strategies.add(resolver);
    }
  }

  /**
   * Returns <tt>true</tt> if resolvers list contains the specified {@code resolverClass}.
   * More formally, returns <tt>true</tt> if and only if all resolvers contains
   * at least one element <tt>e</tt> such that
   * <tt>(resolverClass == resolver.getClass())</tt>.
   *
   * @param resolverClass
   *         element whose presence in this defaultResolvers or customizedResolvers is to be tested
   *
   * @return <tt>true</tt> if resolvers contains the specified {@code resolverClass}
   */
  public boolean contains(Class<?> resolverClass) {
    return defaultStrategies.contains(resolverClass)
            || customizedStrategies.contains(resolverClass);
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
  public void applyConversionService(ConversionService conversionService) {
    setConversionService(conversionService);
    applyConversionService(conversionService, defaultStrategies);
    applyConversionService(conversionService, customizedStrategies);
  }

  static void applyConversionService(
          @Nullable ConversionService conversionService, Iterable<ParameterResolvingStrategy> resolvers) {
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
    defaultStrategies.trimToSize();
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
    final PropsReader propsReader;
    final WebApplicationContext context;
    final BeanFactoryAwareBeanInstantiator beanInstantiator;

    PropsParameterResolver(WebApplicationContext context) {
      super(Props.class);
      this.context = context;
      this.propsReader = new PropsReader(context.getEnvironment());
      this.beanInstantiator = new BeanFactoryAwareBeanInstantiator(context);
    }

    @Override
    protected Object resolveInternal(Props target, RequestContext ctx, MethodParameter parameter) {
      final Object bean = beanInstantiator.instantiate(parameter.getParameterClass(), new Object[] { ctx });
      return propsReader.read(target, bean);
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
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.is(HandlerMethod.class);
    }

    @Override
    public Object resolveParameter(RequestContext context, MethodParameter parameter) throws Throwable {
      return parameter.getHandlerMethod();
    }
  }

}
