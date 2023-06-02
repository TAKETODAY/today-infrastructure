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

package cn.taketoday.http.server.reactive;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBuffer.ByteBufferIterator;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.MultiValueMap;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * {@link ServletHttpHandlerAdapter} extension that uses Jetty APIs for writing
 * to the response with {@link ByteBuffer}.
 *
 * @author Violeta Georgieva
 * @author Brian Clozel
 * @since 4.0
 */
public class JettyHttpHandlerAdapter extends ServletHttpHandlerAdapter {

  public JettyHttpHandlerAdapter(HttpHandler httpHandler) {
    super(httpHandler);
  }

  @Override
  protected ServletServerHttpRequest createRequest(HttpServletRequest request, AsyncContext context)
          throws IOException, URISyntaxException {

    Assert.notNull(getServletPath(), "Servlet path is not initialized");
    return new JettyServerHttpRequest(
            request, context, getServletPath(), getDataBufferFactory(), getBufferSize());
  }

  @Override
  protected ServletServerHttpResponse createResponse(
          HttpServletResponse response, AsyncContext context, ServletServerHttpRequest request) throws IOException {

    return new JettyServerHttpResponse(
            response, context, getDataBufferFactory(), getBufferSize(), request);
  }

  private static final class JettyServerHttpRequest extends ServletServerHttpRequest {

    JettyServerHttpRequest(HttpServletRequest request, AsyncContext asyncContext,
            String servletPath, DataBufferFactory bufferFactory, int bufferSize)
            throws IOException, URISyntaxException {

      super(createHeaders(request), request, asyncContext, servletPath, bufferFactory, bufferSize);
    }

    private static MultiValueMap<String, String> createHeaders(HttpServletRequest servletRequest) {
      Request request = getRequest(servletRequest);
      HttpFields.Mutable fields = HttpFields.build(request.getHttpFields());
      return new JettyHeadersAdapter(fields);
    }

    private static Request getRequest(HttpServletRequest request) {
      if (request instanceof Request jettyRequest) {
        return jettyRequest;
      }
      else if (request instanceof HttpServletRequestWrapper wrapper) {
        HttpServletRequest wrappedRequest = (HttpServletRequest) wrapper.getRequest();
        return getRequest(wrappedRequest);
      }
      else {
        throw new IllegalArgumentException(
                "Cannot convert [" + request.getClass() + "] to org.eclipse.jetty.server.Request");
      }
    }
  }

  private static final class JettyServerHttpResponse extends ServletServerHttpResponse {

    JettyServerHttpResponse(
            HttpServletResponse response, AsyncContext asyncContext,
            DataBufferFactory bufferFactory, int bufferSize, ServletServerHttpRequest request) throws IOException {

      super(createHeaders(response), response, asyncContext, bufferFactory, bufferSize, request);
    }

    private static HttpHeaders createHeaders(HttpServletResponse servletResponse) {
      Response response = getResponse(servletResponse);
      HttpFields.Mutable fields = response.getHttpFields();
      return new DefaultHttpHeaders(new JettyHeadersAdapter(fields));
    }

    private static Response getResponse(HttpServletResponse response) {
      if (response instanceof Response jettyResponse) {
        return jettyResponse;
      }
      else if (response instanceof HttpServletResponseWrapper wrapper) {
        HttpServletResponse wrappedResponse = (HttpServletResponse) wrapper.getResponse();
        return getResponse(wrappedResponse);
      }
      else {
        throw new IllegalArgumentException(
                "Cannot convert [" + response.getClass() + "] to org.eclipse.jetty.server.Response");
      }
    }

    @Override
    protected int writeToOutputStream(DataBuffer dataBuffer) throws IOException {
      if (getOutputStream() instanceof HttpOutput httpOutput) {
        int len = 0;
        try (ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
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

    @Override
    protected void applyHeaders() {
      HttpServletResponse response = getNativeResponse();
      MediaType contentType = null;
      try {
        contentType = getHeaders().getContentType();
      }
      catch (Exception ex) {
        String rawContentType = getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        response.setContentType(rawContentType);
      }
      if (response.getContentType() == null && contentType != null) {
        response.setContentType(contentType.toString());
      }
      Charset charset = (contentType != null ? contentType.getCharset() : null);
      if (response.getCharacterEncoding() == null && charset != null) {
        response.setCharacterEncoding(charset.name());
      }
      long contentLength = getHeaders().getContentLength();
      if (contentLength != -1) {
        response.setContentLengthLong(contentLength);
      }
    }
  }

}
