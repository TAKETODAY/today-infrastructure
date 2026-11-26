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

import java.util.List;

import infra.beans.factory.BeanNotOfRequiredTypeException;
import infra.beans.factory.NoUniqueBeanDefinitionException;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationContext;
import infra.core.MethodParameter;
import infra.http.converter.AllEncompassingFormHttpMessageConverter;
import infra.http.converter.ByteArrayHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.ServerHttpExchange;
import infra.web.RedirectModelManager;
import infra.web.accept.ContentNegotiationManager;
import infra.web.annotation.RequestAttribute;
import infra.web.bind.RequestBindingException;
import infra.web.handler.method.HandlerParameter;
import infra.web.handler.method.ParameterResolvingRegistry;
import infra.web.handler.method.ParameterResolvingStrategies;

/**
 * Creates a {@link ParameterResolvingRegistry} with the standard MVC handler
 * method argument resolvers.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public final class ParameterResolvingRegistryFactory {

  private static final String REGISTRY_BEAN_NAME = "parameterResolvingRegistry";

  private static final Logger logger = LoggerFactory.getLogger(ParameterResolvingRegistryFactory.class);

  private ParameterResolvingRegistryFactory() {
  }

  /**
   * Create a new registry with the built-in resolvers and fallback message converters.
   *
   * @param context the application context for the resolvers
   * @return the populated registry, not registered as a bean
   */
  public static ParameterResolvingRegistry create(ApplicationContext context) {
    return create(context, List.of(
                    new ByteArrayHttpMessageConverter(),
                    new StringHttpMessageConverter(),
                    new AllEncompassingFormHttpMessageConverter()),
            new ContentNegotiationManager(), null, null);
  }

  /**
   * Create a new registry with the built-in resolvers in MVC lookup order.
   *
   * @param context the application context for the resolvers
   * @param messageConverters the converters for request and response bodies
   * @param contentNegotiationManager the manager for resolving requested media types
   * @param redirectModelManager the redirect model manager, if configured
   * @param requestResponseBodyAdvice the request and response body advice to apply
   * @return the populated registry, not registered as a bean
   */
  public static ParameterResolvingRegistry create(ApplicationContext context,
          List<HttpMessageConverter<?>> messageConverters, ContentNegotiationManager contentNegotiationManager,
          @Nullable RedirectModelManager redirectModelManager, @Nullable List<Object> requestResponseBodyAdvice) {

    var registry = new ParameterResolvingRegistry();
    ParameterResolvingStrategies strategies = registry.getDefaultStrategies();
    logger.debug("Registering default parameter-resolvers to {}", strategies);

    ConfigurableBeanFactory beanFactory = context.unwrapFactory(ConfigurableBeanFactory.class);
    if (redirectModelManager == null) {
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
    strategies.add(new RequestResponseBodyMethodProcessor(messageConverters, contentNegotiationManager, requestResponseBodyAdvice));
    strategies.add(new RequestPartMethodArgumentResolver(messageConverters, requestResponseBodyAdvice));
    strategies.add(new RequestHeaderMethodArgumentResolver(beanFactory));
    strategies.add(new RequestHeaderMapMethodArgumentResolver());
    strategies.add(new ExpressionValueMethodArgumentResolver(beanFactory));

    strategies.add(new RequestAttributeMethodArgumentResolver(beanFactory));
    strategies.add(new AutowiredParameterResolver(context));

    // type-based argument resolution
    CookieParameterResolver.register(strategies, beanFactory);

    strategies.add(new HttpExchangeMethodArgumentResolver());
    strategies.add(new ModelAndViewMethodArgumentResolver());
    strategies.add(new ModelMethodProcessor());
    strategies.add(new MapMethodProcessor());
    strategies.add(new ErrorsMethodArgumentResolver());
    strategies.add(new UriComponentsBuilderParameterStrategy());
    strategies.add(new HttpEntityMethodProcessor(
            messageConverters, contentNegotiationManager, requestResponseBodyAdvice, redirectModelManager));

    // fallback

    strategies.add(new RequestParamMethodArgumentResolver(beanFactory, true));
    strategies.add(new ModelAttributeMethodProcessor(true));

    strategies.trimToSize();
    return registry;
  }

  /**
   * Return the configured registry or register one with the built-in strategies
   * if none is configured in the given application context.
   *
   * @param context the application context to look up the registry in
   * @return the configured or newly registered registry
   * @throws NoUniqueBeanDefinitionException if multiple registries match and none can be selected
   */
  public static ParameterResolvingRegistry getOrCreate(ApplicationContext context) {
    var registry = context.getBeanProvider(ParameterResolvingRegistry.class).getIfAvailable();
    if (registry != null) {
      return registry;
    }

    var beanFactory = context.unwrapFactory(ConfigurableBeanFactory.class);
    // Coordinate fallback creation without holding this lock during typed bean lookup.
    synchronized(beanFactory) {
      if (beanFactory.containsSingleton(REGISTRY_BEAN_NAME)) {
        Object existing = beanFactory.getSingleton(REGISTRY_BEAN_NAME);
        if (existing instanceof ParameterResolvingRegistry existingRegistry) {
          return existingRegistry;
        }
        if (existing != null) {
          throw new BeanNotOfRequiredTypeException(REGISTRY_BEAN_NAME, ParameterResolvingRegistry.class, existing.getClass());
        }
      }
      if (beanFactory.containsLocalBean(REGISTRY_BEAN_NAME)) {
        throw new IllegalStateException("Bean name '%s' is already in use".formatted(REGISTRY_BEAN_NAME));
      }
      registry = create(context);
      beanFactory.registerSingleton(REGISTRY_BEAN_NAME, registry);
      return registry;
    }
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
    protected @Nullable Object resolveName(String name, HandlerParameter resolvable, ServerHttpExchange context) {
      return context.getAttribute(name);
    }

    @Override
    protected void handleMissingValue(String name, MethodParameter parameter) {
      throw new RequestBindingException("Missing request attribute '%s' of type %s"
              .formatted(name, parameter.getNestedParameterType().getSimpleName()));
    }

  }

}
