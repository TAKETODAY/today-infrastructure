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

package cn.taketoday.web.handler;

import java.io.IOException;
import java.io.OutputStream;

import cn.taketoday.web.handler.method.RequestMappingHandlerAdapter;

/**
 * A http request handler return value type for asynchronous request processing
 * where the application can write directly to the response {@code OutputStream}
 * without holding up the container thread.
 *
 * <p><strong>Note:</strong> when using this option it is highly recommended to
 * configure explicitly the TaskExecutor used in Web MVC for executing
 * asynchronous requests. Both the MVC Java config and the MVC namespaces provide
 * options to configure asynchronous handling. If not using those, an application
 * can set the {@code taskExecutor} property of
 * {@link RequestMappingHandlerAdapter RequestMappingHandlerAdapter}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/9 12:07
 */
@FunctionalInterface
public interface StreamingResponseBody {

  /**
   * A callback for writing to the response body.
   *
   * @param outputStream the stream for the response body
   * @throws IOException an exception while writing
   */
  void writeTo(OutputStream outputStream) throws IOException;

}
