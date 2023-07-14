/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.util.UriComponentsBuilder;
import cn.taketoday.web.util.UriUtils;

/**
 * Container for HTTP request values extracted from an
 * {@link org.springframework.web.service.annotation.HttpExchange @HttpExchange}-annotated
 * method and argument values passed to it. This is then given to
 * {@link HttpExchangeAdapter} to adapt to the underlying HTTP client.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HttpRequestValues {

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

  protected HttpRequestValues(@Nullable HttpMethod httpMethod,
          @Nullable URI uri, @Nullable String uriTemplate, Map<String, String> uriVariables,
          HttpHeaders headers, MultiValueMap<String, String> cookies, Map<String, Object> attributes,
          @Nullable Object bodyValue) {

    Assert.isTrue(uri != null || uriTemplate != null, "Neither URI nor URI template");

    this.httpMethod = httpMethod;
    this.uri = uri;
    this.uriTemplate = uriTemplate;
    this.uriVariables = uriVariables;
    this.headers = headers;
    this.cookies = cookies;
    this.attributes = attributes;
    this.bodyValue = bodyValue;
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
   */
  @Nullable
  public Object getBodyValue() {
    return this.bodyValue;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link HttpRequestValues}.
   */
  public static class Builder {

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
    private MultiValueMap<String, Object> parts;

    @Nullable
    private Map<String, Object> attributes;

    @Nullable
    private Object bodyValue;

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
     * Add a part for a multipart request. The part may be:
     * <ul>
     * <li>String -- form field
     * <li>{@link cn.taketoday.core.io.Resource Resource} -- file part
     * <li>Object -- content to be encoded (e.g. to JSON)
     * <li>{@link HttpEntity} -- part content and headers although generally it's
     * easier to add headers through the returned builder
     * </ul>
     */
    public Builder addRequestPart(String name, Object part) {
      this.parts = this.parts != null ? this.parts : new LinkedMultiValueMap<>();
      this.parts.add(name, part);
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
     * Set the request body as an Object to be serialized.
     */
    public void setBodyValue(Object bodyValue) {
      this.bodyValue = bodyValue;
    }

    /**
     * Build the {@link HttpRequestValues} instance.
     */
    public HttpRequestValues build() {

      URI uri = this.uri;
      String uriTemplate = this.uriTemplate;
      if (uriTemplate == null) {
        uriTemplate = "";
      }

      Map<String, String> uriVars = (this.uriVars != null ? new HashMap<>(this.uriVars) : Collections.emptyMap());

      Object bodyValue = this.bodyValue;
      if (hasParts()) {
        Assert.isTrue(!hasBody(), "Expected body or request parts, not both");
        bodyValue = buildMultipartBody();
      }

      if (!CollectionUtils.isEmpty(this.requestParams)) {
        if (hasFormDataContentType()) {
          Assert.isTrue(!hasParts(), "Request parts not expected for a form data request");
          Assert.isTrue(!hasBody(), "Body not expected for a form data request");
          bodyValue = new LinkedMultiValueMap<>(this.requestParams);
        }
        else if (uri != null) {
          // insert into prepared URI
          uri = UriComponentsBuilder.fromUri(uri)
                  .queryParams(UriUtils.encodeQueryParams(this.requestParams))
                  .build(true)
                  .toUri();
        }
        else {
          // append to URI template
          uriVars = (uriVars.isEmpty() ? new HashMap<>() : uriVars);
          uriTemplate = appendQueryParams(uriTemplate, uriVars, this.requestParams);
        }
      }

      HttpHeaders headers = HttpHeaders.empty();
      if (this.headers != null) {
        headers = HttpHeaders.create();
        headers.putAll(this.headers);
      }

      MultiValueMap<String, String> cookies =
              this.cookies != null ?
              new LinkedMultiValueMap<>(this.cookies) : EMPTY_COOKIES_MAP;

      Map<String, Object> attributes = (this.attributes != null ?
                                        new HashMap<>(this.attributes) : Collections.emptyMap());

      return createRequestValues(
              this.httpMethod, uri, uriTemplate, uriVars, headers, cookies, attributes, bodyValue);
    }

    protected boolean hasParts() {
      return (this.parts != null);
    }

    protected boolean hasBody() {
      return (this.bodyValue != null);
    }

    protected Object buildMultipartBody() {
      Assert.notNull(this.parts, "`parts` is null, was hasParts() not called?");
      return this.parts;
    }

    private boolean hasFormDataContentType() {
      return (this.headers != null &&
              MediaType.APPLICATION_FORM_URLENCODED.equals(this.headers.getContentType()));
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

    protected HttpRequestValues createRequestValues(@Nullable HttpMethod httpMethod,
            @Nullable URI uri, @Nullable String uriTemplate, Map<String, String> uriVars,
            HttpHeaders headers, MultiValueMap<String, String> cookies, Map<String, Object> attributes,
            @Nullable Object bodyValue) {

      return new HttpRequestValues(
              this.httpMethod, uri, uriTemplate, uriVars, headers, cookies, attributes, bodyValue);
    }
  }

}
