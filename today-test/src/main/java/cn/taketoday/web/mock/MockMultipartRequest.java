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

package cn.taketoday.web.mock;

import java.io.IOException;
import java.util.Collection;

import cn.taketoday.http.ContentDisposition;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.bind.MultipartException;
import cn.taketoday.web.bind.NotMultipartRequestException;
import cn.taketoday.web.multipart.MaxUploadSizeExceededException;
import cn.taketoday.web.multipart.Multipart;
import cn.taketoday.web.multipart.support.AbstractMultipartRequest;
import cn.taketoday.mock.api.MockException;
import cn.taketoday.mock.api.http.HttpMockRequest;
import cn.taketoday.mock.api.http.Part;

/**
 * For Servlet Multipart
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 17:22
 */
public class MockMultipartRequest extends AbstractMultipartRequest {

  private final HttpMockRequest request;

  /**
   * Create a new ServletMultipartRequest wrapper for the given request,
   * immediately parsing the multipart content.
   *
   * @param request the servlet request to wrap
   * @throws MultipartException if parsing failed
   */
  public MockMultipartRequest(HttpMockRequest request) throws MultipartException {
    this(request, true);
  }

  /**
   * Create a new ServletMultipartRequest wrapper for the given request.
   *
   * @param request the servlet request to wrap
   * @param lazyParsing whether multipart parsing should be triggered lazily on
   * first access of multipart files or parameters
   * @throws MultipartException if an immediate parsing attempt failed
   */
  public MockMultipartRequest(HttpMockRequest request, boolean lazyParsing) throws MultipartException {
    this.request = request;
    if (!lazyParsing) {
      parseRequest(request);
    }
  }

  private MultiValueMap<String, Multipart> parseRequest(HttpMockRequest request) {
    try {
      Collection<Part> parts = request.getParts();
      LinkedMultiValueMap<String, Multipart> files = new LinkedMultiValueMap<>(parts.size());

      for (Part part : parts) {
        String headerValue = part.getHeader(HttpHeaders.CONTENT_DISPOSITION);
        ContentDisposition disposition = ContentDisposition.parse(headerValue);
        String filename = disposition.getFilename();
        files.add(part.getName(), new MockMultipartFile(part, filename));
      }
      return files;
    }
    catch (IOException e) {
      throw new MultipartException("MultipartFile parsing failed.", e);
    }
    catch (MockException e) {
      throw new NotMultipartRequestException("This is not a multipart request", e);
    }
    catch (Throwable ex) {
      String msg = ex.getMessage();
      if (msg != null && msg.contains("size") && msg.contains("exceed")) {
        throw new MaxUploadSizeExceededException(-1, ex);
      }
      throw new MultipartException("Failed to parse multipart servlet request", ex);
    }
  }

  @Override
  protected MultiValueMap<String, Multipart> parseRequest() {
    return parseRequest(request);
  }

  @Override
  public String getMultipartContentType(String paramOrFileName) {
    try {
      Part part = request.getPart(paramOrFileName);
      return part != null ? part.getContentType() : null;
    }
    catch (Throwable ex) {
      throw new MultipartException("Could not access multipart servlet request", ex);
    }
  }

  @Override
  public HttpHeaders getMultipartHeaders(String paramOrFileName) {
    try {
      Part part = request.getPart(paramOrFileName);
      if (part != null) {
        HttpHeaders headers = HttpHeaders.forWritable();
        for (String headerName : part.getHeaderNames()) {
          headers.addAll(headerName, part.getHeaders(headerName));
        }
        return headers;
      }
      else {
        return null;
      }
    }
    catch (Throwable ex) {
      throw new MultipartException("Could not access multipart servlet request", ex);
    }
  }

}
