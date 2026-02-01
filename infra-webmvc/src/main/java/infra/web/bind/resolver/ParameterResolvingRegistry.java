/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.bind.resolver;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationContext;
import infra.context.support.ApplicationObjectSupport;
import infra.core.ArraySizeTrimmer;
import infra.core.MethodParameter;
import infra.core.style.ToStringBuilder;
import infra.http.converter.AllEncompassingFormHttpMessageConverter;
import infra.http.converter.ByteArrayHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.util.CollectionUtils;
import infra.web.RedirectModelManager;
import infra.web.RequestContext;
import infra.web.accept.ContentNegotiationManager;
import infra.web.annotation.RequestAttribute;
import infra.web.bind.RequestBindingException;
import infra.web.handler.method.ModelAttributeMethodProcessor;
import infra.web.handler.method.RequestBodyAdvice;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.handler.method.ResponseBodyAdvice;

/**
 * ParameterResolvingStrategy registry
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ParameterResolvingStrategy
 * @since 3.0 2019-07-07 23:24
 */
public class ParameterResolvingRegistry extends ApplicationObjectSupport implements ArraySizeTrimmer, InitializingBean {

  private final ParameterResolvingStrategies defaultStrategies = new ParameterResolvingStrategies(36);

  private final ParameterResolvingStrategies customizedStrategies = new ParameterResolvingStrategies();

  /**
   * @since 3.0.1
   */
  private @Nullable RedirectModelManager redirectModelManager;

  // @since 4.0
  @SuppressWarnings("NullAway.Init")
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
  protected ParameterResolvingStrategy lookupStrategy(ResolvableMethodParameter resolvable, Iterable<ParameterResolvingStrategy> strategies) {
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
  public @Nullable ParameterResolvingStrategy findStrategy(ResolvableMethodParameter parameter) {
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
              parameter, "There isn't have a parameter resolver to resolve parameter: [%s] called: [%s] on %s"
              .formatted(parameter.getParameterType(), parameter.getName(), parameter.getMethod()));
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

    ApplicationContext context = applicationContext();
    ConfigurableBeanFactory beanFactory = context.unwrapFactory(ConfigurableBeanFactory.class);
    RedirectModelManager modelManager = getRedirectModelManager();
    if (modelManager == null) {
      logger.info("RedirectModel disabled");
    }

    // Annotation-based argument resolution
    strategies.add(new RequestParamMethodArgumentResolver(beanFactory, false));
    strategies.add(new RequestParamMapMethodArgumentResolver());
    strategies.add(new PathVariableMethodArgumentResolver(beanFactory));
    strategies.add(new PathVariableMapMethodArgumentResolver());
    strategies.add(new MatrixVariableMethodArgumentResolver());
    strategies.add(new MatrixVariableMapMethodArgumentResolver());
    strategies.add(new ModelAttributeMethodProcessor(false));
    strategies.add(new RequestResponseBodyMethodProcessor(getMessageConverters(), contentNegotiationManager, requestResponseBodyAdvice));
    strategies.add(new RequestPartMethodArgumentResolver(getMessageConverters(), this.requestResponseBodyAdvice));
    strategies.add(new RequestHeaderMethodArgumentResolver(beanFactory));
    strategies.add(new RequestHeaderMapMethodArgumentResolver());
    strategies.add(new ExpressionValueMethodArgumentResolver(beanFactory));

    strategies.add(new RequestAttributeMethodArgumentResolver(beanFactory));
    strategies.add(new AutowiredParameterResolver(context));

    // type-based argument resolution
    CookieParameterResolver.register(strategies, beanFactory);

    strategies.add(new RequestContextMethodArgumentResolver());
    strategies.add(new ModelAndViewMethodArgumentResolver());
    strategies.add(new ModelMethodProcessor());
    strategies.add(new MapMethodProcessor());
    strategies.add(new ErrorsMethodArgumentResolver());
    strategies.add(new UriComponentsBuilderParameterStrategy());
    strategies.add(new HttpEntityMethodProcessor(
            getMessageConverters(), contentNegotiationManager, requestResponseBodyAdvice, modelManager));

    // fallback

    strategies.add(new RequestParamMethodArgumentResolver(beanFactory, true));
    strategies.add(new ModelAttributeMethodProcessor(true));

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

  /**
   * Add one or more customized parameter resolving strategies to the registry.
   *
   * @param strategies the parameter resolving strategies to add
   * @since 4.0
   */
  public void addCustomizedStrategies(ParameterResolvingStrategy @Nullable ... strategies) {
    customizedStrategies.add(strategies);
  }

  /**
   * Add one or more default parameter resolving strategies to the registry.
   *
   * @param strategies the parameter resolving strategies to add
   * @since 4.0
   */
  public void addDefaultStrategies(ParameterResolvingStrategy @Nullable ... strategies) {
    defaultStrategies.add(strategies);
  }

  /**
   * Set the redirect model manager to use for redirect scenarios.
   *
   * @param redirectModelManager the redirect model manager to set
   * @since 4.0
   */
  public void setRedirectModelManager(@Nullable RedirectModelManager redirectModelManager) {
    this.redirectModelManager = redirectModelManager;
  }

  /**
   * Get the redirect model manager to use for redirect scenarios.
   *
   * @return the redirect model manager, or {@code null} if not set
   * @since 4.0
   */
  public @Nullable RedirectModelManager getRedirectModelManager() {
    return redirectModelManager;
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
   * Set one or more {@code RequestBodyAdvice} or {@code ResponseBodyAdvice} instances.
   *
   * <p>This method clears any existing advice objects and adds all objects from the provided list.
   * The order of advice objects in the list is preserved during execution.
   *
   * @param list the list of {@code RequestBodyAdvice} or {@code ResponseBodyAdvice} objects,
   * may be {@code null} which results in clearing all existing advice
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
  @Override
  public void trimToSize() {
    defaultStrategies.trimToSize();
    customizedStrategies.trimToSize();
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
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
    protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext context) {
      return context.getAttribute(name);
    }

    @Override
    protected void handleMissingValue(String name, MethodParameter parameter) {
      throw new RequestBindingException("Missing request attribute '%s' of type %s"
              .formatted(name, parameter.getNestedParameterType().getSimpleName()));
    }

  }

}
