/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.http;

import java.util.Objects;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;

/**
 * Represents an HTTP request or response entity, consisting of headers and body.
 *
 * like so:
 * <pre class="code">
 * HttpHeaders headers = HttpHeaders.create();
 * headers.setContentType(MediaType.TEXT_PLAIN);
 * HttpEntity&lt;String&gt; entity = new HttpEntity&lt;String&gt;(helloWorld, headers);
 * URI location = template.postForLocation("https://example.com", entity);
 * </pre>
 * or
 * <pre class="code">
 * HttpEntity&lt;String&gt; entity = template.getForEntity("https://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * </pre>
 * Can also be used in Web MVC, as a return value from a @Controller method:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public HttpEntity&lt;String&gt; handle() {
 *   HttpHeaders responseHeaders = HttpHeaders.create();
 *   responseHeaders.set("MyResponseHeader", "MyValue");
 *   return new HttpEntity&lt;String&gt;("Hello World", responseHeaders);
 * }
 * </pre>
 *
 * @param <T> the body type
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author TODAY 2020/12/6 17:10
 * @see #getBody()
 * @see #getHeaders()
 * @since 3.0
 */
public class HttpEntity<T> {

  /**
   * The empty {@code HttpEntity}, with no body or headers.
   */
  public static final HttpEntity<?> EMPTY = new HttpEntity<>();

  private final HttpHeaders headers;

  @Nullable
  private final T body;

  /**
   * Create a new, empty {@code HttpEntity}.
   */
  protected HttpEntity() {
    this(null, null);
  }

  /**
   * Create a new {@code HttpEntity} with the given body and no headers.
   *
   * @param body the entity body
   */
  public HttpEntity(T body) {
    this(body, null);
  }

  /**
   * Create a new {@code HttpEntity} with the given headers and no body.
   *
   * @param headers the entity headers
   */
  public HttpEntity(MultiValueMap<String, String> headers) {
    this(null, headers);
  }

  /**
   * Create a new {@code HttpEntity} with the given body and headers.
   *
   * @param body the entity body
   * @param headers the entity headers
   */
  public HttpEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers) {
    DefaultHttpHeaders tempHeaders = new DefaultHttpHeaders();
    if (headers != null) {
      tempHeaders.addAll(headers);
    }
    this.body = body;
    this.headers = tempHeaders;
  }

  /**
   * Returns the headers of this entity.
   */
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  /**
   * Returns the body of this entity.
   */
  @Nullable
  public T getBody() {
    return this.body;
  }

  /**
   * Indicates whether this entity has a body.
   */
  public boolean hasBody() {
    return (this.body != null);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || other.getClass() != getClass()) {
      return false;
    }
    HttpEntity<?> otherEntity = (HttpEntity<?>) other;
    return (Objects.equals(this.headers, otherEntity.headers) &&
            Objects.equals(this.body, otherEntity.body));
  }

  @Override
  public int hashCode() {
    return (Objects.hashCode(this.headers) * 29 + Objects.hashCode(this.body));
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("<");
    if (this.body != null) {
      builder.append(this.body);
      builder.append(',');
    }
    builder.append(this.headers);
    builder.append('>');
    return builder.toString();
  }

}
