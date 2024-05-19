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

package cn.taketoday.http;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Representation for an RFC 9457 problem detail. Includes spec-defined
 * properties, and a {@link #getProperties() properties} map for additional,
 * non-standard properties.
 *
 * <p>For an extended response, an application can add to the
 * {@link #getProperties() properties} map. When using the Jackson library, the
 * {@code properties} map is expanded as top level JSON properties through the
 * {@link cn.taketoday.http.converter.json.ProblemDetailJacksonMixin}.
 *
 * <p>For an extended response, an application can also create a subclass with
 * additional properties. Subclasses can use the protected copy constructor to
 * re-create an existing {@code ProblemDetail} instance as the subclass, e.g.
 * from an {@code @ControllerAdvice} such as
 * {@link cn.taketoday.web.handler.ResponseEntityExceptionHandler}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc9457">RFC 9457</a>
 * @see cn.taketoday.web.ErrorResponse
 * @see cn.taketoday.web.ErrorResponseException
 * @since 4.0 2022/3/2 12:58
 */
public class ProblemDetail {

  private static final URI BLANK_TYPE = URI.create("about:blank");

  private URI type = BLANK_TYPE;

  @Nullable
  private String title;

  private int status;

  @Nullable
  private String detail;

  @Nullable
  private URI instance;

  @Nullable
  private Map<String, Object> properties;

  /**
   * For deserialization.
   */
  protected ProblemDetail() { }

  /**
   * Protected constructor for subclasses.
   * <p>To create a {@link ProblemDetail} instance, use static factory methods,
   * {@link #forStatus(HttpStatusCode)} or {@link #forRawStatusCode(int)}.
   *
   * @param rawStatusCode the response status to use
   */
  protected ProblemDetail(int rawStatusCode) {
    this.status = rawStatusCode;
  }

  /**
   * Copy constructor that could be used from a subclass to re-create a
   * {@code ProblemDetail} in order to extend it with more fields.
   */
  protected ProblemDetail(ProblemDetail other) {
    this.type = other.type;
    this.title = other.title;
    this.status = other.status;
    this.detail = other.detail;
    this.instance = other.instance;
    this.properties = other.properties != null ? new LinkedHashMap<>(other.properties) : null;
  }

  /**
   * Variant of {@link #setType(URI)} for chained initialization.
   *
   * @param type the problem type
   * @return the same instance
   */
  public ProblemDetail withType(URI type) {
    setType(type);
    return this;
  }

  /**
   * Variant of {@link #setTitle(String)} for chained initialization.
   *
   * @param title the problem title
   * @return the same instance
   */
  public ProblemDetail withTitle(@Nullable String title) {
    setTitle(title);
    return this;
  }

  /**
   * Variant of {@link #setStatus(int)} for chained initialization.
   *
   * @param status the response status for the problem
   * @return the same instance
   */
  public ProblemDetail withStatus(HttpStatusCode status) {
    Assert.notNull(status, "HttpStatusCode is required");
    setStatus(status.value());
    return this;
  }

  /**
   * Variant of {@link #setStatus(int)} for chained initialization.
   *
   * @param status the response status value for the problem
   * @return the same instance
   */
  public ProblemDetail withRawStatusCode(int status) {
    setStatus(status);
    return this;
  }

  /**
   * Variant of {@link #setDetail(String)} for chained initialization.
   *
   * @param detail the problem detail
   * @return the same instance
   */
  public ProblemDetail withDetail(@Nullable String detail) {
    setDetail(detail);
    return this;
  }

  /**
   * Variant of {@link #setInstance(URI)} for chained initialization.
   *
   * @param instance the problem instance URI
   * @return the same instance
   */
  public ProblemDetail withInstance(@Nullable URI instance) {
    setInstance(instance);
    return this;
  }

  // Setters for deserialization

  /**
   * Setter for the {@link #getType() problem type}.
   * <p>By default, this is {@link #BLANK_TYPE}.
   *
   * @param type the problem type
   * @see #withType(URI)
   */
  public void setType(URI type) {
    Assert.notNull(type, "'type' is required");
    this.type = type;
  }

  /**
   * Setter for the {@link #getTitle() problem title}.
   * <p>By default, if not explicitly set and the status is well-known, this
   * is sourced from the {@link HttpStatus#getReasonPhrase()}.
   *
   * @param title the problem title
   * @see #withTitle(String)
   */
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  /**
   * Setter for the {@link #getStatus() problem status}.
   *
   * @param status the problem status
   * @see #withStatus(HttpStatusCode)
   * @see #withRawStatusCode(int)
   */
  public void setStatus(int status) {
    this.status = status;
  }

  /**
   * Setter for the {@link #getStatus() problem status}.
   *
   * @param httpStatus the problem status
   */
  public void setStatus(HttpStatus httpStatus) {
    this.status = httpStatus.value();
  }

  /**
   * Setter for the {@link #getDetail() problem detail}.
   * <p>By default, this is not set.
   *
   * @param detail the problem detail
   * @see #withDetail(String)
   */
  public void setDetail(@Nullable String detail) {
    this.detail = detail;
  }

  /**
   * Setter for the {@link #getInstance() problem instance}.
   * <p>By default, when {@code ProblemDetail} is returned from an
   * {@code @ExceptionHandler} method, this is initialized to the request path.
   *
   * @param instance the problem instance
   * @see #withInstance(URI)
   */
  public void setInstance(@Nullable URI instance) {
    this.instance = instance;
  }

  /**
   * Set a "dynamic" property to be added to a generic {@link #getProperties()
   * properties map}.
   * <p>When Jackson JSON is present on the classpath, any properties set here
   * are rendered as top level key-value pairs in the output JSON. Otherwise,
   * they are rendered as a {@code "properties"} sub-map.
   *
   * @param name the property name
   * @param value the property value
   * @see cn.taketoday.http.converter.json.ProblemDetailJacksonMixin
   */
  public void setProperty(String name, @Nullable Object value) {
    if (properties == null) {
      this.properties = new LinkedHashMap<>();
    }
    properties.put(name, value);
  }

  /**
   * Setter for the {@link #getProperties() properties map}.
   * <p>By default, this is not set.
   * <p>When Jackson JSON is present on the classpath, any properties set here
   * are rendered as top level key-value pairs in the output JSON. Otherwise,
   * they are rendered as a {@code "properties"} sub-map.
   *
   * @param properties the properties map
   */
  public void setProperties(@Nullable Map<String, Object> properties) {
    this.properties = properties;
  }

  // Getters

  /**
   * Return the configured {@link #setType(URI) problem type}.
   */
  public URI getType() {
    return this.type;
  }

  /**
   * Return the configured {@link #setTitle(String) problem title}.
   */
  @Nullable
  public String getTitle() {
    if (this.title == null) {
      HttpStatus httpStatus = HttpStatus.resolve(this.status);
      if (httpStatus != null) {
        return httpStatus.getReasonPhrase();
      }
    }
    return this.title;
  }

  /**
   * Return the status associated with the problem, provided either to the
   * constructor or configured via {@link #setStatus(int)}.
   */
  public int getStatus() {
    return this.status;
  }

  /**
   * Return the configured {@link #setDetail(String) problem detail}.
   */
  @Nullable
  public String getDetail() {
    return this.detail;
  }

  /**
   * Return the configured {@link #setInstance(URI) problem instance}.
   */
  @Nullable
  public URI getInstance() {
    return this.instance;
  }

  /**
   * Return a generic map of properties that are not known ahead of time,
   * possibly {@code null} if no properties have been added. To add a property,
   * use {@link #setProperty(String, Object)}.
   * <p>When Jackson JSON is present on the classpath, the content of this map
   * is unwrapped and rendered as top level key-value pairs in the output JSON.
   * Otherwise, they are rendered as a {@code "properties"} sub-map.
   *
   * @see cn.taketoday.http.converter.json.ProblemDetailJacksonMixin
   */
  @Nullable
  public Map<String, Object> getProperties() {
    return this.properties;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof ProblemDetail otherDetail) {
      return status == otherDetail.status
              && Objects.equals(type, otherDetail.type)
              && Objects.equals(getTitle(), otherDetail.getTitle())
              && Objects.equals(detail, otherDetail.detail)
              && Objects.equals(instance, otherDetail.instance)
              && Objects.equals(properties, otherDetail.properties);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + Objects.hashCode(getTitle());
    result = 31 * result + status;
    result = 31 * result + Objects.hashCode(detail);
    result = 31 * result + Objects.hashCode(instance);
    result = 31 * result + Objects.hashCode(properties);
    return result;
  }

  @Override
  public String toString() {
    return "%s[%s]".formatted(getClass().getSimpleName(), initToStringContent());
  }

  /**
   * Return a String representation of the {@code ProblemDetail} fields.
   * Subclasses can override this to append additional fields.
   */
  protected String initToStringContent() {
    return "type='%s', title='%s', status=%d, detail='%s', instance='%s', properties='%s'"
            .formatted(getType(), getTitle(), getStatus(), getDetail(), getInstance(), getProperties());
  }

  // Static factory methods

  /**
   * Create a {@code ProblemDetail} instance with the given status code.
   */
  public static ProblemDetail forStatus(HttpStatusCode status) {
    Assert.notNull(status, "HttpStatusCode is required");
    return forRawStatusCode(status.value());
  }

  /**
   * Create a {@code ProblemDetail} instance with the given status value.
   */
  public static ProblemDetail forRawStatusCode(int status) {
    return new ProblemDetail(status);
  }

  /**
   * Create a {@code ProblemDetail} instance with the given status and detail.
   */
  public static ProblemDetail forStatusAndDetail(HttpStatusCode status, String detail) {
    return forStatus(status).withDetail(detail);
  }

}
