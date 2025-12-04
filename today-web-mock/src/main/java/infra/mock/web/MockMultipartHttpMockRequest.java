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

package infra.mock.web;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.lang.Assert;
import infra.mock.api.MockContext;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.multipart.MultipartException;
import infra.web.multipart.MultipartRequest;
import infra.web.multipart.Part;
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
public class MockMultipartHttpMockRequest extends HttpMockRequestImpl {

  private final MapMultipartRequest multipartRequest;

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
    this.multipartRequest = new MapMultipartRequest();
    setMethod("POST");
    setContentType("multipart/form-data");
  }

  /**
   * Add a file to this request. The parameter name from the multipart
   * form is taken from the {@link Part#getName()}.
   *
   * @param file multipart file to be added
   */
  public void addPart(Part file) {
    Assert.notNull(file, "Part is required");
    multipartRequest.multipartData.add(file.getName(), file);
  }

  public MultipartRequest getMultipartRequest() {
    return multipartRequest;
  }

  class MapMultipartRequest implements MultipartRequest {
    private final MultiValueMap<String, Part> multipartData = new LinkedMultiValueMap<>();

    @Override
    public @Nullable Part getPart(String name) {
      return null;
    }

    @Override
    public Iterable<String> getPartNames() {
      return null;
    }

    @Override
    public List<Part> getParts(String name) {
      return multipartData.get(name);
    }

    @Override
    public MultiValueMap<String, Part> getParts() {
      return multipartData;
    }

    public HttpMethod getRequestMethod() {
      String method = getMethod();
      Assert.state(method != null, "Method is required");
      return HttpMethod.valueOf(method);
    }

    @Override
    public HttpHeaders getHeaders(String paramOrFileName) {
      var file = getPart(paramOrFileName);
      if (file != null) {
        HttpHeaders headers = HttpHeaders.forWritable();
        if (file.getContentType() != null) {
          headers.setContentType(file.getContentType());
        }
        return headers;
      }
      try {
        var part = MockMultipartHttpMockRequest.this.getPart(paramOrFileName);
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
}
