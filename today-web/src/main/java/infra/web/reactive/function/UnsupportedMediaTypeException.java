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

package infra.web.reactive.function;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import infra.core.NestedRuntimeException;
import infra.core.ResolvableType;
import infra.http.MediaType;

/**
 * Exception thrown to indicate that a {@code Content-Type} is not supported.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UnsupportedMediaTypeException extends NestedRuntimeException {

  private final @Nullable MediaType contentType;

  private final @Nullable ResolvableType bodyType;

  private final List<MediaType> supportedMediaTypes;

  /**
   * Constructor for when trying to encode from or decode to a specific Java type.
   */
  public UnsupportedMediaTypeException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
          @Nullable ResolvableType bodyType) {

    super(initReason(contentType, bodyType));
    this.contentType = contentType;
    this.supportedMediaTypes = Collections.unmodifiableList(supportedTypes);
    this.bodyType = bodyType;
  }

  private static String initReason(@Nullable MediaType contentType, @Nullable ResolvableType bodyType) {
    return "Content type '" + (contentType != null ? contentType : "") + "' not supported" +
            (bodyType != null ? " for bodyType=" + bodyType : "");
  }

  /**
   * Return the request Content-Type header if it was parsed successfully,
   * or {@code null} otherwise.
   */
  public @Nullable MediaType getContentType() {
    return this.contentType;
  }

  /**
   * Return the list of supported content types in cases when the Content-Type
   * header is parsed but not supported, or an empty list otherwise.
   */
  public List<MediaType> getSupportedMediaTypes() {
    return this.supportedMediaTypes;
  }

  /**
   * Return the body type in the context of which this exception was generated.
   * This is applicable when the exception was raised as a result trying to
   * encode from or decode to a specific Java type.
   *
   * @return the body type, or {@code null} if not available
   */
  public @Nullable ResolvableType getBodyType() {
    return this.bodyType;
  }

}
