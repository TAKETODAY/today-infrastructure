/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.handler.method;

import java.lang.reflect.Method;
import java.util.Set;

import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;

/**
 * {@code @ExceptionHandler} mapping information. It contains:
 * <ul>
 *     <li>the supported exception types
 *     <li>the producible media types, if any
 *     <li>the method in charge of handling the exception
 * </ul>
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class ExceptionHandlerMappingInfo {

  private final Set<Class<? extends Throwable>> exceptionTypes;

  private final Set<MediaType> producibleTypes;

  private final Method handlerMethod;

  ExceptionHandlerMappingInfo(Set<Class<? extends Throwable>> exceptionTypes, Set<MediaType> producibleMediaTypes, Method handlerMethod) {
    Assert.notNull(exceptionTypes, "exceptionTypes is required");
    Assert.notNull(producibleMediaTypes, "producibleMediaTypes is required");
    Assert.notNull(handlerMethod, "handlerMethod is required");
    this.exceptionTypes = exceptionTypes;
    this.producibleTypes = producibleMediaTypes;
    this.handlerMethod = handlerMethod;
  }

  /**
   * Return the method responsible for handling the exception.
   */
  public Method getHandlerMethod() {
    return this.handlerMethod;
  }

  /**
   * Return the exception types supported by this handler.
   */
  public Set<Class<? extends Throwable>> getExceptionTypes() {
    return this.exceptionTypes;
  }

  /**
   * Return the producible media types by this handler. Can be empty.
   */
  public Set<MediaType> getProducibleTypes() {
    return this.producibleTypes;
  }

}
