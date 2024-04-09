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

package cn.taketoday.web.bind.resolver;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.ApplicationObjectSupport;
import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.ConversionServiceAware;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.http.converter.AllEncompassingFormHttpMessageConverter;
import cn.taketoday.http.converter.ByteArrayHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RedirectModelManager;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.annotation.RequestAttribute;
import cn.taketoday.web.bind.RequestBindingException;
import cn.taketoday.web.bind.resolver.date.DateParameterResolver;
import cn.taketoday.web.bind.resolver.date.LocalDateParameterResolver;
import cn.taketoday.web.bind.resolver.date.LocalDateTimeParameterResolver;
import cn.taketoday.web.bind.resolver.date.LocalTimeParameterResolver;
import cn.taketoday.web.handler.method.ModelAndViewMethodArgumentResolver;
import cn.taketoday.web.handler.method.ModelAttributeMethodProcessor;
import cn.taketoday.web.handler.method.RequestBodyAdvice;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.handler.method.ResponseBodyAdvice;
import cn.taketoday.web.multipart.MultipartConfig;
import cn.taketoday.web.servlet.WebApplicationContext;
import jakarta.servlet.ServletContext;

/**
 * ParameterResolvingStrategy registry
 *
 * @author TODAY 2019-07-07 23:24
 * @see ParameterResolvingStrategy
 * @since 3.0
 */
public class ParameterResolvingRegistry extends ApplicationObjectSupport implements ArraySizeTrimmer, InitializingBean {

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
  private MultipartConfig multipartConfig;

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

  public ParameterResolvingRegistry() {
    this.messageConverters = new ArrayList<>(4);
    this.messageConverters.add(new ByteArrayHttpMessageConverter());
    this.messageConverters.add(new StringHttpMessageConverter());
    this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
  }

  public ParameterResolvingRegistry(List<HttpMessageConverter<?>> messageConverters) {
    setMessageConverters(messageConverters);
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

  @Override
  public void afterPropertiesSet() throws Exception {
    if (defaultStrategies.isEmpty()) {
      registerDefaultStrategies();
    }
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
    for (ParameterResolvingStrategy resolver : strategies) {
      if (resolver.supportsParameter(resolvable)) {
        return resolver;
      }
    }
    return null;
  }

  /**
   * find parameter resolving strategies
   * <p>
   * find in {@code customizedResolvers} and when find in {@code defaultResolvers}
   *
   * @param parameter parameter value to be resolve
   * @return A suitable {@link ParameterResolvingStrategy}
   */
  @Nullable
  public ParameterResolvingStrategy findStrategy(ResolvableMethodParameter parameter) {
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
  public ParameterResolvingStrategy obtainStrategy(ResolvableMethodParameter parameter) {
    ParameterResolvingStrategy resolver = findStrategy(parameter);
    if (resolver == null) {
      throw new ParameterResolverNotFoundException(
              parameter,
              "There isn't have a parameter resolver to resolve parameter: ["
                      + parameter.getParameterType() + "] called: ["
                      + parameter.getName() + "] on " + parameter.getMethod());
    }
    return resolver;
  }

  public void registerDefaultStrategies() {
    registerDefaultStrategies(defaultStrategies);
  }

  /**
   * register default {@link ParameterResolvingStrategy}s
   */
  public void registerDefaultStrategies(ParameterResolvingStrategies strategies) {
    logger.debug("Registering default parameter-resolvers to {}", strategies);

    ApplicationContext context = obtainApplicationContext();
    ConfigurableBeanFactory beanFactory = context.unwrapFactory(ConfigurableBeanFactory.class);
    RedirectModelManager modelManager = getRedirectModelManager();
    if (modelManager == null) {
      logger.info("RedirectModel disabled");
    }

    // Annotation-based argument resolution
    strategies.add(new RequestParamMethodArgumentResolver(beanFactory, false));
    strategies.add(new RequestParamMapMethodArgumentResolver());
    strategies.add(new PathVariableMethodArgumentResolver());
    strategies.add(new PathVariableMapMethodArgumentResolver());
    strategies.add(new MatrixParamMethodArgumentResolver());
    strategies.add(new MatrixParamMapMethodArgumentResolver());
    strategies.add(new ModelAttributeMethodProcessor(false));
    strategies.add(new RequestResponseBodyMethodProcessor(getMessageConverters(), contentNegotiationManager, requestResponseBodyAdvice));
    strategies.add(new RequestPartMethodArgumentResolver(getMessageConverters(), this.requestResponseBodyAdvice));
    strategies.add(new RequestHeaderMethodArgumentResolver(beanFactory));
    strategies.add(new RequestHeaderMapMethodArgumentResolver());
    strategies.add(new ExpressionValueMethodArgumentResolver(beanFactory));

    strategies.add(new RequestAttributeMethodArgumentResolver(beanFactory));
    strategies.add(new AutowiredParameterResolver(context));

    // type-based argument resolution
    if (ServletDetector.isPresent && context instanceof WebApplicationContext servletApp) {
      ServletContext servletContext = servletApp.getServletContext();
      Assert.state(servletContext != null, "ServletContext is not available");
      ServletParameterResolvers.register(beanFactory, strategies, servletContext);
    }

    CookieParameterResolver.register(strategies, beanFactory);

    strategies.add(new RequestContextMethodArgumentResolver());
    strategies.add(new ModelAndViewMethodArgumentResolver());
    strategies.add(new ModelMethodProcessor());
    strategies.add(new MapMethodProcessor());
    strategies.add(new ErrorsMethodArgumentResolver());
    strategies.add(new SessionStatusMethodArgumentResolver());
    strategies.add(new UriComponentsBuilderParameterStrategy());
    strategies.add(new HttpEntityMethodProcessor(
            getMessageConverters(), contentNegotiationManager, requestResponseBodyAdvice, modelManager));

    // Date API support @since 3.0
    strategies.add(new DateParameterResolver());
    strategies.add(new LocalDateParameterResolver());
    strategies.add(new LocalTimeParameterResolver());
    strategies.add(new LocalDateTimeParameterResolver());

    // fallback

    strategies.add(new RequestParamMethodArgumentResolver(beanFactory, true));
    strategies.add(new ModelAttributeMethodProcessor(true));

    // apply conversionService @since 4.0
    applyConversionService(conversionService, strategies);

    // trim size
    strategies.trimToSize();
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

  public void addCustomizedStrategies(ParameterResolvingStrategy... strategies) {
    customizedStrategies.add(strategies);
  }

  public void addDefaultStrategies(ParameterResolvingStrategy... strategies) {
    defaultStrategies.add(strategies);
  }

  public void setRedirectModelManager(@Nullable RedirectModelManager redirectModelManager) {
    this.redirectModelManager = redirectModelManager;
  }

  @Nullable
  public RedirectModelManager getRedirectModelManager() {
    return redirectModelManager;
  }

  public void setMultipartConfig(@Nullable MultipartConfig multipartConfig) {
    this.multipartConfig = multipartConfig;
  }

  @Nullable
  public MultipartConfig getMultipartConfig() {
    return multipartConfig;
  }

  /**
   * Add one or more {@code RequestBodyAdvice} {@code ResponseBodyAdvice}
   *
   * @see RequestBodyAdvice
   * @see ResponseBodyAdvice
   * @since 4.0
   */
  public void addRequestResponseBodyAdvice(@Nullable List<Object> list) {
    CollectionUtils.addAll(requestResponseBodyAdvice, list);
  }

  /**
   * Set one or more {@code RequestBodyAdvice} {@code ResponseBodyAdvice}
   *
   * <p>
   * clear all and add all
   *
   * @see RequestBodyAdvice
   * @see ResponseBodyAdvice
   * @since 4.0
   */
  public void setRequestResponseBodyAdvice(@Nullable List<Object> list) {
    requestResponseBodyAdvice.clear();
    CollectionUtils.addAll(requestResponseBodyAdvice, list);
  }

  /**
   * @since 4.0
   */
  public List<Object> getRequestResponseBodyAdvice() {
    return requestResponseBodyAdvice;
  }

  /**
   * @since 4.0
   */
  public void setConversionService(@Nullable ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  /**
   * apply conversionService to resolvers
   *
   * @throws IllegalArgumentException ConversionService is null
   * @since 4.0
   */
  public void applyConversionService(ConversionService conversionService) {
    Assert.notNull(conversionService, "conversionService is required");
    setConversionService(conversionService);
    applyConversionService(conversionService, defaultStrategies);
    applyConversionService(conversionService, customizedStrategies);
  }

  static void applyConversionService(@Nullable ConversionService conversionService,
          Iterable<ParameterResolvingStrategy> resolvers) {
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
  @Nullable
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
    return ToStringBuilder.from(this)
            .append("messageConverters", messageConverters)
            .append("defaultStrategies", defaultStrategies.size())
            .append("customizedStrategies", customizedStrategies.size())
            .toString();
  }

  // Static

  public static ParameterResolvingRegistry get(ApplicationContext context) {
    var resolvingRegistry = BeanFactoryUtils.find(context, ParameterResolvingRegistry.class);
    if (resolvingRegistry == null) {
      resolvingRegistry = new ParameterResolvingRegistry();
      resolvingRegistry.setApplicationContext(context);
      resolvingRegistry.registerDefaultStrategies();
    }
    return resolvingRegistry;
  }

  // AnnotationParameterResolver

  static final class RequestAttributeMethodArgumentResolver extends AbstractNamedValueResolvingStrategy {
    RequestAttributeMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
      super(beanFactory);
    }

    @Override
    public boolean supportsParameter(ResolvableMethodParameter resolvable) {
      return resolvable.hasParameterAnnotation(RequestAttribute.class);
    }

    @Nullable
    @Override
    protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext context) throws Exception {
      return context.getAttribute(name);
    }

    @Override
    protected void handleMissingValue(String name, MethodParameter parameter) {
      throw new RequestBindingException("Missing request attribute '" + name +
              "' of type " + parameter.getNestedParameterType().getSimpleName());
    }

  }

}
