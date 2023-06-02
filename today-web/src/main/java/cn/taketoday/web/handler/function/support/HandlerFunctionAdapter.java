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

package cn.taketoday.web.handler.function.support;

import java.util.List;

import cn.taketoday.core.Ordered;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.web.HandlerAdapter;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.context.async.WebAsyncManager;
import cn.taketoday.web.handler.function.HandlerFunction;
import cn.taketoday.web.handler.function.ServerRequest;
import cn.taketoday.web.handler.function.ServerResponse;

/**
 * {@code HandlerAdapter} implementation that supports {@link HandlerFunction}s.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HandlerFunctionAdapter implements HandlerAdapter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(HandlerFunctionAdapter.class);

  private int order = Ordered.LOWEST_PRECEDENCE;

  /**
   * Specify the order value for this HandlerAdapter bean.
   * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
   *
   * @see cn.taketoday.core.Ordered#getOrder()
   */
  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  @Override
  public boolean supports(Object handler) {
    return handler instanceof HandlerFunction;
  }

  @Nullable
  @Override
  public Object handle(RequestContext context, Object handler) throws Throwable {
    WebAsyncManager asyncManager = context.getAsyncManager();

    ServerRequest serverRequest = ServerRequest.findRequired(context);
    ServerResponse serverResponse;

    if (asyncManager.hasConcurrentResult()) {
      serverResponse = handleAsync(asyncManager);
    }
    else {
      HandlerFunction<?> handlerFunction = (HandlerFunction<?>) handler;
      serverResponse = handlerFunction.handle(serverRequest);
    }

    if (serverResponse != null) {
      return serverResponse.writeTo(context, new ServerRequestContext(serverRequest));
    }
    else {
      return NONE_RETURN_VALUE;
    }
  }

  @Nullable
  private ServerResponse handleAsync(WebAsyncManager asyncManager) throws Throwable {
    Object result = asyncManager.getConcurrentResult();
    asyncManager.clearConcurrentResult();
    LogFormatUtils.traceDebug(logger, traceOn -> {
      String formatted = LogFormatUtils.formatValue(result, !traceOn);
      return "Resume with async result [" + formatted + "]";
    });
    if (result instanceof ServerResponse) {
      return (ServerResponse) result;
    }
    else if (result instanceof Throwable) {
      throw (Throwable) result;
    }
    else if (result == null) {
      return null;
    }
    else {
      throw new IllegalArgumentException("Unknown result from WebAsyncManager: [" + result + "]");
    }
  }

  private static class ServerRequestContext implements ServerResponse.Context {

    private final ServerRequest serverRequest;

    public ServerRequestContext(ServerRequest serverRequest) {
      this.serverRequest = serverRequest;
    }

    @Override
    public List<HttpMessageConverter<?>> messageConverters() {
      return this.serverRequest.messageConverters();
    }
  }
}
