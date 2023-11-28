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

package cn.taketoday.http.converter.json;

import com.fasterxml.jackson.databind.ser.FilterProvider;

import cn.taketoday.lang.Nullable;

/**
 * A simple holder for the POJO to serialize via
 * {@link MappingJackson2HttpMessageConverter} along with further
 * serialization instructions to be passed in to the converter.
 *
 * <p>On the server side this wrapper is added with a
 * {@code ResponseBodyInterceptor} after content negotiation selects the
 * converter to use but before the write.
 *
 * <p>On the client side, simply wrap the POJO and pass it in to the
 * {@code RestTemplate}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MappingJacksonValue {

  private Object value;

  @Nullable
  private Class<?> serializationView;

  @Nullable
  private FilterProvider filters;

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
   *
   * @see com.fasterxml.jackson.databind.ObjectMapper#writer(FilterProvider)
   * @see com.fasterxml.jackson.annotation.JsonFilter
   * @see Jackson2ObjectMapperBuilder#filters(FilterProvider)
   */
  public void setFilters(@Nullable FilterProvider filters) {
    this.filters = filters;
  }

  /**
   * Return the Jackson filter provider to use.
   *
   * @see com.fasterxml.jackson.databind.ObjectMapper#writer(FilterProvider)
   * @see com.fasterxml.jackson.annotation.JsonFilter
   */
  @Nullable
  public FilterProvider getFilters() {
    return this.filters;
  }

}
