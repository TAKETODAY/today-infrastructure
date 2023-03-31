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

package cn.taketoday.test.web.servlet.request;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockMultipartFile;
import cn.taketoday.mock.web.MockMultipartHttpServletRequest;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;

/**
 * Default builder for {@link MockMultipartHttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 4.0
 */
public class MockMultipartHttpServletRequestBuilder extends MockHttpServletRequestBuilder {

  private final List<MockMultipartFile> files = new ArrayList<>();

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
  MockMultipartHttpServletRequestBuilder(String urlTemplate, Object... uriVariables) {
    this(HttpMethod.POST, urlTemplate, uriVariables);
  }

  /**
   * Variant of {@link #MockMultipartHttpServletRequestBuilder(String, Object...)}
   * that also accepts an {@link HttpMethod}.
   */
  MockMultipartHttpServletRequestBuilder(HttpMethod httpMethod, String urlTemplate, Object... uriVariables) {
    super(httpMethod, urlTemplate, uriVariables);
    super.contentType(MediaType.MULTIPART_FORM_DATA);
  }

  /**
   * Variant of {@link #MockMultipartHttpServletRequestBuilder(String, Object...)}
   * with a {@link URI}.
   */
  MockMultipartHttpServletRequestBuilder(URI uri) {
    this(HttpMethod.POST, uri);
  }

  /**
   * Variant of {@link #MockMultipartHttpServletRequestBuilder(String, Object...)}
   * with a {@link URI} and an {@link HttpMethod}.
   */
  MockMultipartHttpServletRequestBuilder(HttpMethod httpMethod, URI uri) {
    super(httpMethod, uri);
    super.contentType(MediaType.MULTIPART_FORM_DATA);
  }

  /**
   * Add a new {@link MockMultipartFile} with the given content.
   *
   * @param name the name of the file
   * @param content the content of the file
   */
  public MockMultipartHttpServletRequestBuilder file(String name, byte[] content) {
    this.files.add(new MockMultipartFile(name, content));
    return this;
  }

  /**
   * Add the given {@link MockMultipartFile}.
   *
   * @param file the multipart file
   */
  public MockMultipartHttpServletRequestBuilder file(MockMultipartFile file) {
    this.files.add(file);
    return this;
  }

  /**
   * Add {@link Part} components to the request.
   *
   * @param parts one or more parts to add
   */
  public MockMultipartHttpServletRequestBuilder part(Part... parts) {
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
    if (parent instanceof MockHttpServletRequestBuilder) {
      super.merge(parent);
      if (parent instanceof MockMultipartHttpServletRequestBuilder parentBuilder) {
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
   * Create a new {@link MockMultipartHttpServletRequest} based on the
   * supplied {@code ServletContext} and the {@code MockMultipartFiles}
   * added to this builder.
   */
  @Override
  protected final MockHttpServletRequest createServletRequest(ServletContext servletContext) {
    MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest(servletContext);
    Charset defaultCharset = (request.getCharacterEncoding() != null ?
                              Charset.forName(request.getCharacterEncoding()) : StandardCharsets.UTF_8);

    this.files.forEach(request::addFile);
    this.parts.values().stream().flatMap(Collection::stream).forEach(part -> {
      request.addPart(part);
      try {
        String name = part.getName();
        String filename = part.getSubmittedFileName();
        InputStream is = part.getInputStream();
        if (filename != null) {
          request.addFile(new MockMultipartFile(name, filename, part.getContentType(), is));
        }
        else {
          InputStreamReader reader = new InputStreamReader(is, getCharsetOrDefault(part, defaultCharset));
          String value = FileCopyUtils.copyToString(reader);
          request.addParameter(part.getName(), value);
        }
      }
      catch (IOException ex) {
        throw new IllegalStateException("Failed to read content for part " + part.getName(), ex);
      }
    });

    return request;
  }

  private Charset getCharsetOrDefault(Part part, Charset defaultCharset) {
    if (part.getContentType() != null) {
      MediaType mediaType = MediaType.parseMediaType(part.getContentType());
      if (mediaType.getCharset() != null) {
        return mediaType.getCharset();
      }
    }
    return defaultCharset;
  }

}
