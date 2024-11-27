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

package infra.http;

import java.util.Objects;

import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;

/**
 * Represents an HTTP request or response entity, consisting of headers and body.
 * <p>
 * like so:
 * <pre>{@code
 * HttpHeaders headers = HttpHeaders.create();
 * headers.setContentType(MediaType.TEXT_PLAIN);
 * HttpEntity<String> entity = new HttpEntity<>(helloWorld, headers);
 * URI location = template.postForLocation("https://example.com", entity);
 * }</pre>
 * or
 * <pre>{@code
 * HttpEntity<String> entity = template.getForEntity("https://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * }</pre>
 * Can also be used in Web MVC, as a return value from a @Controller method:
 * <pre>{@code
 * @RequestMapping("/handle")
 * public HttpEntity<String> handle() {
 *   HttpHeaders responseHeaders = HttpHeaders.create();
 *   responseHeaders.set("MyResponseHeader", "MyValue");
 *   return new HttpEntity<>("Hello World", responseHeaders);
 * }
 * }</pre>
 *
 * @param <T> the body type
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getBody()
 * @see #getHeaders()
 * @since 3.0 2020/12/6 17:10
 */
public class HttpEntity<T> {

  /**
   * The empty {@code HttpEntity}, with no body or headers.
   */
  public static final HttpEntity<?> EMPTY = new HttpEntity<>();

  @Nullable
  private final T body;

  @Nullable
  private HttpHeaders headers;

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
  public HttpEntity(@Nullable MultiValueMap<String, String> headers) {
    this(null, headers);
  }

  /**
   * Create a new {@code HttpEntity} with the given body and headers.
   *
   * @param body the entity body
   * @param headers the entity headers
   */
  public HttpEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers) {
    this.body = body;
    this.headers = CollectionUtils.isNotEmpty(headers) ? HttpHeaders.copyOf(headers) : null;
  }

  /**
   * Returns the headers of this entity.
   */
  public HttpHeaders getHeaders() {
    HttpHeaders headers = this.headers;
    if (headers == null) {
      headers = HttpHeaders.forWritable();
      this.headers = headers;
    }
    return headers;
  }

  /**
   * Returns the headers of this entity. maybe {@code null}
   *
   * @since 5.0
   */
  @Nullable
  public HttpHeaders headers() {
    return headers;
  }

  /**
   * Return the {@linkplain MediaType media type} of the body, as specified by the
   * {@code Content-Type} header.
   * <p>
   * Returns {@code null} when the {@code Content-Type} header is not set.
   *
   * @throws InvalidMediaTypeException if the media type value cannot be parsed
   * @since 5.0
   */
  @Nullable
  public MediaType getContentType() {
    return headers != null ? headers.getContentType() : null;
  }

  /**
   * Indicates whether this entity has a header.
   *
   * @since 5.0
   */
  public boolean hasHeader(String name) {
    return headers != null && headers.containsKey(name);
  }

  /**
   * Indicates whether this entity has headers.
   *
   * @since 5.0
   */
  public boolean hasHeaders() {
    return CollectionUtils.isNotEmpty(headers);
  }

  /**
   * Returns the body of this entity.
   */
  @Nullable
  public T getBody() {
    return body;
  }

  /**
   * Indicates whether this entity has a body.
   */
  public boolean hasBody() {
    return body != null;
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
    return Objects.equals(this.headers, otherEntity.headers)
            && Objects.equals(this.body, otherEntity.body);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.headers) * 29 + Objects.hashCode(this.body);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("<");
    if (this.body != null) {
      builder.append(this.body);
      builder.append(',');
    }
    builder.append(toString(headers));
    builder.append('>');
    return builder.toString();
  }

  static String toString(@Nullable HttpHeaders headers) {
    return headers != null ? headers.toString() : "[]";
  }

}
