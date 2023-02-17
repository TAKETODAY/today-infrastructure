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

import org.apache.catalina.connector.CoyoteInputStream;
import org.apache.catalina.connector.CoyoteOutputStream;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.connector.ResponseFacade;
import org.apache.coyote.Request;
import org.apache.coyote.Response;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ReflectionUtils;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * {@link ServletHttpHandlerAdapter} extension that uses Tomcat APIs for reading
 * from the request and writing to the response with {@link ByteBuffer}.
 *
 * @author Violeta Georgieva
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 4.0
 */
public class TomcatHttpHandlerAdapter extends ServletHttpHandlerAdapter {

  public TomcatHttpHandlerAdapter(HttpHandler httpHandler) {
    super(httpHandler);
  }

  @Override
  protected ServletServerHttpRequest createRequest(HttpServletRequest request, AsyncContext asyncContext)
          throws IOException, URISyntaxException {

    Assert.notNull(getServletPath(), "Servlet path is not initialized");
    return new TomcatServerHttpRequest(
            request, asyncContext, getServletPath(), getDataBufferFactory(), getBufferSize());
  }

  @Override
  protected ServletServerHttpResponse createResponse(
          HttpServletResponse response,
          AsyncContext asyncContext, ServletServerHttpRequest request) throws IOException {

    return new TomcatServerHttpResponse(
            response, asyncContext, getDataBufferFactory(), getBufferSize(), request);
  }

  private static final class TomcatServerHttpRequest extends ServletServerHttpRequest {
    private static final Field COYOTE_REQUEST_FIELD;

    private final int bufferSize;
    private final DataBufferFactory factory;

    static {
      Field field = ReflectionUtils.findField(RequestFacade.class, "request");
      Assert.state(field != null, "Incompatible Tomcat implementation");
      ReflectionUtils.makeAccessible(field);
      COYOTE_REQUEST_FIELD = field;
    }

    TomcatServerHttpRequest(HttpServletRequest request, AsyncContext context,
            String servletPath, DataBufferFactory factory, int bufferSize)
            throws IOException, URISyntaxException {

      super(createTomcatHttpHeaders(request), request, context, servletPath, factory, bufferSize);
      this.factory = factory;
      this.bufferSize = bufferSize;
    }

    private static MultiValueMap<String, String> createTomcatHttpHeaders(HttpServletRequest request) {
      RequestFacade requestFacade = getRequestFacade(request);
      var connectorRequest = (org.apache.catalina.connector.Request)
              ReflectionUtils.getField(COYOTE_REQUEST_FIELD, requestFacade);
      Assert.state(connectorRequest != null, "No Tomcat connector request");
      Request tomcatRequest = connectorRequest.getCoyoteRequest();
      return new TomcatHeadersAdapter(tomcatRequest.getMimeHeaders());
    }

    private static RequestFacade getRequestFacade(HttpServletRequest request) {
      if (request instanceof RequestFacade facade) {
        return facade;
      }
      else if (request instanceof HttpServletRequestWrapper wrapper) {
        HttpServletRequest wrappedRequest = (HttpServletRequest) wrapper.getRequest();
        return getRequestFacade(wrappedRequest);
      }
      else {
        throw new IllegalArgumentException(
                "Cannot convert [" + request.getClass() + "] to org.apache.catalina.connector.RequestFacade");
      }
    }

    @Override
    protected DataBuffer readFromInputStream() throws IOException {
      if (getInputStream() instanceof CoyoteInputStream coyoteInputStream) {
        DataBuffer dataBuffer = this.factory.allocateBuffer(this.bufferSize);
        int read = -1;
        try {
          try (DataBuffer.ByteBufferIterator iterator = dataBuffer.writableByteBuffers()) {
            Assert.state(iterator.hasNext(), "No ByteBuffer available");
            ByteBuffer byteBuffer = iterator.next();
            read = coyoteInputStream.read(byteBuffer);
          }
          logBytesRead(read);
          if (read > 0) {
            dataBuffer.writePosition(read);
            return dataBuffer;
          }
          else if (read == -1) {
            return EOF_BUFFER;
          }
          else {
            return AbstractListenerReadPublisher.EMPTY_BUFFER;
          }
        }
        finally {
          if (read <= 0) {
            DataBufferUtils.release(dataBuffer);
          }
        }
      }
      else {
        // It's possible InputStream can be wrapped, preventing use of CoyoteInputStream
        return super.readFromInputStream();
      }
    }
  }

  private static final class TomcatServerHttpResponse extends ServletServerHttpResponse {

    private static final Field COYOTE_RESPONSE_FIELD;

    static {
      Field field = ReflectionUtils.findField(ResponseFacade.class, "response");
      Assert.state(field != null, "Incompatible Tomcat implementation");
      ReflectionUtils.makeAccessible(field);
      COYOTE_RESPONSE_FIELD = field;
    }

    TomcatServerHttpResponse(
            HttpServletResponse response, AsyncContext context,
            DataBufferFactory factory, int bufferSize, ServletServerHttpRequest request) throws IOException {

      super(createTomcatHttpHeaders(response), response, context, factory, bufferSize, request);
    }

    private static HttpHeaders createTomcatHttpHeaders(HttpServletResponse response) {
      ResponseFacade responseFacade = getResponseFacade(response);
      var connectorResponse = (org.apache.catalina.connector.Response)
              ReflectionUtils.getField(COYOTE_RESPONSE_FIELD, responseFacade);
      Assert.state(connectorResponse != null, "No Tomcat connector response");
      Response tomcatResponse = connectorResponse.getCoyoteResponse();
      TomcatHeadersAdapter headers = new TomcatHeadersAdapter(tomcatResponse.getMimeHeaders());
      return new DefaultHttpHeaders(headers);
    }

    private static ResponseFacade getResponseFacade(HttpServletResponse response) {
      if (response instanceof ResponseFacade facade) {
        return facade;
      }
      else if (response instanceof HttpServletResponseWrapper wrapper) {
        HttpServletResponse wrappedResponse = (HttpServletResponse) wrapper.getResponse();
        return getResponseFacade(wrappedResponse);
      }
      else {
        throw new IllegalArgumentException(
                "Cannot convert [" + response.getClass() + "] to org.apache.catalina.connector.ResponseFacade");
      }
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
      getHeaders().remove(HttpHeaders.CONTENT_TYPE);
      Charset charset = (contentType != null ? contentType.getCharset() : null);
      if (response.getCharacterEncoding() == null && charset != null) {
        response.setCharacterEncoding(charset.name());
      }
      long contentLength = getHeaders().getContentLength();
      if (contentLength != -1) {
        response.setContentLengthLong(contentLength);
      }
      getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
    }

    @Override
    protected int writeToOutputStream(DataBuffer dataBuffer) throws IOException {
      if (getOutputStream() instanceof CoyoteOutputStream coyoteOutputStream) {
        int len = 0;
        try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
          while (iterator.hasNext() && coyoteOutputStream.isReady()) {
            ByteBuffer byteBuffer = iterator.next();
            len += byteBuffer.remaining();
            coyoteOutputStream.write(byteBuffer);
          }
        }
        return len;
      }
      else {
        return super.writeToOutputStream(dataBuffer);
      }
    }
  }

}
