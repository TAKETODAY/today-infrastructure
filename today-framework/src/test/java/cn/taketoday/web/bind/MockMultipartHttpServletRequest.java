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

package cn.taketoday.web.bind;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.mock.MockHttpServletRequest;
import cn.taketoday.web.mock.MockServletContext;
import cn.taketoday.web.multipart.MultipartFile;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

/**
 * Mock implementation of the {@link  HttpServletRequest} interface.
 *
 * <p>this set of mocks is designed on a Servlet 4.0 baseline.
 *
 * <p>Useful for testing application controllers that access multipart uploads.
 * {@link MockMultipartFile} can be used to populate these mock requests with files.
 *
 * @author Juergen Hoeller
 * @author Eric Crampton
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MockMultipartFile
 * @since 4.0 2022/3/2 17:03
 */
public class MockMultipartHttpServletRequest extends MockHttpServletRequest {

  private final MultiValueMap<String, MultipartFile> multipartFiles = MultiValueMap.fromLinkedHashMap();

  /**
   * Create a new {@code MockMultipartHttpServletRequest} with a default
   * {@link MockServletContext}.
   *
   * @see #MockMultipartHttpServletRequest(ServletContext)
   */
  public MockMultipartHttpServletRequest() {
    this(null);
  }

  /**
   * Create a new {@code MockMultipartHttpServletRequest} with the supplied {@link ServletContext}.
   *
   * @param servletContext the ServletContext that the request runs in
   * (may be {@code null} to use a default {@link MockServletContext})
   */
  public MockMultipartHttpServletRequest(@Nullable ServletContext servletContext) {
    super(servletContext);
    setMethod("POST");
    setContentType("multipart/form-data");
  }

  /**
   * Add a file to this request. The parameter name from the multipart
   * form is taken from the {@link MultipartFile#getName()}.
   *
   * @param file multipart file to be added
   */
  public void addFile(MultipartFile file) {
    Assert.notNull(file, "MultipartFile must not be null");
    this.multipartFiles.add(file.getName(), file);
  }

  public Iterator<String> getFileNames() {
    return this.multipartFiles.keySet().iterator();
  }

  public MultipartFile getFile(String name) {
    return this.multipartFiles.getFirst(name);
  }

  public List<MultipartFile> getFiles(String name) {
    List<MultipartFile> multipartFiles = this.multipartFiles.get(name);
    if (multipartFiles != null) {
      return multipartFiles;
    }
    else {
      return Collections.emptyList();
    }
  }

  public Map<String, MultipartFile> getFileMap() {
    return this.multipartFiles.toSingleValueMap();
  }

  public MultiValueMap<String, MultipartFile> getMultiFileMap() {
    return MultiValueMap.from(this.multipartFiles);
  }

  public String getMultipartContentType(String paramOrFileName) {
    MultipartFile file = getFile(paramOrFileName);
    if (file != null) {
      return file.getContentType();
    }
    try {
      Part part = getPart(paramOrFileName);
      if (part != null) {
        return part.getContentType();
      }
    }
    catch (ServletException | IOException ex) {
      // Should never happen (we're not actually parsing)
      throw new IllegalStateException(ex);
    }
    return null;
  }

  public HttpMethod getRequestMethod() {
    String method = getMethod();
    if (method != null) {
      return HttpMethod.valueOf(method);
    }
    else {
      return null;
    }
  }

  public HttpHeaders getRequestHeaders() {
    HttpHeaders headers = HttpHeaders.create();
    Enumeration<String> headerNames = getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      headers.put(headerName, Collections.list(getHeaders(headerName)));
    }
    return headers;
  }

  public HttpHeaders getMultipartHeaders(String paramOrFileName) {
    MultipartFile file = getFile(paramOrFileName);
    if (file != null) {
      HttpHeaders headers = HttpHeaders.create();
      if (file.getContentType() != null) {
        headers.add(HttpHeaders.CONTENT_TYPE, file.getContentType());
      }
      return headers;
    }
    try {
      Part part = getPart(paramOrFileName);
      if (part != null) {
        HttpHeaders headers = HttpHeaders.create();
        for (String headerName : part.getHeaderNames()) {
          headers.put(headerName, new ArrayList<>(part.getHeaders(headerName)));
        }
        return headers;
      }
    }
    catch (Throwable ex) {
      throw new MultipartException("Could not access multipart servlet request", ex);
    }
    return null;
  }

}

