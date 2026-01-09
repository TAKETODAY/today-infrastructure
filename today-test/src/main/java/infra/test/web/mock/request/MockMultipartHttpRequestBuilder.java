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

package infra.test.web.mock.request;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.mock.api.MockContext;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockMemoryFilePart;
import infra.mock.web.MockMultipartHttpMockRequest;
import infra.util.FileCopyUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.multipart.Part;

/**
 * Default builder for {@link MockMultipartHttpMockRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 4.0
 */
public class MockMultipartHttpRequestBuilder extends MockHttpRequestBuilder {

  private final List<MockMemoryFilePart> files = new ArrayList<>();

  private final MultiValueMap<String, Part> parts = new LinkedMultiValueMap<>();

  /**
   * Package-private constructor. Use static factory methods in
   * {@link MockMvcRequestBuilders}.
   * <p>For other ways to initialize a {@code MockMultipartHttpServletRequest},
   * see {@link #with(RequestPostProcessor)} and the
   * {@link RequestPostProcessor} extension point.
   *
   * @param urlTemplate a URL template; the resulting URL will be encoded
   * @param uriVariables zero or more URI variables
   */
  MockMultipartHttpRequestBuilder(String urlTemplate, Object... uriVariables) {
    this(HttpMethod.POST, urlTemplate, uriVariables);
  }

  /**
   * Variant of {@link #MockMultipartHttpRequestBuilder(String, Object...)}
   * that also accepts an {@link HttpMethod}.
   */
  MockMultipartHttpRequestBuilder(HttpMethod httpMethod, String urlTemplate, Object... uriVariables) {
    super(httpMethod, urlTemplate, uriVariables);
    super.contentType(MediaType.MULTIPART_FORM_DATA);
  }

  /**
   * Variant of {@link #MockMultipartHttpRequestBuilder(String, Object...)}
   * with a {@link URI}.
   */
  MockMultipartHttpRequestBuilder(URI uri) {
    this(HttpMethod.POST, uri);
  }

  /**
   * Variant of {@link #MockMultipartHttpRequestBuilder(String, Object...)}
   * with a {@link URI} and an {@link HttpMethod}.
   */
  MockMultipartHttpRequestBuilder(HttpMethod httpMethod, URI uri) {
    super(httpMethod, uri);
    super.contentType(MediaType.MULTIPART_FORM_DATA);
  }

  /**
   * Add a new {@link MockMemoryFilePart} with the given content.
   *
   * @param name the name of the file
   * @param content the content of the file
   */
  public MockMultipartHttpRequestBuilder file(String name, byte[] content) {
    this.files.add(new MockMemoryFilePart(name, content));
    return this;
  }

  /**
   * Add the given {@link MockMemoryFilePart}.
   *
   * @param file the multipart file
   */
  public MockMultipartHttpRequestBuilder file(MockMemoryFilePart file) {
    this.files.add(file);
    return this;
  }

  /**
   * Add {@link Part} components to the request.
   *
   * @param parts one or more parts to add
   */
  public MockMultipartHttpRequestBuilder part(Part... parts) {
    Assert.notEmpty(parts, "'parts' must not be empty");
    for (Part part : parts) {
      this.parts.add(part.getName(), part);
    }
    return this;
  }

  @Override
  public Object merge(@Nullable Object parent) {
    if (parent == null) {
      return this;
    }
    if (parent instanceof MockHttpRequestBuilder) {
      super.merge(parent);
      if (parent instanceof MockMultipartHttpRequestBuilder parentBuilder) {
        this.files.addAll(parentBuilder.files);
        parentBuilder.parts.keySet().forEach(name ->
                this.parts.putIfAbsent(name, parentBuilder.parts.get(name)));
      }
    }
    else {
      throw new IllegalArgumentException("Cannot merge with [" + parent.getClass().getName() + "]");
    }
    return this;
  }

  /**
   * Create a new {@link MockMultipartHttpMockRequest} based on the
   * supplied {@code MockContext} and the {@code MockMultipartFiles}
   * added to this builder.
   */
  @Override
  protected final HttpMockRequestImpl createMockRequest(MockContext mockContext) {
    MockMultipartHttpMockRequest mockRequest = new MockMultipartHttpMockRequest(mockContext);
    Charset defaultCharset = (mockRequest.getCharacterEncoding() != null ?
            Charset.forName(mockRequest.getCharacterEncoding()) : StandardCharsets.UTF_8);

    this.files.forEach(mockRequest::addPart);
    this.parts.values().stream().flatMap(Collection::stream).forEach(part -> {
      mockRequest.addPart(part);
      try {
        String name = part.getName();
        String filename = part.getOriginalFilename();
        InputStream is = part.getInputStream();
        if (filename != null) {
          mockRequest.addPart(new MockMemoryFilePart(name, filename, part.getContentTypeAsString(), is));
        }
        else {
          InputStreamReader reader = new InputStreamReader(is, getCharsetOrDefault(part, defaultCharset));
          String value = FileCopyUtils.copyToString(reader);
          mockRequest.addParameter(part.getName(), value);
        }
      }
      catch (IOException ex) {
        throw new IllegalStateException("Failed to read content for part " + part.getName(), ex);
      }
    });

    return mockRequest;
  }

  private Charset getCharsetOrDefault(Part part, Charset defaultCharset) {
    if (part.getContentType() != null) {
      MediaType mediaType = part.getContentType();
      if (mediaType.getCharset() != null) {
        return mediaType.getCharset();
      }
    }
    return defaultCharset;
  }

}
