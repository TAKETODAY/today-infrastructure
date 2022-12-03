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

package cn.taketoday.web.service.invoker;

import org.reactivestreams.Publisher;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import cn.taketoday.core.LinkedMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeReference;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.client.MultipartBodyBuilder;
import cn.taketoday.http.codec.FormHttpMessageWriter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.util.UriComponentsBuilder;
import cn.taketoday.web.util.UriUtils;

/**
 * Container for HTTP request values extracted from an
 * {@link cn.taketoday.web.service.annotation.HttpExchange @HttpExchange}-annotated
 * method and argument values passed to it. This is then given to
 * {@link HttpClientAdapter} to adapt to the underlying HTTP client.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class HttpRequestValues {

  private static final MultiValueMap<String, String> EMPTY_COOKIES_MAP =
          MultiValueMap.from(Collections.emptyMap());

  @Nullable
  private final HttpMethod httpMethod;

  @Nullable
  private final URI uri;

  @Nullable
  private final String uriTemplate;

  private final Map<String, String> uriVariables;

  private final HttpHeaders headers;

  private final MultiValueMap<String, String> cookies;

  private final Map<String, Object> attributes;

  @Nullable
  private final Object bodyValue;

  @Nullable
  private final Publisher<?> body;

  @Nullable
  private final TypeReference<?> bodyElementType;

  private HttpRequestValues(@Nullable HttpMethod httpMethod,
          @Nullable URI uri, @Nullable String uriTemplate, Map<String, String> uriVariables,
          HttpHeaders headers, MultiValueMap<String, String> cookies, Map<String, Object> attributes,
          @Nullable Object bodyValue,
          @Nullable Publisher<?> body, @Nullable TypeReference<?> bodyElementType) {

    Assert.isTrue(uri != null || uriTemplate != null, "Neither URI nor URI template");

    this.httpMethod = httpMethod;
    this.uri = uri;
    this.uriTemplate = uriTemplate;
    this.uriVariables = uriVariables;
    this.headers = headers;
    this.cookies = cookies;
    this.attributes = attributes;
    this.bodyValue = bodyValue;
    this.body = body;
    this.bodyElementType = bodyElementType;
  }

  /**
   * Return the HTTP method to use for the request.
   */
  @Nullable
  public HttpMethod getHttpMethod() {
    return this.httpMethod;
  }

  /**
   * Return the URL to use.
   * <p>Typically, this comes from a {@link URI} method argument, which provides
   * the caller with the option to override the {@link #getUriTemplate()
   * uriTemplate} from class and method {@code HttpExchange} annotations.
   * annotation.
   */
  @Nullable
  public URI getUri() {
    return this.uri;
  }

  /**
   * Return the URL template for the request. This comes from the values in
   * class and method {@code HttpExchange} annotations.
   */
  @Nullable
  public String getUriTemplate() {
    return this.uriTemplate;
  }

  /**
   * Return the URL template variables, or an empty map.
   */
  public Map<String, String> getUriVariables() {
    return this.uriVariables;
  }

  /**
   * Return the headers for the request, if any.
   */
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  /**
   * Return the cookies for the request, or an empty map.
   */
  public MultiValueMap<String, String> getCookies() {
    return this.cookies;
  }

  /**
   * Return the attributes associated with the request, or an empty map.
   */
  public Map<String, Object> getAttributes() {
    return this.attributes;
  }

  /**
   * Return the request body as a value to be serialized, if set.
   * <p>This is mutually exclusive with {@link #getBody()}.
   * Only one of the two or neither is set.
   */
  @Nullable
  public Object getBodyValue() {
    return this.bodyValue;
  }

  /**
   * Return the request body as a Publisher.
   * <p>This is mutually exclusive with {@link #getBodyValue()}.
   * Only one of the two or neither is set.
   */
  @Nullable
  public Publisher<?> getBody() {
    return this.body;
  }

  /**
   * Return the element type for a {@linkplain #getBody() Publisher body}.
   */
  @Nullable
  public TypeReference<?> getBodyElementType() {
    return this.bodyElementType;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link HttpRequestValues}.
   */
  public final static class Builder {

    private static final Function<MultiValueMap<String, String>, byte[]> FORM_DATA_SERIALIZER = new FormDataSerializer();

    @Nullable
    private HttpMethod httpMethod;

    @Nullable
    private URI uri;

    @Nullable
    private String uriTemplate;

    @Nullable
    private Map<String, String> uriVars;

    @Nullable
    private HttpHeaders headers;

    @Nullable
    private MultiValueMap<String, String> cookies;

    @Nullable
    private MultiValueMap<String, String> requestParams;

    @Nullable
    private MultipartBodyBuilder multipartBuilder;

    @Nullable
    private Map<String, Object> attributes;

    @Nullable
    private Object bodyValue;

    @Nullable
    private Publisher<?> body;

    @Nullable
    private TypeReference<?> bodyElementType;

    /**
     * Set the HTTP method for the request.
     */
    public Builder setHttpMethod(HttpMethod httpMethod) {
      this.httpMethod = httpMethod;
      return this;
    }

    /**
     * Set the URL to use. When set, this overrides the
     * {@linkplain #setUriTemplate(String) URI template} from the
     * {@code HttpExchange} annotation.
     */
    public Builder setUri(URI uri) {
      this.uri = uri;
      this.uriTemplate = null;
      this.uriVars = null;
      return this;
    }

    /**
     * Set the request URL as a String template.
     */
    public Builder setUriTemplate(String uriTemplate) {
      this.uriTemplate = uriTemplate;
      return this;
    }

    /**
     * Add a URI variable name-value pair.
     * <p>This is mutually exclusive with, and resets any previously set
     * {@linkplain #setUri(URI) full URI}.
     */
    public Builder setUriVariable(String name, String value) {
      this.uriVars = (this.uriVars != null ? this.uriVars : new LinkedHashMap<>());
      this.uriVars.put(name, value);
      return this;
    }

    /**
     * Set the media types for the request {@code Accept} header.
     */
    public Builder setAccept(List<MediaType> acceptableMediaTypes) {
      initHeaders().setAccept(acceptableMediaTypes);
      return this;
    }

    /**
     * Set the media type for the request {@code Content-Type} header.
     */
    public Builder setContentType(MediaType contentType) {
      initHeaders().setContentType(contentType);
      return this;
    }

    /**
     * Add the given header name and values.
     */
    public Builder addHeader(String headerName, String... headerValues) {
      for (String headerValue : headerValues) {
        initHeaders().add(headerName, headerValue);
      }
      return this;
    }

    private HttpHeaders initHeaders() {
      this.headers = (this.headers != null ? this.headers : HttpHeaders.create());
      return this.headers;
    }

    /**
     * Add the given cookie name and values.
     */
    public Builder addCookie(String name, String... values) {
      this.cookies = (this.cookies != null ? this.cookies : new LinkedMultiValueMap<>());
      for (String value : values) {
        this.cookies.add(name, value);
      }
      return this;
    }

    /**
     * Add the given request parameter name and values.
     * <p>When {@code "content-type"} is set to
     * {@code "application/x-www-form-urlencoded"}, request parameters are
     * encoded in the request body. Otherwise, they are added as URL query
     * parameters.
     */
    public Builder addRequestParameter(String name, String... values) {
      this.requestParams = (this.requestParams != null ? this.requestParams : new LinkedMultiValueMap<>());
      for (String value : values) {
        this.requestParams.add(name, value);
      }
      return this;
    }

    /**
     * Add a part to a multipart request. The part value may be as described
     * in {@link MultipartBodyBuilder#part(String, Object)}.
     */
    public Builder addRequestPart(String name, Object part) {
      this.multipartBuilder = (this.multipartBuilder != null ? this.multipartBuilder : new MultipartBodyBuilder());
      this.multipartBuilder.part(name, part);
      return this;
    }

    /**
     * Variant of {@link #addRequestPart(String, Object)} that allows the
     * part value to be produced by a {@link Publisher}.
     */
    public <T, P extends Publisher<T>> Builder addRequestPart(String name, P publisher, ResolvableType type) {
      this.multipartBuilder = (this.multipartBuilder != null ? this.multipartBuilder : new MultipartBodyBuilder());
      this.multipartBuilder.asyncPart(name, publisher, TypeReference.fromType(type.getType()));
      return this;
    }

    /**
     * Configure an attribute to associate with the request.
     *
     * @param name the attribute name
     * @param value the attribute value
     */
    public Builder addAttribute(String name, Object value) {
      this.attributes = (this.attributes != null ? this.attributes : new HashMap<>());
      this.attributes.put(name, value);
      return this;
    }

    /**
     * Set the request body as a concrete value to be serialized.
     * <p>This is mutually exclusive with, and resets any previously set
     * {@linkplain #setBody(Publisher, TypeReference) body Publisher}.
     */
    public void setBodyValue(Object bodyValue) {
      this.bodyValue = bodyValue;
      this.body = null;
      this.bodyElementType = null;
    }

    /**
     * Set the request body as a concrete value to be serialized.
     * <p>This is mutually exclusive with, and resets any previously set
     * {@linkplain #setBodyValue(Object) body value}.
     */
    public <T, P extends Publisher<T>> void setBody(P body, TypeReference<T> elementTye) {
      this.body = body;
      this.bodyElementType = elementTye;
      this.bodyValue = null;
    }

    /**
     * Builder the {@link HttpRequestValues} instance.
     */
    public HttpRequestValues build() {

      URI uri = this.uri;
      String uriTemplate = this.uriTemplate;
      if (uriTemplate == null) {
        uriTemplate = "";
      }

      Map<String, String> uriVars = (this.uriVars != null ? new HashMap<>(this.uriVars) : Collections.emptyMap());

      Object bodyValue = this.bodyValue;

      if (!CollectionUtils.isEmpty(this.requestParams)) {

        boolean isFormData = (this.headers != null &&
                MediaType.APPLICATION_FORM_URLENCODED.equals(this.headers.getContentType()));

        if (isFormData) {
          Assert.isTrue(bodyValue == null && this.body == null, "Expected body or request params, not both");
          bodyValue = FORM_DATA_SERIALIZER.apply(this.requestParams);
        }
        else if (uri != null) {
          uri = UriComponentsBuilder.fromUri(uri)
                  .queryParams(UriUtils.encodeQueryParams(this.requestParams))
                  .build(true)
                  .toUri();
        }
        else {
          uriVars = (uriVars.isEmpty() ? new HashMap<>() : uriVars);
          uriTemplate = appendQueryParams(uriTemplate, uriVars, this.requestParams);
        }
      }
      else if (this.multipartBuilder != null) {
        Assert.isTrue(bodyValue == null && this.body == null, "Expected body or request parts, not both");
        bodyValue = this.multipartBuilder.build();
      }

      HttpHeaders headers = HttpHeaders.empty();
      if (this.headers != null) {
        headers = HttpHeaders.create();
        headers.putAll(this.headers);
      }

      MultiValueMap<String, String> cookies = (this.cookies != null ?
                                               new LinkedMultiValueMap<>(this.cookies) : EMPTY_COOKIES_MAP);

      Map<String, Object> attributes = (this.attributes != null ?
                                        new HashMap<>(this.attributes) : Collections.emptyMap());

      return new HttpRequestValues(
              this.httpMethod, uri, uriTemplate, uriVars, headers, cookies, attributes,
              bodyValue, this.body, this.bodyElementType);
    }

    private String appendQueryParams(
            String uriTemplate, Map<String, String> uriVars, MultiValueMap<String, String> requestParams) {

      UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(uriTemplate);
      int i = 0;
      for (Map.Entry<String, List<String>> entry : requestParams.entrySet()) {
        String nameVar = "queryParam" + i;
        uriVars.put(nameVar, entry.getKey());
        for (int j = 0; j < entry.getValue().size(); j++) {
          String valueVar = nameVar + "[" + j + "]";
          uriVars.put(valueVar, entry.getValue().get(j));
          uriComponentsBuilder.queryParam("{" + nameVar + "}", "{" + valueVar + "}");
        }
        i++;
      }
      return uriComponentsBuilder.build().toUriString();
    }

  }

  private static class FormDataSerializer
          extends FormHttpMessageWriter implements Function<MultiValueMap<String, String>, byte[]> {

    @Override
    public byte[] apply(MultiValueMap<String, String> requestParams) {
      Charset charset = StandardCharsets.UTF_8;
      return serializeForm(requestParams, charset).getBytes(charset);
    }

  }

}
