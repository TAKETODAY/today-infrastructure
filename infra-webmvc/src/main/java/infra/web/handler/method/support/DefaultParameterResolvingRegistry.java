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

package infra.web.handler.method.support;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.core.MethodParameter;
import infra.http.converter.AllEncompassingFormHttpMessageConverter;
import infra.http.converter.ByteArrayHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.Assert;
import infra.util.CollectionUtils;
import infra.web.HttpContext;
import infra.web.RedirectModelManager;
import infra.web.accept.ContentNegotiationManager;
import infra.web.annotation.RequestAttribute;
import infra.web.bind.RequestBindingException;
import infra.web.handler.method.HandlerParameter;
import infra.web.handler.method.ParameterResolvingRegistry;
import infra.web.handler.method.ParameterResolvingStrategies;
import infra.web.handler.method.RequestBodyAdvice;
import infra.web.handler.method.ResponseBodyAdvice;

/**
 * A registry that installs the standard MVC handler method argument resolvers.
 * Custom strategies still take precedence over the built-in strategies.
 *
 * @since 5.0
 */
public class DefaultParameterResolvingRegistry extends ParameterResolvingRegistry
        implements ApplicationContextAware, InitializingBean {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private @Nullable ApplicationContext applicationContext;

  private List<HttpMessageConverter<?>> messageConverters;

  private @Nullable RedirectModelManager redirectModelManager;

  private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

  private final ArrayList<Object> requestResponseBodyAdvice = new ArrayList<>();

  /**
   * Create a registry with the standard MVC message converters.
   */
  public DefaultParameterResolvingRegistry() {
    this.messageConverters = new ArrayList<>(4);
    this.messageConverters.add(new ByteArrayHttpMessageConverter());
    this.messageConverters.add(new StringHttpMessageConverter());
    this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
  }

  /**
   * Create a registry with the supplied message converters.
   * @param messageConverters the converters for reading request bodies
   */
  public DefaultParameterResolvingRegistry(List<HttpMessageConverter<?>> messageConverters) {
    this.messageConverters = messageConverters;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
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

  @Override
  public void afterPropertiesSet() throws Exception {
    if (getDefaultStrategies().isEmpty()) {
      registerDefaultStrategies();
    }
  }

  /**
   * Append the built-in strategies to this registry's default strategies.
   */
  public void registerDefaultStrategies() {
    registerDefaultStrategies(getDefaultStrategies());
  }

  /**
   * Append the built-in strategies to the given collection in MVC lookup order.
   * @param strategies the collection to populate
   */
  public void registerDefaultStrategies(ParameterResolvingStrategies strategies) {
    logger.debug("Registering default parameter-resolvers to {}", strategies);

    ApplicationContext context = this.applicationContext;
    Assert.state(context != null, "ApplicationContext is required");
    ConfigurableBeanFactory beanFactory = context.unwrapFactory(ConfigurableBeanFactory.class);
    RedirectModelManager modelManager = getRedirectModelManager();
    if (modelManager == null) {
      logger.debug("RedirectModel disabled");
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

    strategies.add(new HttpContextMethodArgumentResolver());
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
   * Return the configured registry or register one with the built-in strategies
   * if none is configured in the given application context.
   *
   * @param context the application context to look up the registry in
   * @return the configured or newly registered registry
   */
  public static ParameterResolvingRegistry get(ApplicationContext context) {
    var resolvingRegistry = BeanFactoryUtils.find(context, ParameterResolvingRegistry.class);
    if (resolvingRegistry == null) {
      var defaultRegistry = new DefaultParameterResolvingRegistry();
      defaultRegistry.setApplicationContext(context);
      defaultRegistry.registerDefaultStrategies();
      context.unwrapFactory(ConfigurableBeanFactory.class)
              .registerSingleton("parameterResolvingRegistry", defaultRegistry);
      resolvingRegistry = defaultRegistry;
    }
    return resolvingRegistry;
  }

  private static final class RequestAttributeMethodArgumentResolver extends AbstractNamedValueResolvingStrategy {

    RequestAttributeMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
      super(beanFactory);
    }

    @Override
    public boolean supportsParameter(HandlerParameter parameter) {
      return parameter.hasParameterAnnotation(RequestAttribute.class);
    }

    @Override
    protected @Nullable Object resolveName(String name, HandlerParameter resolvable, HttpContext context) {
      return context.getAttribute(name);
    }

    @Override
    protected void handleMissingValue(String name, MethodParameter parameter) {
      throw new RequestBindingException("Missing request attribute '%s' of type %s"
              .formatted(name, parameter.getNestedParameterType().getSimpleName()));
    }

  }

}
