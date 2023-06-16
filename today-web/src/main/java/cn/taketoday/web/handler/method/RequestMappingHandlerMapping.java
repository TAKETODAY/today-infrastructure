/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.handler.method;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import cn.taketoday.core.StringValueResolver;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.core.annotation.RepeatableContainers;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.annotation.CrossOrigin;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.handler.condition.AbstractRequestCondition;
import cn.taketoday.web.handler.condition.CompositeRequestCondition;
import cn.taketoday.web.handler.condition.ConsumesRequestCondition;
import cn.taketoday.web.handler.condition.RequestCondition;

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

  private Map<String, Predicate<Class<?>>> pathPrefixes = Collections.emptyMap();

  private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

  private final RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();

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
    Assert.notNull(contentNegotiationManager, "ContentNegotiationManager must not be null");
    this.contentNegotiationManager = contentNegotiationManager;
    config.setContentNegotiationManager(contentNegotiationManager);
  }

  /**
   * Return the configured {@link ContentNegotiationManager}.
   */
  public ContentNegotiationManager getContentNegotiationManager() {
    return this.contentNegotiationManager;
  }

  @Override
  public void afterPropertiesSet() {
    config.setPatternParser(getPatternParser());
    config.setContentNegotiationManager(getContentNegotiationManager());

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
    var annotations = MergedAnnotations.from(
            beanType, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none());
    return annotations.isPresent(Controller.class)
            || annotations.isPresent(RequestMapping.class);
  }

  /**
   * Uses method and type-level @{@link RequestMapping} annotations to create
   * the RequestMappingInfo.
   *
   * @return the created RequestMappingInfo, or {@code null} if the method
   * does not have a {@code @RequestMapping} annotation.
   * @see #getCustomMethodCondition(Method)
   * @see #getCustomTypeCondition(Class)
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
   * @see #getCustomTypeCondition(Class)
   * @see #getCustomMethodCondition(Method)
   */
  @Nullable
  private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
    RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
    if (requestMapping != null) {
      RequestCondition<?> customCondition =
              element instanceof Class ? getCustomTypeCondition((Class<?>) element)
                                       : getCustomMethodCondition((Method) element);
      return createRequestMappingInfo(requestMapping, customCondition);
    }
    return null;
  }

  /**
   * Provide a custom type-level request condition.
   * The custom {@link RequestCondition} can be of any type so long as the
   * same condition type is returned from all calls to this method in order
   * to ensure custom request conditions can be combined and compared.
   * <p>Consider extending {@link AbstractRequestCondition} for custom
   * condition types and using {@link CompositeRequestCondition} to provide
   * multiple custom conditions.
   *
   * @param handlerType the handler type for which to create the condition
   * @return the condition, or {@code null}
   */
  @Nullable
  protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
    return null;
  }

  /**
   * Provide a custom method-level request condition.
   * The custom {@link RequestCondition} can be of any type so long as the
   * same condition type is returned from all calls to this method in order
   * to ensure custom request conditions can be combined and compared.
   * <p>Consider extending {@link AbstractRequestCondition} for custom
   * condition types and using {@link CompositeRequestCondition} to provide
   * multiple custom conditions.
   *
   * @param method the handler method for which to create the condition
   * @return the condition, or {@code null}
   */
  @Nullable
  protected RequestCondition<?> getCustomMethodCondition(Method method) {
    return null;
  }

  /**
   * Create a {@link RequestMappingInfo} from the supplied
   * {@link RequestMapping @RequestMapping} annotation, which is either
   * a directly declared annotation, a meta-annotation, or the synthesized
   * result of merging annotation attributes within an annotation hierarchy.
   */
  protected RequestMappingInfo createRequestMappingInfo(
          RequestMapping requestMapping, @Nullable RequestCondition<?> customCondition) {

    var builder = RequestMappingInfo.paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))
            .params(requestMapping.params())
            .methods(requestMapping.method())
            .combine(requestMapping.combine())
            .headers(requestMapping.headers())
            .consumes(requestMapping.consumes())
            .produces(requestMapping.produces())
            .mappingName(requestMapping.name());
    if (customCondition != null) {
      builder.customCondition(customCondition);
    }
    return builder.options(this.config).build();
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
   * {@link cn.taketoday.web.util.pattern.PathPattern} or
   * {@link cn.taketoday.core.PathMatcher} based matching.
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

  @Override
  protected CorsConfiguration initCorsConfiguration(Object handler, HandlerMethod handlerMethod, Method method, RequestMappingInfo mappingInfo) {
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
      throw new IllegalStateException("@CrossOrigin's allowCredentials value must be \"true\", \"false\", " +
              "or an empty string (\"\"): current value is [" + allowCredentials + "]");
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
