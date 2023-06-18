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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import cn.taketoday.aot.hint.TypeHint.Builder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Gather the need for reflection at runtime.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class ReflectionHints {

  private final Map<TypeReference, TypeHint.Builder> types = new HashMap<>();

  /**
   * Return the types that require reflection.
   *
   * @return the type hints
   */
  public Stream<TypeHint> typeHints() {
    return this.types.values().stream().map(TypeHint.Builder::build);
  }

  /**
   * Return the reflection hints for the type defined by the specified
   * {@link TypeReference}.
   *
   * @param type the type to inspect
   * @return the reflection hints for this type, or {@code null}
   */
  @Nullable
  public TypeHint getTypeHint(TypeReference type) {
    Builder typeHintBuilder = this.types.get(type);
    return (typeHintBuilder != null ? typeHintBuilder.build() : null);
  }

  /**
   * Return the reflection hints for the specified type.
   *
   * @param type the type to inspect
   * @return the reflection hints for this type, or {@code null}
   */
  @Nullable
  public TypeHint getTypeHint(Class<?> type) {
    return getTypeHint(TypeReference.of(type));
  }

  /**
   * Register or customize reflection hints for the type defined by the
   * specified {@link TypeReference}.
   *
   * @param type the type to customize
   * @param typeHint a builder to further customize hints for that type
   * @return {@code this}, to facilitate method chaining
   * @see #registerType(TypeReference, MemberCategory...)
   */
  public ReflectionHints registerType(TypeReference type, Consumer<TypeHint.Builder> typeHint) {
    Builder builder = this.types.computeIfAbsent(type, TypeHint.Builder::new);
    typeHint.accept(builder);
    return this;
  }

  /**
   * Register or customize reflection hints for the specified type
   * using the specified {@link MemberCategory MemberCategories}.
   *
   * @param type the type to customize
   * @param memberCategories the member categories to apply
   * @return {@code this}, to facilitate method chaining
   */
  public ReflectionHints registerType(TypeReference type, MemberCategory... memberCategories) {
    return registerType(type, TypeHint.builtWith(memberCategories));
  }

  /**
   * Register or customize reflection hints for the specified type.
   *
   * @param type the type to customize
   * @param typeHint a builder to further customize hints for that type
   * @return {@code this}, to facilitate method chaining
   * @see #registerType(Class, MemberCategory...)
   */
  public ReflectionHints registerType(Class<?> type, Consumer<TypeHint.Builder> typeHint) {
    Assert.notNull(type, "'type' must not be null");
    if (type.getCanonicalName() != null) {
      registerType(TypeReference.of(type), typeHint);
    }
    return this;
  }

  /**
   * Register or customize reflection hints for the specified type
   * using the specified {@link MemberCategory MemberCategories}.
   *
   * @param type the type to customize
   * @param memberCategories the member categories to apply
   * @return {@code this}, to facilitate method chaining
   */
  public ReflectionHints registerType(Class<?> type, MemberCategory... memberCategories) {
    Assert.notNull(type, "'type' must not be null");
    if (type.getCanonicalName() != null) {
      registerType(TypeReference.of(type), memberCategories);
    }
    return this;
  }

  /**
   * Register or customize reflection hints for the specified type if it
   * is available using the specified {@link ClassLoader}.
   *
   * @param classLoader the classloader to use to check if the type is present
   * @param typeName the type to customize
   * @param typeHint a builder to further customize hints for that type
   * @return {@code this}, to facilitate method chaining
   * @see #registerTypeIfPresent(ClassLoader, String, MemberCategory...)
   */
  public ReflectionHints registerTypeIfPresent(@Nullable ClassLoader classLoader,
          String typeName, Consumer<TypeHint.Builder> typeHint) {

    if (ClassUtils.isPresent(typeName, classLoader)) {
      registerType(TypeReference.of(typeName), typeHint);
    }
    return this;
  }

  /**
   * Register or customize reflection hints for the specified type if it
   * is available using the specified {@link ClassLoader}.
   *
   * @param classLoader the classloader to use to check if the type is present
   * @param typeName the type to customize
   * @param memberCategories the member categories to apply
   * @return {@code this}, to facilitate method chaining
   */
  public ReflectionHints registerTypeIfPresent(@Nullable ClassLoader classLoader,
          String typeName, MemberCategory... memberCategories) {

    return registerTypeIfPresent(classLoader, typeName, TypeHint.builtWith(memberCategories));
  }

  /**
   * Register or customize reflection hints for the types defined by the
   * specified list of {@link TypeReference type references}. The specified
   * {@code typeHint} consumer is invoked for each type.
   *
   * @param types the types to customize
   * @param typeHint a builder to further customize hints for each type
   * @return {@code this}, to facilitate method chaining
   */
  public ReflectionHints registerTypes(Iterable<TypeReference> types, Consumer<TypeHint.Builder> typeHint) {
    types.forEach(type -> registerType(type, typeHint));
    return this;
  }

  /**
   * Register the need for reflection on the specified {@link Field}.
   *
   * @param field the field that requires reflection
   * @return {@code this}, to facilitate method chaining
   */
  public ReflectionHints registerField(Field field) {
    return registerType(TypeReference.of(field.getDeclaringClass()),
            typeHint -> typeHint.withField(field.getName()));
  }

  /**
   * Register the need for reflection on the specified {@link Constructor},
   * using the specified {@link ExecutableMode}.
   *
   * @param constructor the constructor that requires reflection
   * @param mode the requested mode
   * @return {@code this}, to facilitate method chaining
   */
  public ReflectionHints registerConstructor(Constructor<?> constructor, ExecutableMode mode) {
    return registerType(TypeReference.of(constructor.getDeclaringClass()),
            typeHint -> typeHint.withConstructor(mapParameters(constructor), mode));
  }

  /**
   * Register the need for reflection on the specified {@link Method},
   * using the specified {@link ExecutableMode}.
   *
   * @param method the method that requires reflection
   * @param mode the requested mode
   * @return {@code this}, to facilitate method chaining
   */
  public ReflectionHints registerMethod(Method method, ExecutableMode mode) {
    return registerType(TypeReference.of(method.getDeclaringClass()),
            typeHint -> typeHint.withMethod(method.getName(), mapParameters(method), mode));
  }

  private List<TypeReference> mapParameters(Executable executable) {
    return TypeReference.listOf(executable.getParameterTypes());
  }

}
