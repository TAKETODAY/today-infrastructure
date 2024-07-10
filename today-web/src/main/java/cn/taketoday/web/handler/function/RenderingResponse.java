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

package cn.taketoday.web.handler.function;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.view.ModelAndView;

/**
 * Rendering-specific subtype of {@link ServerResponse} that exposes model and template data.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface RenderingResponse extends ServerResponse {

  /**
   * Return the name of the template to be rendered.
   */
  String name();

  /**
   * Return the unmodifiable model map.
   */
  Map<String, Object> model();

  // Builder

  /**
   * Create a builder with the template name, status code, headers and model of the given response.
   *
   * @param other the response to copy the values from
   * @return the created builder
   */
  static Builder from(RenderingResponse other) {
    return new DefaultRenderingResponseBuilder(other);
  }

  /**
   * Create a builder with the given template name.
   *
   * @param name the name of the template to render
   * @return the created builder
   */
  static Builder create(String name) {
    return new DefaultRenderingResponseBuilder(name);
  }

  /**
   * Create a builder with the given template name.
   *
   * @param view the name of the template to render
   * @return the created builder
   */
  static ViewBuilder create(ModelAndView view) {
    return new ModelAndViewRenderingResponseBuilder(view);
  }

  /**
   * Defines a builder for {@code RenderingResponse}.
   */
  interface Builder {

    /**
     * Add the supplied attribute to the model using a
     * {@linkplain cn.taketoday.core.Conventions#getVariableName generated name}.
     * <p><em>Note: Empty {@link Collection Collections} are not added to
     * the model when using this method because we cannot correctly determine
     * the true convention name. View code should check for {@code null} rather
     * than for empty collections.</em>
     *
     * @param attribute the model attribute value (never {@code null})
     */
    Builder modelAttribute(Object attribute);

    /**
     * Add the supplied attribute value under the supplied name.
     *
     * @param name the name of the model attribute (never {@code null})
     * @param value the model attribute value (can be {@code null})
     */
    Builder modelAttribute(String name, @Nullable Object value);

    /**
     * Copy all attributes in the supplied array into the model,
     * using attribute name generation for each element.
     *
     * @see #modelAttribute(Object)
     */
    Builder modelAttributes(Object... attributes);

    /**
     * Copy all attributes in the supplied {@code Collection} into the model,
     * using attribute name generation for each element.
     *
     * @see #modelAttribute(Object)
     */
    Builder modelAttributes(Collection<?> attributes);

    /**
     * Copy all attributes in the supplied {@code Map} into the model.
     *
     * @see #modelAttribute(String, Object)
     */
    Builder modelAttributes(Map<String, ?> attributes);

    /**
     * Add the given header value(s) under the given name.
     *
     * @param headerName the header name
     * @param headerValues the header value(s)
     * @return this builder
     * @see HttpHeaders#add(String, String)
     */
    Builder header(String headerName, String... headerValues);

    /**
     * Manipulate this response's headers with the given consumer. The
     * headers provided to the consumer are "live", so that the consumer can be used to
     * {@linkplain HttpHeaders#setOrRemove(String, String) overwrite} existing header values,
     * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
     * {@link HttpHeaders} methods.
     *
     * @param headersConsumer a function that consumes the {@code HttpHeaders}
     * @return this builder
     */
    Builder headers(Consumer<HttpHeaders> headersConsumer);

    /**
     * Add the given HttpHeaders.
     *
     * @param headers the headers
     * @return this builder
     * @see MultiValueMap#setAll(Map)
     * @since 5.0
     */
    Builder headers(@Nullable HttpHeaders headers);

    /**
     * Set the HTTP status.
     *
     * @param status the response status
     * @return this builder
     */
    Builder status(HttpStatusCode status);

    /**
     * Set the HTTP status.
     *
     * @param status the response status
     * @return this builder
     */
    Builder status(int status);

    /**
     * Add the given cookie to the response.
     *
     * @param cookie the cookie to add
     * @return this builder
     */
    Builder cookie(HttpCookie cookie);

    /**
     * Add a cookie with the given name and value(s).
     *
     * @param name the cookie name
     * @param values the cookie value(s)
     * @return this builder
     * @since 5.0
     */
    Builder cookie(String name, String... values);

    /**
     * Manipulate this response's cookies with the given consumer. The
     * cookies provided to the consumer are "live", so that the consumer can be used to
     * {@linkplain MultiValueMap#setOrRemove(Object, Object) overwrite} existing cookies,
     * {@linkplain MultiValueMap#remove(Object) remove} cookies, or use any of the other
     * {@link MultiValueMap} methods.
     *
     * @param cookiesConsumer a function that consumes the cookies
     * @return this builder
     */
    Builder cookies(Consumer<MultiValueMap<String, HttpCookie>> cookiesConsumer);

    /**
     * Add a cookies with the given name and values.
     *
     * @param cookies the cookies
     * @return this builder
     * @see MultiValueMap#setAll(Map)
     * @since 5.0
     */
    Builder cookies(@Nullable Collection<HttpCookie> cookies);

    /**
     * Add a cookies with the given name and values.
     *
     * @param cookies the cookies
     * @return this builder
     * @see MultiValueMap#setAll(Map)
     * @since 5.0
     */
    Builder cookies(@Nullable MultiValueMap<String, HttpCookie> cookies);

    /**
     * Build the response.
     *
     * @return the built response
     */
    RenderingResponse build();
  }

  /**
   * Defines a builder for {@code RenderingResponse}.
   */
  interface ViewBuilder {

    /**
     * Add the given header value(s) under the given name.
     *
     * @param headerName the header name
     * @param headerValues the header value(s)
     * @return this builder
     * @see HttpHeaders#add(String, String)
     */
    ViewBuilder header(String headerName, String... headerValues);

    /**
     * Manipulate this response's headers with the given consumer. The
     * headers provided to the consumer are "live", so that the consumer can be used to
     * {@linkplain HttpHeaders#setOrRemove(String, String) overwrite} existing header values,
     * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
     * {@link HttpHeaders} methods.
     *
     * @param headersConsumer a function that consumes the {@code HttpHeaders}
     * @return this builder
     */
    ViewBuilder headers(Consumer<HttpHeaders> headersConsumer);

    /**
     * Add the given HttpHeaders.
     *
     * @param headers the headers
     * @return this builder
     * @see MultiValueMap#setAll(Map)
     * @since 5.0
     */
    ViewBuilder headers(@Nullable HttpHeaders headers);

    /**
     * Set the HTTP status.
     *
     * @param status the response status
     * @return this builder
     */
    ViewBuilder status(HttpStatusCode status);

    /**
     * Set the HTTP status.
     *
     * @param status the response status
     * @return this builder
     */
    ViewBuilder status(int status);

    /**
     * Add the given cookie to the response.
     *
     * @param cookie the cookie to add
     * @return this builder
     */
    ViewBuilder cookie(HttpCookie cookie);

    /**
     * Add a cookie with the given name and value(s).
     *
     * @param name the cookie name
     * @param values the cookie value(s)
     * @return this builder
     * @since 5.0
     */
    ViewBuilder cookie(String name, String... values);

    /**
     * Manipulate this response's cookies with the given consumer. The
     * cookies provided to the consumer are "live", so that the consumer can be used to
     * {@linkplain MultiValueMap#setOrRemove(Object, Object) overwrite} existing cookies,
     * {@linkplain MultiValueMap#remove(Object) remove} cookies, or use any of the other
     * {@link MultiValueMap} methods.
     *
     * @param cookiesConsumer a function that consumes the cookies
     * @return this builder
     */
    ViewBuilder cookies(Consumer<MultiValueMap<String, HttpCookie>> cookiesConsumer);

    /**
     * Add a cookies with the given name and values.
     *
     * @param cookies the cookies
     * @return this builder
     * @see MultiValueMap#setAll(Map)
     * @since 5.0
     */
    ViewBuilder cookies(@Nullable Collection<HttpCookie> cookies);

    /**
     * Add a cookies with the given name and values.
     *
     * @param cookies the cookies
     * @return this builder
     * @see MultiValueMap#setAll(Map)
     * @since 5.0
     */
    ViewBuilder cookies(@Nullable MultiValueMap<String, HttpCookie> cookies);

    /**
     * Build the response.
     *
     * @return the built response
     */
    RenderingResponse build();
  }

}
