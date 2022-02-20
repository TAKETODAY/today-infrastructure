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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.expression.EmbeddedValueResolverAware;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.cors.CorsConfigurationSource;
import cn.taketoday.web.cors.CorsProcessor;
import cn.taketoday.web.cors.DefaultCorsProcessor;
import cn.taketoday.web.cors.UrlBasedCorsConfigurationSource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogDelegateFactory;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * Abstract base class for {@link HandlerRegistry}
 * implementations. Supports ordering, a default handler, handler interceptors,
 * including handler interceptors mapped by path patterns.
 *
 * @author TODAY
 * @see #lookup(RequestContext)
 * @since 2019-12-24 15:02
 */
public abstract class AbstractHandlerRegistry
        extends WebApplicationContextSupport
        implements HandlerRegistry, Ordered, EmbeddedValueResolverAware, BeanNameAware {
  /**
   * Dedicated "hidden" logger for request mappings.
   */
  protected final Logger mappingsLogger =
          LogDelegateFactory.getHiddenLog(HandlerRegistry.class.getName() + ".Mappings");

  private Object defaultHandler;

  private int order = Ordered.LOWEST_PRECEDENCE;

  @Nullable
  private String beanName;

  /** @since 4.0 */
  private StringValueResolver embeddedValueResolver;

  private final PathPatternParser patternParser = new PathPatternParser();

  private final List<Object> interceptors = new ArrayList<>();

  private final List<HandlerInterceptor> adaptedInterceptors = new ArrayList<>();

  @Nullable
  private CorsConfigurationSource corsConfigurationSource;

  private CorsProcessor corsProcessor = new DefaultCorsProcessor();

  /**
   * Return the {@link #setCorsConfigurationSource(CorsConfigurationSource)
   * configured} {@code CorsConfigurationSource}, if any.
   *
   * @since .0
   */
  @Nullable
  public CorsConfigurationSource getCorsConfigurationSource() {
    return this.corsConfigurationSource;
  }

  /**
   * Look up a handler for the given request, falling back to the default
   * handler if no specific one is found.
   *
   * @param context current HTTP request context
   * @return the corresponding handler instance, or the default handler
   * @see #lookupInternal
   */
  @Override
  public final Object lookup(final RequestContext context) {
    Object handler = lookupInternal(context);
    if (handler == null) {
      handler = getDefaultHandler();
    }

    if (handler instanceof String) {
      handler = obtainApplicationContext().getBean((String) handler);
    }
    return handler;
  }

  /**
   * Look up a handler for the given request, returning {@code null} if no
   * specific one is found. This method is called by {@link #lookup};
   *
   * @param context current HTTP request context
   * @return the corresponding handler instance, or {@code null} if none found
   */
  @Nullable
  protected abstract Object lookupInternal(RequestContext context);

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
    this.interceptors.addAll(Arrays.asList(interceptors));
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
      UrlBasedCorsConfigurationSource source;
      if (getPatternParser() != null) {
        source = new UrlBasedCorsConfigurationSource(getPatternParser());
        source.setCorsConfigurations(corsConfigurations);
      }
      else {
        source = new UrlBasedCorsConfigurationSource();
        source.setCorsConfigurations(corsConfigurations);
      }
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

  @Override
  protected void initApplicationContext(ApplicationContext context) {
    super.initApplicationContext(context);
  }

  /**
   * @since 4.0
   */
  @Override
  public void setEmbeddedValueResolver(StringValueResolver resolver) {
    this.embeddedValueResolver = resolver;
  }

  /** @since 3.0.3 */
  public String resolveVariables(String expression) {
    if (embeddedValueResolver != null) {
      return embeddedValueResolver.resolveStringValue(expression);
    }
    return expression;
  }

}
