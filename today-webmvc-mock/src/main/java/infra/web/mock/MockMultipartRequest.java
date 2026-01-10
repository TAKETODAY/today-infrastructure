/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.mock;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;

import infra.http.HttpHeaders;
import infra.mock.api.MockException;
import infra.mock.api.http.HttpMockRequest;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.multipart.MultipartException;
import infra.web.multipart.NotMultipartRequestException;
import infra.web.multipart.Part;
import infra.web.multipart.AbstractMultipartRequest;

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

  private MultiValueMap<String, Part> parseRequest(HttpMockRequest request) {
    try {
      Collection<Part> parts = request.getParts();
      LinkedMultiValueMap<String, Part> files = new LinkedMultiValueMap<>(parts.size());

      for (var part : parts) {
        files.add(part.getName(), part);
      }
      return files;
    }
    catch (IOException e) {
      throw new MultipartException("Part parsing failed.", e);
    }
    catch (MockException e) {
      throw new NotMultipartRequestException("This is not a multipart request", e);
    }
    catch (Throwable ex) {
      throw new MultipartException("Failed to parse multipart request", ex);
    }
  }

  @Override
  protected MultiValueMap<String, Part> parseRequest() {
    return parseRequest(request);
  }

  @Override
  public @Nullable HttpHeaders getHeaders(String name) {
    try {
      var part = request.getPart(name);
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
