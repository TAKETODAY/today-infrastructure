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

import org.reactivestreams.Publisher;

import java.net.URI;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.client.MultipartBodyBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;

/**
 * {@link HttpRequestValues} extension for use with {@link ReactorHttpExchangeAdapter}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class ReactiveHttpRequestValues extends HttpRequestValues {

  @Nullable
  private final Publisher<?> body;

  @Nullable
  private final ParameterizedTypeReference<?> bodyElementType;

  private ReactiveHttpRequestValues(@Nullable HttpMethod httpMethod,
          @Nullable URI uri, @Nullable String uriTemplate, Map<String, String> uriVariables,
          HttpHeaders headers, MultiValueMap<String, String> cookies, Map<String, Object> attributes,
          @Nullable Object bodyValue, @Nullable Publisher<?> body, @Nullable ParameterizedTypeReference<?> elementType) {

    super(httpMethod, uri, uriTemplate, uriVariables, headers, cookies, attributes, bodyValue);

    this.body = body;
    this.bodyElementType = elementType;
  }

  /**
   * Return a {@link Publisher} that will produce for the request body.
   * <p>This is mutually exclusive with {@link #getBodyValue()}.
   * Only one of the two or neither is set.
   */
  @Nullable
  public Publisher<?> getBodyPublisher() {
    return this.body;
  }

  /**
   * Return the element type for a {@linkplain #getBodyPublisher() Publisher body}.
   */
  @Nullable
  public ParameterizedTypeReference<?> getBodyPublisherElementType() {
    return this.bodyElementType;
  }

  /**
   * Return the request body as a Publisher.
   * <p>This is mutually exclusive with {@link #getBodyValue()}.
   * Only one of the two or neither is set.
   */
  @Nullable
  public Publisher<?> getBody() {
    return getBodyPublisher();
  }

  /**
   * Return the element type for a {@linkplain #getBodyPublisher() Publisher body}.
   */
  @Nullable
  public ParameterizedTypeReference<?> getBodyElementType() {
    return getBodyPublisherElementType();
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link ReactiveHttpRequestValues}.
   */
  public final static class Builder extends HttpRequestValues.Builder {

    @Nullable
    private MultipartBodyBuilder multipartBuilder;

    @Nullable
    private Publisher<?> body;

    @Nullable
    private ParameterizedTypeReference<?> bodyElementType;

    @Override
    public Builder setHttpMethod(HttpMethod httpMethod) {
      super.setHttpMethod(httpMethod);
      return this;
    }

    @Override
    public Builder setUri(URI uri) {
      super.setUri(uri);
      return this;
    }

    @Override
    public Builder setUriTemplate(String uriTemplate) {
      super.setUriTemplate(uriTemplate);
      return this;
    }

    @Override
    public Builder setUriVariable(String name, String value) {
      super.setUriVariable(name, value);
      return this;
    }

    @Override
    public Builder setAccept(List<MediaType> acceptableMediaTypes) {
      super.setAccept(acceptableMediaTypes);
      return this;
    }

    @Override
    public Builder setContentType(MediaType contentType) {
      super.setContentType(contentType);
      return this;
    }

    @Override
    public Builder addHeader(String headerName, String... headerValues) {
      super.addHeader(headerName, headerValues);
      return this;
    }

    @Override
    public Builder addCookie(String name, String... values) {
      super.addCookie(name, values);
      return this;
    }

    @Override
    public Builder addRequestParameter(String name, String... values) {
      super.addRequestParameter(name, values);
      return this;
    }

    @Override
    public Builder addAttribute(String name, Object value) {
      super.addAttribute(name, value);
      return this;
    }

    /**
     * Add a part to a multipart request. The part value may be as described
     * in {@link MultipartBodyBuilder#part(String, Object)}.
     */
    @Override
    public Builder addRequestPart(String name, Object part) {
      this.multipartBuilder = (this.multipartBuilder != null ? this.multipartBuilder : new MultipartBodyBuilder());
      this.multipartBuilder.part(name, part);
      return this;
    }

    /**
     * Variant of {@link #addRequestPart(String, Object)} that allows the
     * part value to be produced by a {@link Publisher}.
     */
    public <T, P extends Publisher<T>> Builder addRequestPartPublisher(
            String name, P publisher, ParameterizedTypeReference<T> elementTye) {

      this.multipartBuilder = (this.multipartBuilder != null ? this.multipartBuilder : new MultipartBodyBuilder());
      this.multipartBuilder.asyncPart(name, publisher, elementTye);
      return this;
    }

    public <T, P extends Publisher<T>> Builder addRequestPart(String name, P publisher, ResolvableType type) {
      return addRequestPartPublisher(name, publisher, ParameterizedTypeReference.forType(type.getType()));
    }

    /**
     * {@inheritDoc}
     * <p>This is mutually exclusive with, and resets any previously set
     * {@linkplain #setBodyPublisher(Publisher, ParameterizedTypeReference)}.
     */
    @Override
    public void setBodyValue(Object bodyValue) {
      super.setBodyValue(bodyValue);
      this.body = null;
      this.bodyElementType = null;
    }

    /**
     * Set the request body as a Reactive Streams Publisher.
     * <p>This is mutually exclusive with, and resets any previously set
     * {@linkplain #setBodyValue(Object) body value}.
     */
    @SuppressWarnings("DataFlowIssue")
    public <T, P extends Publisher<T>> void setBodyPublisher(P body, ParameterizedTypeReference<T> elementTye) {
      this.body = body;
      this.bodyElementType = elementTye;
      super.setBodyValue(null);
    }

    public <T, P extends Publisher<T>> void setBody(P body, ParameterizedTypeReference<T> elementTye) {
      setBodyPublisher(body, elementTye);
    }

    @Override
    public ReactiveHttpRequestValues build() {
      return (ReactiveHttpRequestValues) super.build();
    }

    @Override
    protected boolean hasParts() {
      return (this.multipartBuilder != null);
    }

    @Override
    protected boolean hasBody() {
      return (super.hasBody() || this.body != null);
    }

    @Override
    protected Object buildMultipartBody() {
      Assert.notNull(this.multipartBuilder, "`multipartBuilder` is null, was hasParts() not called?");
      return this.multipartBuilder.build();
    }

    @Override
    protected ReactiveHttpRequestValues createRequestValues(@Nullable HttpMethod httpMethod,
            @Nullable URI uri, @Nullable String uriTemplate, Map<String, String> uriVars,
            HttpHeaders headers, MultiValueMap<String, String> cookies, Map<String, Object> attributes,
            @Nullable Object bodyValue) {

      return new ReactiveHttpRequestValues(
              httpMethod, uri, uriTemplate, uriVars, headers, cookies, attributes,
              bodyValue, this.body, this.bodyElementType);
    }

  }

}
