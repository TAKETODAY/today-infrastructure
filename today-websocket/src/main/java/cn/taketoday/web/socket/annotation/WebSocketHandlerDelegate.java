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

package cn.taketoday.web.socket.annotation;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.util.pattern.PathPattern;

/**
 * Annotation {@link WebSocketHandler} Delegate
 *
 * @author TODAY 2021/5/8 22:43
 * @since 3.0.1
 */
public class WebSocketHandlerDelegate {

  // @since 4.0 more efficient
  protected final PathPattern pathPattern;
  @Nullable
  protected final WebSocketHandlerMethod onOpen;
  @Nullable
  protected final WebSocketHandlerMethod onClose;
  @Nullable
  protected final WebSocketHandlerMethod onError;
  @Nullable
  protected final WebSocketHandlerMethod onMessage;
  @Nullable
  protected final ActionMappingAnnotationHandler afterHandshake;

  protected final boolean containsPathVariable;

  public WebSocketHandlerDelegate(PathPattern pathPattern, // @formatter:off
                                   boolean containsPathVariable,
                                   @Nullable WebSocketHandlerMethod onOpen,
                                   @Nullable WebSocketHandlerMethod onClose,
                                   @Nullable WebSocketHandlerMethod onError,
                                   @Nullable WebSocketHandlerMethod onMessage,
                                   @Nullable ActionMappingAnnotationHandler afterHandshake) { // @formatter:on
    this.onOpen = onOpen;
    this.onClose = onClose;
    this.onError = onError;
    this.onMessage = onMessage;
    this.pathPattern = pathPattern;
    this.afterHandshake = afterHandshake;
    this.containsPathVariable = containsPathVariable;
  }

  public void afterHandshake(RequestContext context) throws Throwable {
    if (afterHandshake != null) {
      afterHandshake.handleRequest(context);
    }
  }

}
