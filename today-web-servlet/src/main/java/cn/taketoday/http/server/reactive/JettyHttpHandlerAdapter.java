/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.http.server.reactive;

import org.eclipse.jetty.ee10.servlet.HttpOutput;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@link ServletHttpHandlerAdapter} extension that uses Jetty APIs for writing
 * to the response with {@link ByteBuffer}.
 *
 * @author Violeta Georgieva
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JettyHttpHandlerAdapter extends ServletHttpHandlerAdapter {

  public JettyHttpHandlerAdapter(HttpHandler httpHandler) {
    super(httpHandler);
  }

  @Override
  protected ServletServerHttpResponse createResponse(HttpServletResponse response,
          AsyncContext context, ServletServerHttpRequest request) throws IOException {

    return new Jetty12ServerHttpResponse(
            response, context, getDataBufferFactory(), getBufferSize(), request);
  }

  private static final class Jetty12ServerHttpResponse extends ServletServerHttpResponse {

    Jetty12ServerHttpResponse(HttpServletResponse response, AsyncContext asyncContext,
            DataBufferFactory bufferFactory, int bufferSize, ServletServerHttpRequest request)
            throws IOException {

      super(response, asyncContext, bufferFactory, bufferSize, request);
    }

    @Override
    protected int writeToOutputStream(DataBuffer dataBuffer) throws IOException {
      OutputStream output = getOutputStream();
      if (output instanceof HttpOutput httpOutput) {
        int len = 0;
        try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
          while (iterator.hasNext() && httpOutput.isReady()) {
            ByteBuffer byteBuffer = iterator.next();
            len += byteBuffer.remaining();
            httpOutput.write(byteBuffer);
          }
        }
        return len;
      }
      return super.writeToOutputStream(dataBuffer);
    }
  }

}
