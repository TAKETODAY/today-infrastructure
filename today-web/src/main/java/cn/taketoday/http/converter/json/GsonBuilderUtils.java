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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Base64;

/**
 * A simple utility class for obtaining a Google Gson 2.x {@link GsonBuilder}
 * which Base64-encodes {@code byte[]} properties when reading and writing JSON.
 *
 * @author Juergen Hoeller
 * @author Roy Clarkson
 * @see GsonFactoryBean#setBase64EncodeByteArrays
 * @since 4.0
 */
public abstract class GsonBuilderUtils {

  /**
   * Obtain a {@link GsonBuilder} which Base64-encodes {@code byte[]}
   * properties when reading and writing JSON.
   * <p>A custom {@link com.google.gson.TypeAdapter} will be registered via
   * {@link GsonBuilder#registerTypeHierarchyAdapter(Class, Object)} which
   * serializes a {@code byte[]} property to and from a Base64-encoded String
   * instead of a JSON array.
   */
  public static GsonBuilder gsonBuilderWithBase64EncodedByteArrays() {
    GsonBuilder builder = new GsonBuilder();
    builder.registerTypeHierarchyAdapter(byte[].class, new Base64TypeAdapter());
    return builder;
  }

  private static class Base64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

    @Override
    public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
      return new JsonPrimitive(Base64.getEncoder().encodeToString(src));
    }

    @Override
    public byte[] deserialize(JsonElement json, Type type, JsonDeserializationContext cxt) {
      return Base64.getDecoder().decode(json.getAsString());
    }
  }

}
