/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.converter.json;

import org.jspecify.annotations.Nullable;

import tools.jackson.databind.ser.FilterProvider;

/**
 * A simple holder for the POJO to serialize via
 *
 * <p>On the server side this wrapper is added with a
 * {@code ResponseBodyInterceptor} after content negotiation selects the
 * converter to use but before to write.
 *
 * <p>On the client side, simply wrap the POJO and pass it in to the
 * {@code RestTemplate}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class MappingJacksonValue {

  private Object value;

  private @Nullable Class<?> serializationView;

  private @Nullable FilterProvider filters;

  /**
   * Create a new instance wrapping the given POJO to be serialized.
   *
   * @param value the Object to be serialized
   */
  public MappingJacksonValue(Object value) {
    this.value = value;
  }

  /**
   * Modify the POJO to serialize.
   */
  public void setValue(Object value) {
    this.value = value;
  }

  /**
   * Return the POJO that needs to be serialized.
   */
  public Object getValue() {
    return this.value;
  }

  /**
   * Set the serialization view to serialize the POJO with.
   *
   * @see com.fasterxml.jackson.databind.ObjectMapper#writerWithView(Class)
   * @see com.fasterxml.jackson.annotation.JsonView
   */
  public void setSerializationView(@Nullable Class<?> serializationView) {
    this.serializationView = serializationView;
  }

  /**
   * Return the serialization view to use.
   *
   * @see com.fasterxml.jackson.databind.ObjectMapper#writerWithView(Class)
   * @see com.fasterxml.jackson.annotation.JsonView
   */
  @Nullable
  public Class<?> getSerializationView() {
    return this.serializationView;
  }

  /**
   * Set the Jackson filter provider to serialize the POJO with.
   */
  public void setFilters(@Nullable FilterProvider filters) {
    this.filters = filters;
  }

  /**
   * Return the Jackson filter provider to use.
   */
  @Nullable
  public FilterProvider getFilters() {
    return this.filters;
  }

}
