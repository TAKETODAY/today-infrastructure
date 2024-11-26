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

package infra.web.service.invoker;

import infra.core.MethodParameter;
import infra.core.conversion.ConversionService;
import infra.http.MediaType;
import infra.lang.Nullable;
import infra.util.MultiValueMap;
import infra.web.annotation.RequestParam;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestParam @RequestParam}
 * annotated arguments.
 *
 * <p>When {@code "content-type"} is set to
 * {@code "application/x-www-form-urlencoded"}, request parameters are encoded
 * in the request body. Otherwise, they are added as URL query parameters.
 *
 * <p>The argument may be:
 * <ul>
 * <li>{@code Map<String, ?>} or
 * {@link MultiValueMap MultiValueMap&lt;String, ?&gt;} with
 * multiple request parameter and value(s).
 * <li>{@code Collection} or an array of request parameters.
 * <li>An individual request parameter.
 * </ul>
 *
 * <p>Individual request parameters may be Strings or Objects to be converted to
 * String values through the configured {@link ConversionService}.
 *
 * <p>If the value is required but {@code null}, {@link IllegalArgumentException}
 * is raised. The value is not required if:
 * <ul>
 * <li>{@link RequestParam#required()} is set to {@code false}
 * <li>{@link RequestParam#defaultValue()} provides a fallback value
 * <li>The argument is declared as {@link java.util.Optional}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RequestParamArgumentResolver extends AbstractNamedValueArgumentResolver {

  private boolean favorSingleValue;

  public RequestParamArgumentResolver(ConversionService conversionService) {
    super(conversionService);
  }

  /**
   * Whether to format multiple values (e.g. collection, array) as a single
   * String value through the configured {@link ConversionService} unless the
   * content type is form data, or it is a multipart request.
   * <p>By default, this is {@code false} in which case formatting is not applied,
   * and a separate parameter with the same name is created for each value.
   *
   * @since 5.0
   */
  public void setFavorSingleValue(boolean favorSingleValue) {
    this.favorSingleValue = favorSingleValue;
  }

  /**
   * Return the setting for {@link #setFavorSingleValue favorSingleValue}.
   *
   * @since 5.0
   */
  public boolean isFavorSingleValue() {
    return this.favorSingleValue;
  }

  @Override
  @Nullable
  protected NamedValueInfo createNamedValueInfo(MethodParameter parameter, HttpRequestValues.Metadata metadata) {
    RequestParam annot = parameter.getParameterAnnotation(RequestParam.class);
    if (annot == null) {
      return null;
    }
    return new NamedValueInfo(annot.name(), annot.required(), annot.defaultValue(),
            "request parameter", supportsMultipleValues(parameter, metadata));
  }

  @Override
  protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
    // Shouldn't be called since we override createNamedValueInfo with HttpRequestValues.Metadata
    throw new UnsupportedOperationException();
  }

  /**
   * Determine whether the resolver should send multi-value request parameters
   * as individual values. If not, they are formatted to a single String value.
   * The default implementation uses {@link #isFavorSingleValue()} to decide
   * unless the content type is form data, or it is a multipart request.
   *
   * @since 5.0
   */
  protected boolean supportsMultipleValues(MethodParameter parameter, HttpRequestValues.Metadata metadata) {
    return (!isFavorSingleValue() || isFormOrMultipartContent(metadata));
  }

  /**
   * Whether the content type is form data, or it is a multipart request.
   *
   * @since 5.0
   */
  protected boolean isFormOrMultipartContent(HttpRequestValues.Metadata metadata) {
    MediaType mediaType = metadata.getContentType();
    return (mediaType != null && (mediaType.getType().equals("multipart") ||
            mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)));
  }

  @Override
  protected void addRequestValue(
          String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

    requestValues.addRequestParameter(name, (String) value);
  }

}
