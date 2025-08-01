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

package infra.web.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.BeanNameAware;
import infra.context.expression.EmbeddedValueResolverAware;
import infra.context.support.ApplicationObjectSupport;
import infra.core.Ordered;
import infra.core.StringValueResolver;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.LogDelegateFactory;
import infra.logging.Logger;
import infra.util.CollectionUtils;
import infra.web.HandlerInterceptor;
import infra.web.HandlerMapping;
import infra.web.HandlerMatchingMetadata;
import infra.web.HandlerWrapper;
import infra.web.HttpRequestHandler;
import infra.web.RequestContext;
import infra.web.accept.ApiVersionStrategy;
import infra.web.cors.CorsConfiguration;
import infra.web.cors.CorsConfigurationSource;
import infra.web.cors.CorsProcessor;
import infra.web.cors.DefaultCorsProcessor;
import infra.web.cors.UrlBasedCorsConfigurationSource;
import infra.web.util.pattern.PathPatternParser;

/**
 * Abstract base class for {@link HandlerMapping}
 * implementations. Supports ordering, a default handler, handler interceptors,
 * including handler interceptors mapped by path patterns.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getHandlerInternal
 * @see #setDefaultHandler
 * @see #setInterceptors
 * @see HandlerInterceptor
 * @since 2019-12-24 15:02
 */
public abstract class AbstractHandlerMapping extends ApplicationObjectSupport
        implements HandlerMapping, Ordered, EmbeddedValueResolverAware, BeanNameAware {

  /**
   * Dedicated "hidden" logger for request mappings.
   */
  protected final Logger mappingsLogger =
          LogDelegateFactory.getHiddenLog(HandlerMapping.class.getName() + ".Mappings");

  @Nullable
  private Object defaultHandler;

  private int order = Ordered.LOWEST_PRECEDENCE;

  @Nullable
  private String beanName;

  /**
   * @since 4.0
   */
  @Nullable
  protected StringValueResolver embeddedValueResolver;

  private final PathPatternParser patternParser = new PathPatternParser();

  private final ArrayList<HandlerInterceptor> interceptors = new ArrayList<>();

  @Nullable
  private CorsConfigurationSource corsConfigurationSource;

  private CorsProcessor corsProcessor = new DefaultCorsProcessor();

  @Nullable
  private ApiVersionStrategy apiVersionStrategy;

  /**
   * Shortcut method for setting the same property on the underlying pattern
   * parser in use. For more details see:
   * <ul>
   * <li>{@link #getPatternParser()} -- the underlying pattern parser
   * <li>{@link PathPatternParser#setMatchOptionalTrailingSeparator(boolean)} --
   * the trailing slash option, including its default value.
   * </ul>
   */
  public void setUseTrailingSlashMatch(boolean trailingSlashMatch) {
    this.patternParser.setMatchOptionalTrailingSeparator(trailingSlashMatch);
  }

  /**
   * Set the interceptors to apply for all handlers mapped by this handler registry.
   *
   * @param interceptors array of handler interceptors
   * @since 4.0
   */
  public void setInterceptors(Object... interceptors) {
    CollectionUtils.addAll(this.interceptors, interceptors);
  }

  /**
   * Set "global" CORS configuration mappings. The first matching URL pattern
   * determines the {@code CorsConfiguration} to use which is then further
   * {@link CorsConfiguration#combine(CorsConfiguration) combined} with the
   * {@code CorsConfiguration} for the selected handler.
   * <p>This is mutually exclusive with
   * {@link #setCorsConfigurationSource(CorsConfigurationSource)}.
   *
   * @see #setCorsProcessor(CorsProcessor)
   * @since 4.0
   */
  public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
    if (CollectionUtils.isEmpty(corsConfigurations)) {
      this.corsConfigurationSource = null;
    }
    else {
      var source = new UrlBasedCorsConfigurationSource(getPatternParser());
      source.setCorsConfigurations(corsConfigurations);
      setCorsConfigurationSource(source);
    }
  }

  /**
   * Set a {@code CorsConfigurationSource} for "global" CORS config. The
   * {@code CorsConfiguration} determined by the source is
   * {@link CorsConfiguration#combine(CorsConfiguration) combined} with the
   * {@code CorsConfiguration} for the selected handler.
   * <p>This is mutually exclusive with {@link #setCorsConfigurations(Map)}.
   *
   * @see #setCorsProcessor(CorsProcessor)
   * @since 4.0
   */
  public void setCorsConfigurationSource(CorsConfigurationSource source) {
    Assert.notNull(source, "CorsConfigurationSource is required");
    this.corsConfigurationSource = source;
  }

  /**
   * Configure a custom {@link CorsProcessor} to use to apply the matched
   * {@link CorsConfiguration} for a request.
   * <p>By default {@link DefaultCorsProcessor} is used.
   *
   * @since 4.0
   */
  public void setCorsProcessor(CorsProcessor corsProcessor) {
    Assert.notNull(corsProcessor, "CorsProcessor is required");
    this.corsProcessor = corsProcessor;
  }

  /**
   * Return the configured {@link CorsProcessor}.
   *
   * @since 4.0
   */
  public CorsProcessor getCorsProcessor() {
    return this.corsProcessor;
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

  /**
   * Specify the order value for this HandlerMapping bean.
   * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
   *
   * @see Ordered#getOrder()
   */
  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return order;
  }

  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  protected String formatMappingName() {
    return this.beanName != null ? "'" + this.beanName + "'" : getClass().getName();
  }

  /**
   * @since 4.0
   */
  @Override
  public void setEmbeddedValueResolver(StringValueResolver resolver) {
    this.embeddedValueResolver = resolver;
  }

  /** @since 3.0.3 */
  @Nullable
  protected String resolveEmbeddedVariables(String expression) {
    if (embeddedValueResolver != null) {
      return embeddedValueResolver.resolveStringValue(expression);
    }
    return expression;
  }

  /**
   * Return the {@link #setCorsConfigurationSource(CorsConfigurationSource)
   * configured} {@code CorsConfigurationSource}, if any.
   *
   * @since 4.0
   */
  @Nullable
  public CorsConfigurationSource getCorsConfigurationSource() {
    return this.corsConfigurationSource;
  }

  /**
   * Set the default handler for this handler registry. This handler will be
   * returned if no specific mapping was found.
   * <p>
   * Default is {@code null}, indicating no default handler.
   */
  public void setDefaultHandler(@Nullable Object defaultHandler) {
    this.defaultHandler = defaultHandler;
  }

  /**
   * Return the default handler for this handler mapping, or {@code null} if none.
   */
  @Nullable
  public Object getDefaultHandler() {
    return this.defaultHandler;
  }

  /**
   * Return the {@link PathPatternParser}
   *
   * @since 4.0
   */
  public PathPatternParser getPatternParser() {
    return this.patternParser;
  }

  /**
   * Shortcut method for setting the same property on the underlying pattern
   * parser in use. For more details see:
   * <ul>
   * <li>{@link #getPatternParser()} -- the underlying pattern parser
   * <li>{@link PathPatternParser#setCaseSensitive(boolean)} -- the case
   * sensitive slash option, including its default value.
   * </ul>
   * <p><strong>Note:</strong> aside from
   */
  public void setUseCaseSensitiveMatch(boolean caseSensitiveMatch) {
    this.patternParser.setCaseSensitive(caseSensitiveMatch);
  }

  /**
   * Initializes the interceptors.
   *
   * @see #extendInterceptors(java.util.List)
   * @since 4.0
   */
  @Override
  protected void initApplicationContext() throws BeansException {
    extendInterceptors(this.interceptors);
    detectMappedInterceptors(this.interceptors);
  }

  /**
   * Extension hook that subclasses can override to register additional interceptors,
   * given the configured interceptors (see {@link #setInterceptors}).
   * <p>The default implementation is empty.
   *
   * @param interceptors the configured interceptor List (never {@code null}), allowing
   * to add further interceptors before as well as after the existing interceptors
   * @since 4.0
   */
  protected void extendInterceptors(List<HandlerInterceptor> interceptors) {

  }

  /**
   * Detect beans of type {@link MappedInterceptor} and add them to the list
   * of mapped interceptors.
   * <p>This is called in addition to any {@link MappedInterceptor}s that may
   * have been provided via {@link #setInterceptors}, by default adding all
   * beans of type {@link MappedInterceptor} from the current context and its
   * ancestors. Subclasses can override and refine this policy.
   *
   * @param mappedInterceptors an empty list to add to
   * @since 4.0
   */
  protected void detectMappedInterceptors(List<HandlerInterceptor> mappedInterceptors) {
    mappedInterceptors.addAll(BeanFactoryUtils.beansOfTypeIncludingAncestors(
            obtainApplicationContext(), MappedInterceptor.class, true, false).values());
  }

  /**
   * Look up a handler for the given request, falling back to the default
   * handler if no specific one is found.
   *
   * @param request current HTTP request context
   * @return the corresponding handler instance, or the default handler
   * @see #getHandlerInternal
   */
  @Nullable
  @Override
  public final Object getHandler(final RequestContext request) throws Exception {
    Comparable<?> version = null;
    if (this.apiVersionStrategy != null) {
      version = (Comparable<?>) request.getAttribute(API_VERSION_ATTRIBUTE);
      if (version == null) {
        version = apiVersionStrategy.resolveParseAndValidateVersion(request);
        if (version != null) {
          request.setAttribute(API_VERSION_ATTRIBUTE, version);
        }
      }
    }
    Object handler = getHandlerInternal(request);
    if (handler == null) {
      handler = getDefaultHandler();
    }
    if (handler == null) {
      return null;
    }

    if (handler instanceof String) {
      handler = obtainApplicationContext().getBean((String) handler);
    }

    HandlerExecutionChain chain;
    if (hasCorsConfigurationSource(handler) || request.isPreFlightRequest()) {
      // handler config
      CorsConfiguration config = getCorsConfiguration(handler, request);
      CorsConfigurationSource global = getCorsConfigurationSource();
      if (global != null) {
        // global config
        CorsConfiguration globalConfig = global.getCorsConfiguration(request);
        if (globalConfig != null) {
          config = globalConfig.combine(config);
        }
      }
      if (config != null) {
        config.validateAllowCredentials();
        config.validateAllowPrivateNetwork();
      }
      chain = getCorsHandlerExecutionChain(request, handler, config);
    }
    else {
      chain = getHandlerExecutionChain(handler, null);
    }

    if (!request.hasMatchingMetadata()) {
      request.setMatchingMetadata(new HandlerMatchingMetadata(handler, request, patternParser));
    }

    if (version != null) {
      apiVersionStrategy.handleDeprecations(version, request);
    }
    return chain;
  }

  /**
   * Look up a handler for the given request, returning {@code null} if no
   * specific one is found. This method is called by {@link #getHandler};
   * a {@code null} return value will lead to the default handler, if one is set.
   * <p>On CORS pre-flight requests this method should return a match not for
   * the pre-flight request but for the expected actual request based on the URL
   * path, the HTTP methods from the "Access-Control-Request-Method" header, and
   * the headers from the "Access-Control-Request-Headers" header thus allowing
   * the CORS configuration to be obtained via {@link #getCorsConfiguration(Object, RequestContext)},
   * <p>Note: This method may also return a pre-built {@link HandlerExecutionChain},
   * combining a handler object with dynamically determined interceptors.
   * Statically specified interceptors will get merged into such an existing chain.
   *
   * @param request current HTTP request
   * @return the corresponding handler instance, or {@code null} if none found
   * @throws Exception if there is an internal error
   */
  @Nullable
  protected abstract Object getHandlerInternal(RequestContext request) throws Exception;

  /**
   * Build a {@link HandlerExecutionChain} for the given handler, including
   * applicable interceptors.
   * <p>The default implementation builds a standard {@link HandlerExecutionChain}
   * with the given handler, the common interceptors of the handler mapping, and any
   * {@link MappedInterceptor MappedInterceptors} matching to the current request URL. Interceptors
   * are added in the order they were registered. Subclasses may override this
   * in order to extend/rearrange the list of interceptors.
   * <p><b>NOTE:</b> The passed-in handler object may be a raw handler or a
   * pre-built {@link HandlerExecutionChain}. This method should handle those
   * two cases explicitly, either building a new {@link HandlerExecutionChain}
   * or extending the existing chain.
   *
   * @param handler the resolved handler instance (never {@code null})
   * @return the HandlerExecutionChain (never {@code null})
   * @since 4.0
   */
  protected HandlerExecutionChain getHandlerExecutionChain(Object handler, @Nullable HandlerInterceptor firstInterceptor) {
    ArrayList<HandlerInterceptor> interceptors = null;
    if (firstInterceptor != null) {
      interceptors = new ArrayList<>(4);
      interceptors.add(firstInterceptor);
    }

    HandlerInterceptor[] ia = getHandlerInterceptors(handler);
    if (ia != null) {
      if (interceptors == null) {
        interceptors = new ArrayList<>(ia.length + 2);
      }
      for (HandlerInterceptor interceptor : ia) {
        interceptors.add(interceptor);
      }
    }
    var global = this.interceptors;
    if (!global.isEmpty()) {
      if (interceptors == null) {
        interceptors = new ArrayList<>(global);
      }
      else {
        interceptors.addAll(global);
      }
    }

    return new HandlerExecutionChain(handler, interceptors == null
            ? null : interceptors.toArray(new HandlerInterceptor[interceptors.size()]));
  }

  @Nullable
  protected HandlerInterceptor[] getHandlerInterceptors(Object handler) {
    return null;
  }

  /**
   * Return {@code true} if there is a {@link CorsConfigurationSource} for this handler.
   *
   * @since 4.0
   */
  protected boolean hasCorsConfigurationSource(Object handler) {
    if (handler instanceof HandlerWrapper wrapper) {
      handler = wrapper.getRawHandler();
    }
    return handler instanceof CorsConfigurationSource || this.corsConfigurationSource != null;
  }

  /**
   * Retrieve the CORS configuration for the given handler.
   *
   * @param handler the handler to check (never {@code null}).
   * @param request the current request.
   * @return the CORS configuration for the handler, or {@code null} if none
   * @since 4.0
   */
  @Nullable
  protected CorsConfiguration getCorsConfiguration(Object handler, RequestContext request) {
    Object resolvedHandler = handler;
    if (handler instanceof HandlerWrapper wrapper) {
      resolvedHandler = wrapper.getRawHandler();
    }
    if (resolvedHandler instanceof CorsConfigurationSource configSource) {
      return configSource.getCorsConfiguration(request);
    }
    return null;
  }

  /**
   * Update the HandlerExecutionChain for CORS-related handling.
   * <p>For pre-flight requests, the default implementation replaces the selected
   * handler with a simple HttpRequestHandler that invokes the configured
   * {@link #setCorsProcessor}.
   * <p>For actual requests, the default implementation inserts a
   * HandlerInterceptor that makes CORS-related checks and adds CORS headers.
   *
   * @param request the current request
   * @param handler the handler
   * @param config the applicable CORS configuration (possibly {@code null})
   * @since 4.0
   */
  protected HandlerExecutionChain getCorsHandlerExecutionChain(RequestContext request, Object handler, @Nullable CorsConfiguration config) {
    if (request.isPreFlightRequest()) {
      ArrayList<HandlerInterceptor> interceptors = new ArrayList<>();
      HandlerInterceptor[] interceptorsArr = getHandlerInterceptors(handler);
      if (interceptorsArr != null) {
        for (HandlerInterceptor interceptor : interceptorsArr) {
          interceptors.add(interceptor);
        }
      }
      if (!this.interceptors.isEmpty()) {
        interceptors.addAll(this.interceptors);
      }
      return new HandlerExecutionChain(new PreFlightHandler(config),
              interceptors.isEmpty() ? null : interceptors.toArray(new HandlerInterceptor[interceptors.size()]));
    }
    else {
      return getHandlerExecutionChain(handler, new CorsInterceptor(config));
    }
  }

  private class PreFlightHandler implements HttpRequestHandler, CorsConfigurationSource {

    @Nullable
    private final CorsConfiguration config;

    public PreFlightHandler(@Nullable CorsConfiguration config) {
      this.config = config;
    }

    @Override
    public Object handleRequest(RequestContext request) throws Throwable {
      corsProcessor.process(this.config, request);
      return NONE_RETURN_VALUE;
    }

    @Override
    @Nullable
    public CorsConfiguration getCorsConfiguration(RequestContext request) {
      return this.config;
    }

  }

  private class CorsInterceptor implements HandlerInterceptor, CorsConfigurationSource {

    @Nullable
    private final CorsConfiguration config;

    public CorsInterceptor(@Nullable CorsConfiguration config) {
      this.config = config;
    }

    @Override
    public boolean beforeProcess(RequestContext request, Object handler) throws Throwable {
      return corsProcessor.process(config, request);
    }

    @Override
    @Nullable
    public CorsConfiguration getCorsConfiguration(RequestContext request) {
      return this.config;
    }

  }

}
