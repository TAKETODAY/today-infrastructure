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

package infra.web.handler.function.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.beans.factory.InitializingBean;
import infra.context.ApplicationContext;
import infra.core.annotation.Order;
import infra.http.converter.AllEncompassingFormHttpMessageConverter;
import infra.http.converter.ByteArrayHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.web.RequestContext;
import infra.web.accept.DefaultApiVersionStrategy;
import infra.web.handler.AbstractHandlerMapping;
import infra.web.handler.function.HandlerFunction;
import infra.web.handler.function.RouterFunction;
import infra.web.handler.function.RouterFunctions;
import infra.web.handler.function.ServerRequest;
import infra.web.util.pattern.PathPatternParser;

/**
 * {@code HandlerMapping} implementation that supports {@link RouterFunction RouterFunctions}.
 *
 * <p>If no {@link RouterFunction} is provided at
 * {@linkplain #RouterFunctionMapping(RouterFunction) construction time}, this mapping
 * will detect all router functions in the application context, and consult them in
 * {@linkplain Order order}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RouterFunctionMapping extends AbstractHandlerMapping implements InitializingBean {

  @Nullable
  private RouterFunction<?> routerFunction;

  private List<HttpMessageConverter<?>> messageConverters = Collections.emptyList();

  private boolean detectHandlerFunctionsInAncestorContexts = false;

  /**
   * Create an empty {@code RouterFunctionMapping}.
   * <p>If this constructor is used, this mapping will detect all
   * {@link RouterFunction} instances available in the application context.
   */
  public RouterFunctionMapping() {
  }

  /**
   * Create a {@code RouterFunctionMapping} with the given {@link RouterFunction}.
   * <p>If this constructor is used, no application context detection will occur.
   *
   * @param routerFunction the router function to use for mapping
   */
  public RouterFunctionMapping(RouterFunction<?> routerFunction) {
    this.routerFunction = routerFunction;
  }

  /**
   * Set the router function to map to.
   * <p>If this property is used, no application context detection will occur.
   */
  public void setRouterFunction(@Nullable RouterFunction<?> routerFunction) {
    this.routerFunction = routerFunction;
  }

  /**
   * Return the configured {@link RouterFunction}.
   * <p><strong>Note:</strong> When router functions are detected from the
   * ApplicationContext, this method may return {@code null} if invoked
   * prior to {@link #afterPropertiesSet()}.
   *
   * @return the router function or {@code null}
   */
  @Nullable
  public RouterFunction<?> getRouterFunction() {
    return this.routerFunction;
  }

  /**
   * Set the message body converters to use.
   * <p>These converters are used to convert from and to HTTP requests and responses.
   */
  public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
    this.messageConverters = messageConverters;
  }

  /**
   * Set whether to detect handler functions in ancestor ApplicationContexts.
   * <p>Default is "false": Only handler functions in the current ApplicationContext
   * will be detected, i.e. only in the context that this HandlerMapping itself
   * is defined in (typically the current DispatcherHandler's context).
   * <p>Switch this flag on to detect handler beans in ancestor contexts
   * (typically the Infra root WebApplicationContext) as well.
   */
  public void setDetectHandlerFunctionsInAncestorContexts(boolean detectHandlerFunctionsInAncestorContexts) {
    this.detectHandlerFunctionsInAncestorContexts = detectHandlerFunctionsInAncestorContexts;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.routerFunction == null) {
      initRouterFunctions();
    }
    if (CollectionUtils.isEmpty(this.messageConverters)) {
      initMessageConverters();
    }
    if (this.routerFunction != null) {
      PathPatternParser patternParser = getPatternParser();
      RouterFunctions.changeParser(this.routerFunction, patternParser);
      if (getApiVersionStrategy() instanceof DefaultApiVersionStrategy davs) {
        if (davs.detectSupportedVersions()) {
          this.routerFunction.accept(new SupportedVersionVisitor(davs));
        }
      }
    }
  }

  /**
   * Detect all {@linkplain RouterFunction router functions} in the current
   * application context.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void initRouterFunctions() {
    List<RouterFunction> routerFunctions = obtainApplicationContext()
            .getBeanProvider(RouterFunction.class)
            .orderedList();

    ApplicationContext parentContext = obtainApplicationContext().getParent();
    if (parentContext != null && !detectHandlerFunctionsInAncestorContexts) {
      parentContext.getBeanProvider(RouterFunction.class).stream().forEach(routerFunctions::remove);
    }

    this.routerFunction = routerFunctions.stream().reduce(RouterFunction::andOther).orElse(null);
    logRouterFunctions(routerFunctions);
  }

  @SuppressWarnings({ "rawtypes" })
  private void logRouterFunctions(List<RouterFunction> routerFunctions) {
    if (mappingsLogger.isDebugEnabled()) {
      routerFunctions.forEach(function -> mappingsLogger.debug("Mapped {}", function));
    }
    else if (logger.isDebugEnabled()) {
      int total = routerFunctions.size();
      String message = total + " RouterFunction(s) in " + formatMappingName();
      if (logger.isTraceEnabled()) {
        if (total > 0) {
          routerFunctions.forEach(function -> logger.trace("Mapped {}", function));
        }
        else {
          logger.trace(message);
        }
      }
      else if (total > 0) {
        logger.debug(message);
      }
    }
  }

  /**
   * Initializes a default set of {@linkplain HttpMessageConverter message converters}.
   */
  private void initMessageConverters() {
    var messageConverters = new ArrayList<HttpMessageConverter<?>>(4);
    messageConverters.add(new ByteArrayHttpMessageConverter());
    messageConverters.add(new StringHttpMessageConverter());
    messageConverters.add(new AllEncompassingFormHttpMessageConverter());

    this.messageConverters = messageConverters;
  }

  @Nullable
  @Override
  protected Object getHandlerInternal(RequestContext context) throws Exception {
    if (routerFunction != null) {
      ServerRequest request = ServerRequest.create(context, messageConverters, getApiVersionStrategy());
      HandlerFunction<?> handlerFunction = routerFunction.route(request).orElse(null);
      if (handlerFunction != null) {
        context.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, request);
      }
      return handlerFunction;
    }
    else {
      return null;
    }
  }

}
