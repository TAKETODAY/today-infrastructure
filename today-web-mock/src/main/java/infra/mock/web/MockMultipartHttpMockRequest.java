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

package infra.mock.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.mock.api.MockContext;
import infra.mock.api.MockException;
import infra.mock.api.http.Part;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.bind.MultipartException;
import infra.web.multipart.Multipart;
import infra.web.multipart.MultipartFile;
import infra.web.multipart.MultipartRequest;
import infra.web.util.WebUtils;

/**
 * Mock implementation of the
 * {@link infra.web.multipart.MultipartRequest} interface.
 *
 * <p>Useful for testing application controllers that access multipart uploads.
 * {@link MockMultipartFile} can be used to populate these mock requests with files.
 *
 * @author Juergen Hoeller
 * @author Eric Crampton
 * @author Arjen Poutsma
 * @see MockMultipartFile
 * @since 4.0
 */
public class MockMultipartHttpMockRequest extends HttpMockRequestImpl implements MultipartRequest {

  private final MultiValueMap<String, Multipart> multipartData = new LinkedMultiValueMap<>();

  /**
   * Create a new {@code MockMultipartHttpServletRequest} with a default
   * {@link MockContextImpl}.
   *
   * @see #MockMultipartHttpMockRequest(MockContext)
   */
  public MockMultipartHttpMockRequest() {
    this(null);
  }

  /**
   * Create a new {@code MockMultipartHttpServletRequest} with the supplied {@link MockContext}.
   *
   * @param mockContext the MockContext that the request runs in
   * (may be {@code null} to use a default {@link MockContextImpl})
   */
  public MockMultipartHttpMockRequest(@Nullable MockContext mockContext) {
    super(mockContext);
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
    Assert.notNull(file, "MultipartFile is required");
    this.multipartData.add(file.getName(), file);
  }

  @Override
  public Iterator<String> getFileNames() {
    return this.multipartData.keySet().iterator();
  }

  @Override
  public MultipartFile getFile(String name) {
    return getMultipartFiles().getFirst(name);
  }

  @Override
  public List<MultipartFile> getFiles(String name) {
    List<MultipartFile> multipartFiles = getMultipartFiles().get(name);
    return Objects.requireNonNullElse(multipartFiles, Collections.emptyList());
  }

  @Override
  public List<Multipart> multipartData(String name) {
    return multipartData().get(name);
  }

  @Override
  public Map<String, MultipartFile> getFileMap() {
    return getMultipartFiles().toSingleValueMap();
  }

  /**
   * Obtain the MultipartFile Map for retrieval,
   * lazily initializing it if necessary.
   */
  @Override
  public MultiValueMap<String, MultipartFile> getMultipartFiles() {
    MultiValueMap<String, MultipartFile> ret = new LinkedMultiValueMap<>();
    for (Map.Entry<String, List<Multipart>> entry : multipartData().entrySet()) {
      for (Multipart multipart : entry.getValue()) {
        if (multipart instanceof MultipartFile file) {
          ret.add(entry.getKey(), file);
        }
      }
    }
    return ret;
  }

  @Override
  public MultiValueMap<String, Multipart> multipartData() {
    return multipartData;
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
    catch (MockException | IOException ex) {
      // Should never happen (we're not actually parsing)
      throw new IllegalStateException(ex);
    }
    return null;
  }

  public HttpMethod getRequestMethod() {
    String method = getMethod();
    Assert.state(method != null, "Method is required");
    return HttpMethod.valueOf(method);
  }

  public HttpHeaders getRequestHeaders() {
    HttpHeaders headers = HttpHeaders.forWritable();
    Enumeration<String> headerNames = getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      headers.put(headerName, Collections.list(getHeaders(headerName)));
    }
    return headers;
  }

  @Override
  public HttpHeaders getMultipartHeaders(String paramOrFileName) {
    MultipartFile file = getFile(paramOrFileName);
    if (file != null) {
      HttpHeaders headers = HttpHeaders.forWritable();
      if (file.getContentType() != null) {
        headers.add(HttpHeaders.CONTENT_TYPE, file.getContentType());
      }
      return headers;
    }
    try {
      Part part = getPart(paramOrFileName);
      if (part != null) {
        HttpHeaders headers = HttpHeaders.forWritable();
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

  @Override
  public void cleanup() {
    WebUtils.cleanupMultipartRequest(multipartData);
  }

}
