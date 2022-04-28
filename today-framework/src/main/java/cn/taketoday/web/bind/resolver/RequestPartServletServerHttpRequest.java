/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.bind.resolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.client.support.HttpRequestDecorator;
import cn.taketoday.http.server.ServerHttpRequest;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.MultipartException;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.MultipartHttpServletRequest;
import cn.taketoday.web.multipart.support.DefaultMultipartHttpServletRequest;
import jakarta.servlet.http.Part;

/**
 * {@link HttpRequest} implementation that accesses one part of a multipart
 * request. If using {@link MultipartResolver} configuration the part is accessed
 * through a {@link MultipartFile}. Or if using Servlet multipart processing
 * the part is accessed through {@code ServletRequest.getPart}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 15:59
 */
public class RequestPartServletServerHttpRequest extends HttpRequestDecorator implements HttpInputMessage {

  private final MultipartHttpServletRequest multipartRequest;

  private final String requestPartName;

  private final HttpHeaders multipartHeaders;

  /**
   * Create a new {@code RequestPartServletServerHttpRequest} instance.
   *
   * @param request the current servlet request
   * @param requestPartName the name of the part to adapt to the {@link ServerHttpRequest} contract
   * @throws MissingRequestPartException if the request part cannot be found
   * @throws MultipartException if MultipartHttpServletRequest cannot be initialized
   */
  public RequestPartServletServerHttpRequest(RequestContext request, String requestPartName)
          throws MissingRequestPartException {
    super(request);

    this.requestPartName = requestPartName;
    this.multipartRequest = MultipartResolutionDelegate.asMultipartHttpServletRequest(request);
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
    // Prefer Servlet Part resolution to cover file as well as parameter streams
    boolean servletParts = multipartRequest instanceof DefaultMultipartHttpServletRequest;
    if (servletParts) {
      Part part = retrieveServletPart();
      if (part != null) {
        return part.getInputStream();
      }
    }

    // Framework-style distinction between MultipartFile and String parameters
    MultipartFile file = multipartRequest.getFile(requestPartName);
    if (file != null) {
      return file.getInputStream();
    }
    String paramValue = multipartRequest.getParameter(requestPartName);
    if (paramValue != null) {
      return new ByteArrayInputStream(paramValue.getBytes(determineCharset()));
    }

    // Fallback: Servlet Part resolution even if not indicated
    if (!servletParts) {
      Part part = retrieveServletPart();
      if (part != null) {
        return part.getInputStream();
      }
    }

    throw new IllegalStateException("No body available for request part '" + requestPartName + "'");
  }

  @Nullable
  private Part retrieveServletPart() {
    try {
      return multipartRequest.getPart(requestPartName);
    }
    catch (Exception ex) {
      throw new MultipartException("Failed to retrieve request part '" + requestPartName + "'", ex);
    }
  }

  private Charset determineCharset() {
    MediaType contentType = getHeaders().getContentType();
    if (contentType != null) {
      Charset charset = contentType.getCharset();
      if (charset != null) {
        return charset;
      }
    }
    String encoding = multipartRequest.getCharacterEncoding();
    return encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
  }

}
