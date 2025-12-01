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

package infra.web.bind.resolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpRequest;
import infra.http.MediaType;
import infra.http.client.support.HttpRequestDecorator;
import infra.util.CollectionUtils;
import infra.web.RequestContext;
import infra.web.bind.MultipartException;
import infra.web.multipart.Part;
import infra.web.multipart.MultipartFile;
import infra.web.multipart.MultipartRequest;

/**
 * {@link HttpRequest} implementation that accesses one part of a multipart
 * request.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 15:59
 */
public class RequestPartServerHttpRequest extends HttpRequestDecorator implements HttpInputMessage {

  private final RequestContext request;

  private final String requestPartName;

  private final HttpHeaders multipartHeaders;

  private final MultipartRequest multipartRequest;

  /**
   * Create a new {@code RequestPartServerHttpRequest} instance.
   *
   * @param request the  request
   * @param requestPartName the name of the part to adapt to the {@link HttpRequest} contract
   * @throws MissingRequestPartException if the request part cannot be found
   * @throws MultipartException if RequestPartServerHttpRequest cannot be initialized
   */
  public RequestPartServerHttpRequest(RequestContext request, String requestPartName)
          throws MissingRequestPartException {
    super(request);
    this.request = request;
    this.requestPartName = requestPartName;
    this.multipartRequest = request.multipartRequest();
    HttpHeaders multipartHeaders = multipartRequest.getMultipartHeaders(requestPartName);
    if (multipartHeaders == null) {
      throw new MissingRequestPartException(requestPartName);
    }
    this.multipartHeaders = multipartHeaders;
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.multipartHeaders;
  }

  @Override
  public InputStream getBody() throws IOException {
    Part part = CollectionUtils.firstElement(multipartRequest.multipartData(requestPartName));
    if (part instanceof MultipartFile file) {
      return file.getInputStream();
    }
    else if (part != null) {
      return new ByteArrayInputStream(part.getContentAsByteArray());
    }

    String paramValue = request.getParameter(requestPartName);
    if (paramValue != null) {
      return new ByteArrayInputStream(paramValue.getBytes(determineCharset()));
    }

    throw new MissingRequestPartException(requestPartName);
  }

  private Charset determineCharset() {
    MediaType contentType = getHeaders().getContentType();
    if (contentType != null) {
      Charset charset = contentType.getCharset();
      if (charset != null) {
        return charset;
      }
    }

    return StandardCharsets.UTF_8;
  }

}
