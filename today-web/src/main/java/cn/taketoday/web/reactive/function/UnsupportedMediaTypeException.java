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

package cn.taketoday.web.reactive.function;

import java.util.Collections;
import java.util.List;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;

/**
 * Exception thrown to indicate that a {@code Content-Type} is not supported.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class UnsupportedMediaTypeException extends NestedRuntimeException {

  @Nullable
  private final MediaType contentType;

  private final List<MediaType> supportedMediaTypes;

  @Nullable
  private final ResolvableType bodyType;

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
            (bodyType != null ? " for bodyType=" + bodyType.toString() : "");
  }

  /**
   * Return the request Content-Type header if it was parsed successfully,
   * or {@code null} otherwise.
   */
  @Nullable
  public MediaType getContentType() {
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
  @Nullable
  public ResolvableType getBodyType() {
    return this.bodyType;
  }

}
