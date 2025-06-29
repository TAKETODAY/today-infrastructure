/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler.method;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import infra.context.ApplicationContext;
import infra.core.DefaultParameterNameDiscoverer;
import infra.core.ParameterNameDiscoverer;
import infra.core.PathMatcher;
import infra.core.StringValueResolver;
import infra.core.annotation.AnnotatedElementUtils;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.core.annotation.RepeatableContainers;
import infra.http.HttpMethod;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.lang.Nullable;
import infra.stereotype.Controller;
import infra.util.CollectionUtils;
import infra.util.StringUtils;
import infra.web.RequestContext;
import infra.web.accept.ApiVersionStrategy;
import infra.web.accept.ContentNegotiationManager;
import infra.web.accept.DefaultApiVersionStrategy;
import infra.web.annotation.CrossOrigin;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.bind.resolver.ParameterResolvingRegistry;
import infra.web.cors.CorsConfiguration;
import infra.web.handler.condition.AbstractRequestCondition;
import infra.web.handler.condition.CompositeRequestCondition;
import infra.web.handler.condition.ConsumesRequestCondition;
import infra.web.handler.condition.RequestCondition;
import infra.web.service.annotation.HttpExchange;

/**
 * Creates {@link RequestMappingInfo} instances from type and method-level
 * {@link RequestMapping @RequestMapping} annotations in
 * {@link Controller @Controller} classes.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/1 22:38
 */
public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping {

  private static final HttpMethod[] EMPTY_REQUEST_METHOD_ARRAY = new HttpMethod[0];

  private Map<String, Predicate<Class<?>>> pathPrefixes = Collections.emptyMap();

  private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

  private final RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();

  @Nullable
  private ParameterResolvingRegistry resolvingRegistry;

  @Nullable
  private ApiVersionStrategy apiVersionStrategy;

  private ResolvableParameterFactory parameterFactory;

  private ParameterNameDiscoverer parameterNameDiscoverer = ParameterNameDiscoverer.getSharedInstance();

  /**
   * Configure path prefixes to apply to controller methods.
   * <p>Prefixes are used to enrich the mappings of every {@code @RequestMapping}
   * method whose controller type is matched by the corresponding
   * {@code Predicate}. The prefix for the first matching predicate is used.
   * <p>Consider using {@link HandlerTypePredicate HandlerTypePredicate}
   * to group controllers.
   *
   * @param prefixes a map with path prefixes as key
   */
  public void setPathPrefixes(Map<String, Predicate<Class<?>>> prefixes) {
    this.pathPrefixes = prefixes.isEmpty()
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(prefixes));
  }

  /**
   * The configured path prefixes as a read-only, possibly empty map.
   */
  public Map<String, Predicate<Class<?>>> getPathPrefixes() {
    return this.pathPrefixes;
  }

  /**
   * Set the {@link ContentNegotiationManager} to use to determine requested media types.
   * If not set, the default constructor is used.
   */
  public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
    Assert.notNull(contentNegotiationManager, "ContentNegotiationManager is required");
    this.contentNegotiationManager = contentNegotiationManager;
    config.setContentNegotiationManager(contentNegotiationManager);
  }

  /**
   * Return the configured {@link ContentNegotiationManager}.
   */
  public ContentNegotiationManager getContentNegotiationManager() {
    return this.contentNegotiationManager;
  }

  /**
   * Set the ParameterNameDiscoverer to use for resolving method parameter names if needed
   * (e.g. for default attribute names).
   * <p>Default is a {@link DefaultParameterNameDiscoverer}.
   */
  public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  public void setResolvingRegistry(@Nullable ParameterResolvingRegistry resolvingRegistry) {
    this.resolvingRegistry = resolvingRegistry;
  }

  /**
   * Configure a strategy to manage API versioning.
   *
   * @param strategy the strategy to use
   * @since 5.0
   */
  public void setApiVersionStrategy(@Nullable ApiVersionStrategy strategy) {
    this.apiVersionStrategy = strategy;
  }

  /**
   * Return the configured {@link ApiVersionStrategy} strategy.
   *
   * @since 5.0
   */
  @Nullable
  public ApiVersionStrategy getApiVersionStrategy() {
    return this.apiVersionStrategy;
  }

  @Override
  public void afterPropertiesSet() {
    config.setPatternParser(getPatternParser());
    config.setContentNegotiationManager(getContentNegotiationManager());
    config.setApiVersionStrategy(getApiVersionStrategy());

    ApplicationContext context = obtainApplicationContext();
    if (resolvingRegistry == null) {
      resolvingRegistry = ParameterResolvingRegistry.get(context);
    }

    this.parameterFactory = new RegistryResolvableParameterFactory(resolvingRegistry, parameterNameDiscoverer);

    super.afterPropertiesSet();
  }

  /**
   * Obtain a {@link RequestMappingInfo.BuilderConfiguration} that can reflects
   * the internal configuration of this {@code HandlerMapping} and can be used
   * to set {@link RequestMappingInfo.Builder#options(RequestMappingInfo.BuilderConfiguration)}.
   * <p>This is useful for programmatic registration of request mappings via
   * {@link #registerHandlerMethod(Object, Method, RequestMappingInfo)}.
   *
   * @return the builder configuration that reflects the internal state
   */
  public RequestMappingInfo.BuilderConfiguration getBuilderConfiguration() {
    return this.config;
  }

  /**
   * {@inheritDoc}
   * <p>Expects a handler to have either a type-level @{@link Controller}
   * annotation or a type-level @{@link RequestMapping} annotation.
   */
  @Override
  protected boolean isHandler(Class<?> beanType) {
    var annotations = MergedAnnotations.from(beanType, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.NONE);
    return annotations.isPresent(Controller.class)
            || annotations.isPresent(RequestMapping.class);
  }

  @Override
  protected HandlerMethod createHandlerMethod(Object handler, Method method) {
    if (handler instanceof String beanName) {
      ApplicationContext context = obtainApplicationContext();
      return new InvocableHandlerMethod(beanName,
              context.getBeanFactory(), context, method, parameterFactory);
    }
    return new InvocableHandlerMethod(handler, method, parameterFactory);
  }

  @Nullable
  @Override
  protected HandlerMethod getHandlerInternal(RequestContext request) {
    if (this.apiVersionStrategy != null) {
      Comparable<?> version = (Comparable<?>) request.getAttribute(API_VERSION_ATTRIBUTE);
      if (version == null) {
        version = apiVersionStrategy.resolveParseAndValidateVersion(request);
        if (version != null) {
          request.setAttribute(API_VERSION_ATTRIBUTE, version);
          apiVersionStrategy.handleDeprecations(version, request);
        }
      }
    }
    return super.getHandlerInternal(request);
  }

  /**
   * Uses method and type-level @{@link RequestMapping} annotations to create
   * the RequestMappingInfo.
   *
   * @return the created RequestMappingInfo, or {@code null} if the method
   * does not have a {@code @RequestMapping} annotation.
   * @see #getCustomCondition(AnnotatedElement)
   */
  @Override
  @Nullable
  protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
    RequestMappingInfo info = createRequestMappingInfo(method);
    if (info != null) {
      if (info.isCombine()) {
        RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
        if (typeInfo != null) {
          info = typeInfo.combine(info);
        }
      }

      if (info.getPathPatternsCondition().isEmptyPathMapping()) {
        info = info.mutate().paths("", "/").options(this.config).build();
      }

      String prefix = getPathPrefix(handlerType);
      if (prefix != null) {
        info = RequestMappingInfo.paths(prefix)
                .options(config)
                .build()
                .combine(info);
      }
    }
    return info;
  }

  @Nullable
  String getPathPrefix(Class<?> handlerType) {
    for (Map.Entry<String, Predicate<Class<?>>> entry : pathPrefixes.entrySet()) {
      if (entry.getValue().test(handlerType)) {
        String prefix = entry.getKey();
        return resolveEmbeddedVariables(prefix);
      }
    }
    return null;
  }

  /**
   * Delegates to {@link #createRequestMappingInfo(RequestMapping, RequestCondition)},
   * supplying the appropriate custom {@link RequestCondition} depending on whether
   * the supplied {@code annotatedElement} is a class or method.
   *
   * @see #getCustomCondition(AnnotatedElement)
   */
  @Nullable
  private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
    RequestMappingInfo requestMappingInfo = null;
    RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
    if (requestMapping != null) {
      requestMappingInfo = createRequestMappingInfo(requestMapping, getCustomCondition(element));
    }

    if (requestMappingInfo == null) {
      HttpExchange httpExchange = AnnotatedElementUtils.findMergedAnnotation(element, HttpExchange.class);
      if (httpExchange != null) {
        requestMappingInfo = createRequestMappingInfo(httpExchange, getCustomCondition(element));
      }
    }

    if (requestMappingInfo != null && this.apiVersionStrategy instanceof DefaultApiVersionStrategy davs) {
      String version = requestMappingInfo.getVersionCondition().getVersion();
      if (version != null) {
        davs.addMappedVersion(version);
      }
    }
    return requestMappingInfo;
  }

  /**
   * Provide a custom method-level or type-level request condition.
   * The custom {@link RequestCondition} can be of any type so long as the
   * same condition type is returned from all calls to this method in order
   * to ensure custom request conditions can be combined and compared.
   * <p>Consider extending {@link AbstractRequestCondition} for custom
   * condition types and using {@link CompositeRequestCondition} to provide
   * multiple custom conditions.
   *
   * @param element the handler method or handler type for which to create the condition
   * @return the condition, or {@code null}
   */
  @Nullable
  protected RequestCondition<?> getCustomCondition(AnnotatedElement element) {
    return null;
  }

  /**
   * Create a {@link RequestMappingInfo} from the supplied
   * {@link RequestMapping @RequestMapping} annotation, which is either
   * a directly declared annotation, a meta-annotation, or the synthesized
   * result of merging annotation attributes within an annotation hierarchy.
   */
  protected RequestMappingInfo createRequestMappingInfo(RequestMapping requestMapping, @Nullable RequestCondition<?> customCondition) {
    var builder = RequestMappingInfo.paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))
            .params(requestMapping.params())
            .methods(requestMapping.method())
            .combine(requestMapping.combine())
            .headers(requestMapping.headers())
            .consumes(requestMapping.consumes())
            .produces(requestMapping.produces())
            .version(requestMapping.version())
            .mappingName(requestMapping.name());

    if (customCondition != null) {
      builder.customCondition(customCondition);
    }
    return builder.options(this.config).build();
  }

  /**
   * Create a {@link RequestMappingInfo} from the supplied
   * {@link HttpExchange @HttpExchange} annotation, or meta-annotation,
   * or synthesized result of merging annotation attributes within an
   * annotation hierarchy.
   */
  protected RequestMappingInfo createRequestMappingInfo(HttpExchange httpExchange, @Nullable RequestCondition<?> customCondition) {
    var builder = RequestMappingInfo.paths(
                    resolveEmbeddedValuesInPatterns(toStringArray(httpExchange.value())))
            .methods(toMethodArray(httpExchange.method()))
            .consumes(toStringArray(httpExchange.contentType()))
            .combine(true)
            .produces(httpExchange.accept())
            .headers(httpExchange.headers());

    if (customCondition != null) {
      builder.customCondition(customCondition);
    }

    return builder.options(this.config).build();
  }

  private static String[] toStringArray(String value) {
    return StringUtils.hasText(value) ? new String[] { value } : Constant.EMPTY_STRING_ARRAY;
  }

  private static HttpMethod[] toMethodArray(String method) {
    return StringUtils.hasText(method) ?
            new HttpMethod[] { HttpMethod.valueOf(method) } : EMPTY_REQUEST_METHOD_ARRAY;
  }

  /**
   * Resolve placeholder values in the given array of patterns.
   *
   * @return a new array with updated patterns
   */
  protected String[] resolveEmbeddedValuesInPatterns(String[] patterns) {
    StringValueResolver embeddedValueResolver = this.embeddedValueResolver;
    if (embeddedValueResolver == null) {
      return patterns;
    }
    else {
      String[] resolvedPatterns = new String[patterns.length];
      for (int i = 0; i < patterns.length; i++) {
        resolvedPatterns[i] = embeddedValueResolver.resolveStringValue(patterns[i]);
      }
      return resolvedPatterns;
    }
  }

  @Override
  public void registerMapping(RequestMappingInfo mapping, Object handler, Method method) {
    super.registerMapping(mapping, handler, method);
    updateConsumesCondition(mapping, method);
  }

  /**
   * {@inheritDoc}
   * <p><strong>Note:</strong> To create the {@link RequestMappingInfo},
   * please use {@link #getBuilderConfiguration()} and set the options on
   * {@link RequestMappingInfo.Builder#options(RequestMappingInfo.BuilderConfiguration)}
   * to match how this {@code HandlerMapping} is configured. This
   * is important for example to ensure use of
   * {@link infra.web.util.pattern.PathPattern} or
   * {@link PathMatcher} based matching.
   *
   * @param handler the bean name of the handler or the handler instance
   * @param method the method to register
   * @param mapping the mapping conditions associated with the handler method
   */
  @Override
  protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
    super.registerHandlerMethod(handler, method, mapping);
    updateConsumesCondition(mapping, method);
  }

  private void updateConsumesCondition(RequestMappingInfo info, Method method) {
    ConsumesRequestCondition condition = info.getConsumesCondition();
    if (!condition.isEmpty()) {
      for (Parameter parameter : method.getParameters()) {
        MergedAnnotation<RequestBody> annot = MergedAnnotations.from(parameter).get(RequestBody.class);
        if (annot.isPresent()) {
          condition.setBodyRequired(annot.getBoolean("required"));
          break;
        }
      }
    }
  }

  @Nullable
  @Override
  protected CorsConfiguration initCorsConfiguration(Object handler,
          HandlerMethod handlerMethod, Method method, RequestMappingInfo mappingInfo) {
    Class<?> beanType = handlerMethod.getBeanType();
    CrossOrigin typeAnnotation = AnnotatedElementUtils.findMergedAnnotation(beanType, CrossOrigin.class);
    CrossOrigin methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, CrossOrigin.class);

    if (typeAnnotation == null && methodAnnotation == null) {
      return null;
    }

    CorsConfiguration config = new CorsConfiguration();
    updateCorsConfig(config, typeAnnotation);
    updateCorsConfig(config, methodAnnotation);

    if (CollectionUtils.isEmpty(config.getAllowedMethods())) {
      for (HttpMethod allowedMethod : mappingInfo.getMethodsCondition().getMethods()) {
        config.addAllowedMethod(allowedMethod.name());
      }
    }
    return config.applyPermitDefaultValues();
  }

  private void updateCorsConfig(CorsConfiguration config, @Nullable CrossOrigin annotation) {
    if (annotation == null) {
      return;
    }
    for (String origin : annotation.origins()) {
      config.addAllowedOrigin(resolveCorsAnnotationValue(origin));
    }
    for (String patterns : annotation.originPatterns()) {
      config.addAllowedOriginPattern(resolveCorsAnnotationValue(patterns));
    }
    for (HttpMethod method : annotation.methods()) {
      config.addAllowedMethod(method.name());
    }
    for (String header : annotation.allowedHeaders()) {
      config.addAllowedHeader(resolveCorsAnnotationValue(header));
    }
    for (String header : annotation.exposedHeaders()) {
      config.addExposedHeader(resolveCorsAnnotationValue(header));
    }

    String allowCredentials = resolveCorsAnnotationValue(annotation.allowCredentials());
    if ("true".equalsIgnoreCase(allowCredentials)) {
      config.setAllowCredentials(true);
    }
    else if ("false".equalsIgnoreCase(allowCredentials)) {
      config.setAllowCredentials(false);
    }
    else if (!allowCredentials.isEmpty()) {
      throw new IllegalStateException(
              "@CrossOrigin's allowCredentials value must be \"true\", \"false\", or an empty string (\"\"): current value is [%s]"
                      .formatted(allowCredentials));
    }

    if (annotation.maxAge() >= 0) {
      config.setMaxAge(annotation.maxAge());
    }
  }

  private String resolveCorsAnnotationValue(String value) {
    if (embeddedValueResolver != null) {
      String resolved = embeddedValueResolver.resolveStringValue(value);
      return resolved != null ? resolved : "";
    }
    else {
      return value;
    }
  }

}
