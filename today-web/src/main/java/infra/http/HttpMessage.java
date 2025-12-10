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

package infra.http;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Represents the base interface for HTTP request and response messages.
 * Consists of {@link HttpHeaders}, retrievable via {@link #getHeaders()}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface HttpMessage {

  /**
   * Return the headers of this message.
   *
   * @return a corresponding HttpHeaders object (never {@code null})
   */
  HttpHeaders getHeaders();

  /**
   * Return the first header value for the given header name, if any.
   *
   * @param name the header name
   * @return the first header value, or {@code null} if none
   * @since 5.0
   */
  default @Nullable String getHeader(String name) {
    return getHeaders().getFirst(name);
  }

  /**
   * Returns the values of the specified header as a {@code List<String>}.
   * If the request does not have a header with the specified name,
   * this method returns an empty list.
   *
   * @param name the header name
   * @return all header values, or an empty list if none
   * @see HttpHeaders#getOrEmpty(String)
   * @since 5.0
   */
  default List<String> getHeaders(String name) {
    return getHeaders().getOrEmpty(name);
  }

  /**
   * Return the header names for this message.
   *
   * @return a collection of header names
   * @since 5.0
   */
  default Collection<String> getHeaderNames() {
    return getHeaders().keySet();
  }

  /**
   * Return the {@code Content-Type} header value, if any.
   *
   * @return the content type, or {@code null} if not defined
   * @throws InvalidMediaTypeException if the media type value cannot be parsed
   * @see HttpHeaders#getContentType()
   * @since 5.0
   */
  default @Nullable MediaType getContentType() {
    return getHeaders().getContentType();
  }

  /**
   * Return the {@code Content-Type} header value as a string, if any.
   *
   * @return the content type as a string, or {@code null} if not defined
   * @see HttpHeaders#getContentType()
   * @since 5.0
   */
  default @Nullable String getContentTypeAsString() {
    return getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
  }

  /**
   * Determine the content length for this message.
   *
   * @return the content length, or -1 if not defined
   * @see HttpHeaders#getContentLength()
   * @see HttpHeaders#setContentLength(long)
   * @since 5.0
   */
  default long getContentLength() {
    return getHeaders().getContentLength();
  }

}
