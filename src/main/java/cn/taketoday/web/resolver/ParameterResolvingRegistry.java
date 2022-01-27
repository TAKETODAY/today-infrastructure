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
package cn.taketoday.web.resolver;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.beans.factory.support.DependencyInjectorAwareInstantiator;
import cn.taketoday.beans.factory.support.PropertyValuesBinder;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.context.expression.ExpressionEvaluator;
import cn.taketoday.context.expression.ExpressionInfo;
import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.core.PathMatcher;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.ConversionServiceAware;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.http.converter.AllEncompassingFormHttpMessageConverter;
import cn.taketoday.http.converter.ByteArrayHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.annotation.RequestAttribute;
import cn.taketoday.web.handler.method.RequestBodyAdvice;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.handler.method.ResponseBodyAdvice;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.resolver.date.DateParameterResolver;
import cn.taketoday.web.resolver.date.LocalDateParameterResolver;
import cn.taketoday.web.resolver.date.LocalDateTimeParameterResolver;
import cn.taketoday.web.resolver.date.LocalTimeParameterResolver;
import cn.taketoday.web.util.UrlPathHelper;
import cn.taketoday.web.view.RedirectModelManager;

import static cn.taketoday.web.resolver.ConverterAwareParameterResolver.from;

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
  @Nullable
  private RedirectModelManager redirectModelManager;
  /**
   * @since 3.0.1
   */
  @Nullable
  private MultipartConfiguration multipartConfig;
  /**
   * @since 3.0.1
   */
  private ExpressionEvaluator expressionEvaluator;

  /**
   * @since 4.0
   */
  private ConversionService conversionService;

  // @since 4.0
  private List<HttpMessageConverter<?>> messageConverters;

  // @since 4.0
  private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

  // @since 4.0
  private final ArrayList<Object> requestResponseBodyAdvice = new ArrayList<>();

  private PathMatcher pathMatcher;
  private UrlPathHelper urlPathHelper;

  public void setPathMatcher(PathMatcher pathMatcher) {
    this.pathMatcher = pathMatcher;
  }

  public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
    this.urlPathHelper = urlPathHelper;
  }

  public ParameterResolvingRegistry() {
    this.messageConverters = new ArrayList<>(4);
    this.messageConverters.add(new ByteArrayHttpMessageConverter());
    this.messageConverters.add(new StringHttpMessageConverter());
    this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
  }

  /**
   * Set the {@link ContentNegotiationManager} to use to determine requested media types.
   * If not set, the default constructor is used.
   *
   * @since 4.0
   */
  public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
    this.contentNegotiationManager = contentNegotiationManager;
  }

  /**
   * Provide the converters to use in argument resolvers and return value
   * handlers that support reading and/or writing to the body of the
   * request and response.
   *
   * @since 4.0
   */
  public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
    this.messageConverters = messageConverters;
  }

  /**
   * Return the configured message body converters.
   *
   * @since 4.0
   */
  public List<HttpMessageConverter<?>> getMessageConverters() {
    return this.messageConverters;
  }

  /**
   * Add one or more {@code RequestBodyAdvice} instances to intercept the
   * request before it is read and converted for {@code @RequestBody} and
   * {@code HttpEntity} method arguments.
   */
  public void setRequestBodyAdvice(@Nullable List<RequestBodyAdvice> requestBodyAdvice) {
    if (requestBodyAdvice != null) {
      this.requestResponseBodyAdvice.addAll(requestBodyAdvice);
    }
  }

  /**
   * Add one or more {@code ResponseBodyAdvice} instances to intercept the
   * response before {@code @ResponseBody} or {@code ResponseEntity} return
   * values are written to the response body.
   */
  public void setResponseBodyAdvice(@Nullable List<ResponseBodyAdvice<?>> responseBodyAdvice) {
    if (responseBodyAdvice != null) {
      this.requestResponseBodyAdvice.addAll(responseBodyAdvice);
    }
  }

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
   * Find a suitable {@link ParameterResolvingStrategy} for given {@link ResolvableMethodParameter}
   *
   * @param resolvable resolvable MethodParameter
   * @return a suitable {@link ParameterResolvingStrategy},
   * if returns {@code null} no suitable  {@link ParameterResolvingStrategy}
   */
  @Nullable
  protected ParameterResolvingStrategy lookupStrategy(
          ResolvableMethodParameter resolvable, Iterable<ParameterResolvingStrategy> strategies) {
    for (final ParameterResolvingStrategy resolver : strategies) {
      if (resolver.supportsParameter(resolvable)) {
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
   * @param parameter parameter value to be resolve
   * @return A suitable {@link ParameterResolvingStrategy}
   */
  @Nullable
  public ParameterResolvingStrategy findStrategy(final ResolvableMethodParameter parameter) {
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
   * @throws ParameterResolverNotFoundException If there isn't a suitable resolver
   */
  public ParameterResolvingStrategy obtainResolvingStrategy(final ResolvableMethodParameter parameter) {
    final ParameterResolvingStrategy resolver = findStrategy(parameter);
    if (resolver == null) {
      throw new ParameterResolverNotFoundException(
              parameter,
              "There isn't have a parameter resolver to resolve parameter: ["
                      + parameter.getParameterType() + "] called: ["
                      + parameter.getName() + "] on " + parameter.getParameter().getExecutable());
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
            new ValueParameterResolver(expressionEvaluator),
            new PropsParameterResolver(context),
            new AutowiredParameterResolver(context)
    );

    // For cookies
    // ------------------------------------------

    CookieParameterResolver.register(strategies);

    // For multipart
    // -------------------------------------------
    MultipartConfiguration multipartConfig = getMultipartConfig();
    if (multipartConfig == null) { // @since 4.0
      multipartConfig = createMultipartConfig();
      setMultipartConfig(multipartConfig);
    }
    Assert.state(multipartConfig != null, "MultipartConfiguration is required");

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

    // type-based argument resolution
    strategies.add(new ModelParameterResolver(modelManager));
    strategies.add(new StreamParameterResolver());
    strategies.add(new ThrowableHandlerParameterResolver());

    strategies.add(new HttpEntityMethodProcessor(
            getMessageConverters(), contentNegotiationManager, requestResponseBodyAdvice, modelManager));

    // Annotation-based argument resolution
    strategies.add(new RequestResponseBodyMethodProcessor(
            getMessageConverters(), contentNegotiationManager, requestResponseBodyAdvice));

    // Date API support @since 3.0
    strategies.add(new DateParameterResolver());
    strategies.add(new LocalDateParameterResolver());
    strategies.add(new LocalTimeParameterResolver());
    strategies.add(new LocalDateTimeParameterResolver());

    // @since 3.0
    configureDataBinder(strategies);

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
   * config ParameterResolver using {@link PropertyValuesBinder}
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
   * @param resolverClass element whose presence in this defaultResolvers or customizedResolvers is to be tested
   * @return <tt>true</tt> if resolvers contains the specified {@code resolverClass}
   */
  public boolean contains(Class<?> resolverClass) {
    return defaultStrategies.contains(resolverClass)
            || customizedStrategies.contains(resolverClass);
  }

  //

  public void setRedirectModelManager(@Nullable RedirectModelManager redirectModelManager) {
    this.redirectModelManager = redirectModelManager;
  }

  @Nullable
  public RedirectModelManager getRedirectModelManager() {
    return redirectModelManager;
  }

  public void setMultipartConfig(@Nullable MultipartConfiguration multipartConfig) {
    this.multipartConfig = multipartConfig;
  }

  @Nullable
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

  @Override
  public String toString() {
    return ToStringBuilder.valueOf(this)
            .append("defaultStrategies", defaultStrategies.size())
            .append("customizedStrategies", customizedStrategies.size())
            .toString();
  }

  // ParameterResolver

  record OR(Class<?> one, Class<?> two) implements ParameterResolvingStrategy.SupportsFunction {

    @Override
    public boolean supports(ResolvableMethodParameter parameter) {
      return parameter.is(one) || parameter.is(two);
    }
  }

  // AnnotationParameterResolver

  static final class PropsParameterResolver extends AnnotationParameterResolver<Props> {
    final PropsReader propsReader;
    final WebApplicationContext context;
    final DependencyInjectorAwareInstantiator beanInstantiator;

    PropsParameterResolver(WebApplicationContext context) {
      super(Props.class);
      this.context = context;
      this.propsReader = new PropsReader(context.getEnvironment());
      this.beanInstantiator = DependencyInjectorAwareInstantiator.from(context);
    }

    @Override
    protected Object resolveInternal(Props target, RequestContext ctx, ResolvableMethodParameter parameter) {
      final Object bean = beanInstantiator.instantiate(parameter.getParameterType(), new Object[] { ctx });
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
    protected Object resolveInternal(Value target, RequestContext context, ResolvableMethodParameter parameter) {
      ExpressionInfo expressionInfo = new ExpressionInfo(target);
      return expressionEvaluator.evaluate(expressionInfo, parameter.getParameterType());
    }
  }

  static final class RequestAttributeParameterResolver extends AnnotationParameterResolver<RequestAttribute> {
    RequestAttributeParameterResolver() {
      super(RequestAttribute.class);
    }

    @Override
    public Object resolveParameter(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return context.getAttribute(resolvable.getName());
    }
  }

}
