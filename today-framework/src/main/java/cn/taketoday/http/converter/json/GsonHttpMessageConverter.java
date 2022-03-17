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

import com.google.gson.Gson;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Implementation of {@link cn.taketoday.http.converter.HttpMessageConverter}
 * that can read and write JSON using the
 * <a href="https://code.google.com/p/google-gson/">Google Gson</a> library.
 *
 * <p>This converter can be used to bind to typed beans or untyped {@code HashMap}s.
 * By default, it supports {@code application/json} and {@code application/*+json} with
 * {@code UTF-8} character set.
 *
 * <p>Tested against Gson 2.8; compatible with Gson 2.0 and higher.
 *
 * @author Roy Clarkson
 * @author Juergen Hoeller
 * @see com.google.gson.Gson
 * @see com.google.gson.GsonBuilder
 * @see #setGson
 * @since 4.0
 */
public class GsonHttpMessageConverter extends AbstractJsonHttpMessageConverter {

  private Gson gson;

  /**
   * Construct a new {@code GsonHttpMessageConverter} with default configuration.
   */
  public GsonHttpMessageConverter() {
    this.gson = new Gson();
  }

  /**
   * Construct a new {@code GsonHttpMessageConverter} with the given delegate.
   *
   * @param gson the Gson instance to use
   */
  public GsonHttpMessageConverter(Gson gson) {
    Assert.notNull(gson, "A Gson instance is required");
    this.gson = gson;
  }

  /**
   * Set the {@code Gson} instance to use.
   * If not set, a default {@link Gson#Gson() Gson} instance will be used.
   * <p>Setting a custom-configured {@code Gson} is one way to take further
   * control of the JSON serialization process.
   *
   * @see #GsonHttpMessageConverter(Gson)
   */
  public void setGson(Gson gson) {
    Assert.notNull(gson, "A Gson instance is required");
    this.gson = gson;
  }

  /**
   * Return the configured {@code Gson} instance for this converter.
   */
  public Gson getGson() {
    return this.gson;
  }

  @Override
  protected Object readInternal(Type resolvedType, Reader reader) throws Exception {
    return getGson().fromJson(reader, resolvedType);
  }

  @Override
  protected void writeInternal(Object object, @Nullable Type type, Writer writer) throws Exception {
    // In Gson, toJson with a type argument will exclusively use that given type,
    // ignoring the actual type of the object... which might be more specific,
    // e.g. a subclass of the specified type which includes additional fields.
    // As a consequence, we're only passing in parameterized type declarations
    // which might contain extra generics that the object instance doesn't retain.
    if (type instanceof ParameterizedType) {
      getGson().toJson(object, type, writer);
    }
    else {
      getGson().toJson(object, writer);
    }
  }

}
