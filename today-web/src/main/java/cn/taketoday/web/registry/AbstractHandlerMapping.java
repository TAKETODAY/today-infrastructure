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

package cn.taketoday.web.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.context.aware.ApplicationContextSupport;
import cn.taketoday.context.expression.EmbeddedValueResolverAware;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogDelegateFactory;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.HandlerMapping;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.cors.CorsConfigurationSource;
import cn.taketoday.web.cors.CorsProcessor;
import cn.taketoday.web.cors.DefaultCorsProcessor;
import cn.taketoday.web.cors.UrlBasedCorsConfigurationSource;
import cn.taketoday.web.handler.HandlerExecutionChain;
import cn.taketoday.web.handler.MappedInterceptor;
import cn.taketoday.web.util.pattern.PathPatternParser;

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
public abstract class AbstractHandlerMapping extends ApplicationContextSupport
        implements HandlerMapping, Ordered, EmbeddedValueResolverAware, BeanNameAware {

  /**
   * Dedicated "hidden" logger for request mappings.
   */
  protected final Logger mappingsLogger =
          LogDelegateFactory.getHiddenLog(HandlerMapping.class.getName() + ".Mappings");

  private Object defaultHandler;

  private int order = Ordered.LOWEST_PRECEDENCE;

  @Nullable
  private String beanName;

  /** @since 4.0 */
  protected StringValueResolver embeddedValueResolver;

  private final PathPatternParser patternParser = new PathPatternParser();

  private final ArrayList<HandlerInterceptor> interceptors = new ArrayList<>();

  @Nullable
  private CorsConfigurationSource corsConfigurationSource;

  private CorsProcessor corsProcessor = new DefaultCorsProcessor();

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
    Assert.notNull(source, "CorsConfigurationSource must not be null");
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
    Assert.notNull(corsProcessor, "CorsProcessor must not be null");
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
  public void setDefaultHandler(Object defaultHandler) {
    this.defaultHandler = defaultHandler;
  }

  /**
   * Return the default handler for this handler mapping, or {@code null} if none.
   */
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
  protected void extendInterceptors(List<HandlerInterceptor> interceptors) { }

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
  @Override
  public final Object getHandler(final RequestContext request) throws Exception {
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

    HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);

    if (hasCorsConfigurationSource(handler) || request.isPreFlightRequest()) {
      CorsConfiguration config = getCorsConfiguration(handler, request);
      CorsConfigurationSource source = getCorsConfigurationSource();
      if (source != null) {
        CorsConfiguration globalConfig = source.getCorsConfiguration(request);
        if (globalConfig != null) {
          config = globalConfig.combine(config);
        }
      }
      if (config != null) {
        config.validateAllowCredentials();
      }
      executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
    }

    if (!request.hasMatchingMetadata()) {
      request.setMatchingMetadata(new HandlerMatchingMetadata(handler, request, patternParser));
    }

    return executionChain;
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
   * <p>For simply adding an interceptor in a custom subclass, consider calling
   * {@code super.getHandlerExecutionChain(handler, request)} and invoking
   * {@link HandlerExecutionChain#addInterceptor} on the returned chain object.
   *
   * @param handler the resolved handler instance (never {@code null})
   * @param request current HTTP request
   * @return the HandlerExecutionChain (never {@code null})
   * @since 4.0
   */
  protected HandlerExecutionChain getHandlerExecutionChain(Object handler, RequestContext request) {
    var chain = handler instanceof HandlerExecutionChain executionChain
                ? executionChain : new HandlerExecutionChain(handler);

    HandlerInterceptor[] interceptors = getHandlerInterceptors(handler);
    chain.addInterceptors(interceptors);

    for (HandlerInterceptor interceptor : this.interceptors) {
      if (interceptor instanceof MappedInterceptor mappedInterceptor) {
        if (mappedInterceptor.matches(request)) {
          chain.addInterceptor(mappedInterceptor.getInterceptor());
        }
      }
      else {
        chain.addInterceptor(interceptor);
      }
    }
    return chain;
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
    if (handler instanceof HandlerExecutionChain handlerExecutionChain) {
      handler = handlerExecutionChain.getHandler();
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
    if (handler instanceof HandlerExecutionChain chain) {
      resolvedHandler = chain.getHandler();
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
   * @param chain the handler chain
   * @param config the applicable CORS configuration (possibly {@code null})
   * @since 4.0
   */
  protected HandlerExecutionChain getCorsHandlerExecutionChain(
          RequestContext request, HandlerExecutionChain chain, @Nullable CorsConfiguration config) {

    if (request.isPreFlightRequest()) {
      HandlerInterceptor[] interceptors = chain.getInterceptors();
      return new HandlerExecutionChain(new PreFlightHandler(config), interceptors);
    }
    else {
      chain.addInterceptor(0, new CorsInterceptor(config));
      return chain;
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
