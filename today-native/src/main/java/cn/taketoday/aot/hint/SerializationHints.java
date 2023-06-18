/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.hint;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import cn.taketoday.lang.Nullable;

/**
 * Gather the need for Java serialization at runtime.
 *
 * @author Stephane Nicoll
 * @see Serializable
 * @since 4.0
 */
public class SerializationHints {

  private final Set<JavaSerializationHint> javaSerializationHints;

  public SerializationHints() {
    this.javaSerializationHints = new LinkedHashSet<>();
  }

  /**
   * Return the {@link JavaSerializationHint java serialization hints} for types
   * that need to be serialized using Java serialization at runtime.
   *
   * @return a stream of {@link JavaSerializationHint java serialization hints}
   */
  public Stream<JavaSerializationHint> javaSerializationHints() {
    return this.javaSerializationHints.stream();
  }

  /**
   * Register that the type defined by the specified {@link TypeReference}
   * need to be serialized using java serialization.
   *
   * @param type the type to register
   * @param serializationHint a builder to further customize the serialization
   * @return {@code this}, to facilitate method chaining
   */
  public SerializationHints registerType(TypeReference type, @Nullable Consumer<JavaSerializationHint.Builder> serializationHint) {
    JavaSerializationHint.Builder builder = new JavaSerializationHint.Builder(type);
    if (serializationHint != null) {
      serializationHint.accept(builder);
    }
    this.javaSerializationHints.add(builder.build());
    return this;
  }

  /**
   * Register that the type defined by the specified {@link TypeReference}
   * need to be serialized using java serialization.
   *
   * @param type the type to register
   * @return {@code this}, to facilitate method chaining
   */
  public SerializationHints registerType(TypeReference type) {
    return registerType(type, null);
  }

  /**
   * Register that the specified type need to be serialized using java
   * serialization.
   *
   * @param type the type to register
   * @param serializationHint a builder to further customize the serialization
   * @return {@code this}, to facilitate method chaining
   */
  public SerializationHints registerType(Class<? extends Serializable> type, @Nullable Consumer<JavaSerializationHint.Builder> serializationHint) {
    return registerType(TypeReference.of(type), serializationHint);
  }

  /**
   * Register that the specified type need to be serialized using java
   * serialization.
   *
   * @param type the type to register
   * @return {@code this}, to facilitate method chaining
   */
  public SerializationHints registerType(Class<? extends Serializable> type) {
    return registerType(type, null);
  }

}
