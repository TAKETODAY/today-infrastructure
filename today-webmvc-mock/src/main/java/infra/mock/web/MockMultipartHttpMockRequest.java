/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.mock.web;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import infra.http.HttpHeaders;
import infra.lang.Assert;
import infra.mock.api.MockContext;
import infra.util.CollectionUtils;
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
 * {@link MockMemoryFilePart} can be used to populate these mock requests with files.
 *
 * @author Juergen Hoeller
 * @author Eric Crampton
 * @author Arjen Poutsma
 * @see MockMemoryFilePart
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
      Part first = multipartData.getFirst(name);
      if (first == null) {
        first = MockMultipartHttpMockRequest.super.getPart(name);
      }
      return first;
    }

    @Override
    public Iterable<String> getPartNames() {
      Set<String> strings = new LinkedHashSet<>();
      CollectionUtils.addAll(strings, MockMultipartHttpMockRequest.super.parts.keySet());
      CollectionUtils.addAll(strings, multipartData.keySet());
      return strings;
    }

    @Override
    public List<Part> getParts(String name) {
      List<Part> list = multipartData.get(name);
      if (list == null) {
        list = MockMultipartHttpMockRequest.super.parts.get(name);
      }
      return list;
    }

    @Override
    public MultiValueMap<String, Part> getParts() {
      LinkedMultiValueMap<String, Part> copied = MultiValueMap.copyOf(multipartData);
      copied.addAll(MockMultipartHttpMockRequest.super.parts);
      return copied;
    }

    @Override
    public HttpHeaders getHeaders(String name) {
      var file = getPart(name);
      if (file != null) {
        HttpHeaders headers = HttpHeaders.forWritable();
        if (file.getContentType() != null) {
          headers.setContentType(file.getContentType());
        }
        return headers;
      }
      try {
        var part = MockMultipartHttpMockRequest.this.getPart(name);
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
      WebUtils.cleanupMultipartRequest(MockMultipartHttpMockRequest.super.parts);
    }
  }
}
