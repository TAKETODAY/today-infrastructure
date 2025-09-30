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

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import infra.context.ApplicationContext;
import infra.core.DefaultParameterNameDiscoverer;
import infra.core.MethodParameter;
import infra.core.ParameterNameDiscoverer;
import infra.core.StringValueResolver;
import infra.core.annotation.AnnotatedElementUtils;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotationPredicates;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.core.annotation.RepeatableContainers;
import infra.http.HttpMethod;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.stereotype.Controller;
import infra.util.CollectionUtils;
import infra.util.PathMatcher;
import infra.util.StringUtils;
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

  @SuppressWarnings("NullAway.Init")
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
    List<AnnotationDescriptor> descriptors = MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
            .stream()
            .filter(MergedAnnotationPredicates.typeIn(RequestMapping.class, HttpExchange.class))
            .filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
            .map(AnnotationDescriptor::new)
            .distinct()
            .toList();

    RequestMappingInfo info = null;

    ArrayList<AnnotationDescriptor> mappingDescriptors = new ArrayList<>();
    ArrayList<AnnotationDescriptor> exchangeDescriptors = new ArrayList<>();

    for (var descriptor : descriptors) {
      if (descriptor.annotation instanceof RequestMapping) {
        mappingDescriptors.add(descriptor);
      }
      else if (descriptor.annotation instanceof HttpExchange) {
        exchangeDescriptors.add(descriptor);
      }
    }

    if (!mappingDescriptors.isEmpty()) {
      checkMultipleAnnotations(element, mappingDescriptors);
      info = createRequestMappingInfo((RequestMapping) mappingDescriptors.get(0).annotation, getCustomCondition(element));
    }

    if (!exchangeDescriptors.isEmpty()) {
      checkMultipleAnnotations(element, info, mappingDescriptors, exchangeDescriptors);
      info = createRequestMappingInfo((HttpExchange) exchangeDescriptors.get(0).annotation, getCustomCondition(element));
    }

    if (info != null && getApiVersionStrategy() instanceof DefaultApiVersionStrategy davs) {
      String version = info.getVersionCondition().getVersion();
      if (version != null) {
        davs.addMappedVersion(version);
      }
    }
    return info;
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

  private void checkMultipleAnnotations(AnnotatedElement element, List<AnnotationDescriptor> mappingDescriptors) {
    if (logger.isWarnEnabled() && mappingDescriptors.size() > 1) {
      logger.warn("Multiple @RequestMapping annotations found on {}, but only the first will be used: {}",
              element, mappingDescriptors);
    }
  }

  private static void checkMultipleAnnotations(AnnotatedElement element, @Nullable RequestMappingInfo info,
          List<AnnotationDescriptor> mappingDescriptors, List<AnnotationDescriptor> exchangeDescriptors) {
    if (info != null) {
      throw new IllegalStateException("%s is annotated with @RequestMapping and @HttpExchange annotations, but only one is allowed: %s"
              .formatted(element, Stream.of(mappingDescriptors, exchangeDescriptors).flatMap(List::stream).toList()));
    }

    if (exchangeDescriptors.size() != 1) {
      throw new IllegalStateException("Multiple @HttpExchange annotations found on %s, but only one is allowed: %s"
              .formatted(element, exchangeDescriptors));
    }
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
  @SuppressWarnings("NullAway")
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
    registerHandlerMethod(handler, method, mapping);
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
  protected HandlerMethod registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
    HandlerMethod handlerMethod = super.registerHandlerMethod(handler, method, mapping);
    updateConsumesCondition(mapping, handlerMethod);
    return handlerMethod;
  }

  private void updateConsumesCondition(RequestMappingInfo info, HandlerMethod handlerMethod) {
    ConsumesRequestCondition condition = info.getConsumesCondition();
    if (!condition.isEmpty()) {
      for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
        RequestBody requestBody = parameter.getParameterAnnotation(RequestBody.class);
        if (requestBody != null) {
          condition.setBodyRequired(requestBody.required());
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

  private static class AnnotationDescriptor {

    public final Annotation annotation;

    public final MergedAnnotation<?> root;

    AnnotationDescriptor(MergedAnnotation<Annotation> mergedAnnotation) {
      this.annotation = mergedAnnotation.synthesize();
      this.root = mergedAnnotation.getRoot();
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof AnnotationDescriptor that && this.annotation.equals(that.annotation));
    }

    @Override
    public int hashCode() {
      return this.annotation.hashCode();
    }

    @Override
    public String toString() {
      return this.root.synthesize().toString();
    }

  }

}
