/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

/**
 * Implementation of {@link cn.taketoday.http.converter.HttpMessageConverter}
 * that can read and write JSON using the
 * <a href="http://json-b.net/">JSON Binding API</a>.
 *
 * <p>This converter can be used to bind to typed beans or untyped {@code HashMap}s.
 * By default, it supports {@code application/json} and {@code application/*+json} with
 * {@code UTF-8} character set.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see jakarta.json.bind.Jsonb
 * @see jakarta.json.bind.JsonbBuilder
 * @see #setJsonb
 * @since 4.0
 */
public class JsonbHttpMessageConverter extends AbstractJsonHttpMessageConverter {

  private Jsonb jsonb;

  /**
   * Construct a new {@code JsonbHttpMessageConverter} with default configuration.
   */
  public JsonbHttpMessageConverter() {
    this(JsonbBuilder.create());
  }

  /**
   * Construct a new {@code JsonbHttpMessageConverter} with the given configuration.
   *
   * @param config the {@code JsonbConfig} for the underlying delegate
   */
  public JsonbHttpMessageConverter(JsonbConfig config) {
    this.jsonb = JsonbBuilder.create(config);
  }

  /**
   * Construct a new {@code JsonbHttpMessageConverter} with the given delegate.
   *
   * @param jsonb the Jsonb instance to use
   */
  public JsonbHttpMessageConverter(Jsonb jsonb) {
    Assert.notNull(jsonb, "A Jsonb instance is required");
    this.jsonb = jsonb;
  }

  /**
   * Set the {@code Jsonb} instance to use.
   * If not set, a default {@code Jsonb} instance will be created.
   * <p>Setting a custom-configured {@code Jsonb} is one way to take further
   * control of the JSON serialization process.
   *
   * @see #JsonbHttpMessageConverter(Jsonb)
   * @see #JsonbHttpMessageConverter(JsonbConfig)
   * @see JsonbBuilder
   */
  public void setJsonb(Jsonb jsonb) {
    Assert.notNull(jsonb, "A Jsonb instance is required");
    this.jsonb = jsonb;
  }

  /**
   * Return the configured {@code Jsonb} instance for this converter.
   */
  public Jsonb getJsonb() {
    return this.jsonb;
  }

  @Override
  protected Object readInternal(Type resolvedType, Reader reader) throws Exception {
    return getJsonb().fromJson(reader, resolvedType);
  }

  @Override
  protected void writeInternal(Object object, @Nullable Type type, Writer writer) throws Exception {
    if (type instanceof ParameterizedType) {
      getJsonb().toJson(object, type, writer);
    }
    else {
      getJsonb().toJson(object, writer);
    }
  }

  @Override
  protected boolean supportsRepeatableWrites(Object o) {
    return true;
  }

}
