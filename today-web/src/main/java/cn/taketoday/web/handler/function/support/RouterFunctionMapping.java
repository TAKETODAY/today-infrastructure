/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.handler.function.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.http.converter.AllEncompassingFormHttpMessageConverter;
import cn.taketoday.http.converter.ByteArrayHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.AbstractHandlerMapping;
import cn.taketoday.web.handler.function.HandlerFunction;
import cn.taketoday.web.handler.function.RouterFunction;
import cn.taketoday.web.handler.function.RouterFunctions;
import cn.taketoday.web.handler.function.ServerRequest;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * {@code HandlerMapping} implementation that supports {@link RouterFunction RouterFunctions}.
 *
 * <p>If no {@link RouterFunction} is provided at
 * {@linkplain #RouterFunctionMapping(RouterFunction) construction time}, this mapping
 * will detect all router functions in the application context, and consult them in
 * {@linkplain cn.taketoday.core.annotation.Order order}.
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
  public RouterFunctionMapping() { }

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
   * is defined in (typically the current DispatcherServlet's context).
   * <p>Switch this flag on to detect handler beans in ancestor contexts
   * (typically the Spring root WebApplicationContext) as well.
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
            .orderedStream()
            .collect(Collectors.toList());

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
    else if (log.isDebugEnabled()) {
      int total = routerFunctions.size();
      String message = total + " RouterFunction(s) in " + formatMappingName();
      if (log.isTraceEnabled()) {
        if (total > 0) {
          routerFunctions.forEach(function -> log.trace("Mapped {}", function));
        }
        else {
          log.trace(message);
        }
      }
      else if (total > 0) {
        log.debug(message);
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
      ServerRequest request = ServerRequest.create(context, messageConverters);
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
