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

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.expression.EmbeddedValueResolverAware;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogDelegateFactory;
import cn.taketoday.logging.Logger;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContextSupport;
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
