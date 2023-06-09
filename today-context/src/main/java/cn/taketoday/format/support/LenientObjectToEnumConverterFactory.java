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

package cn.taketoday.format.support;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.ConverterFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;

/**
 * Abstract base class for converting from a type to a {@link Enum}.
 *
 * @param <T> the source type
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("rawtypes")
abstract class LenientObjectToEnumConverterFactory<T> implements ConverterFactory<T, Enum<?>> {

  private static final Map<String, List<String>> ALIASES;

  static {
    MultiValueMap<String, String> aliases = MultiValueMap.forLinkedHashMap();
    aliases.add("true", "on");
    aliases.add("false", "off");
    ALIASES = Collections.unmodifiableMap(aliases);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E extends Enum<?>> Converter<T, E> getConverter(Class<E> targetType) {
    Class<?> enumType = targetType;
    while (enumType != null && !enumType.isEnum()) {
      enumType = enumType.getSuperclass();
    }
    if (enumType == null) {
      throw new IllegalArgumentException(
              "The target type " + targetType.getName() + " does not refer to an enum");

    }
    return new LenientToEnumConverter<>((Class<E>) enumType);
  }

  @SuppressWarnings("unchecked")
  private class LenientToEnumConverter<E extends Enum> implements Converter<T, E> {

    private final Class<E> enumType;

    LenientToEnumConverter(Class<E> enumType) {
      this.enumType = enumType;
    }

    @Nullable
    @Override
    public E convert(T source) {
      String value = source.toString().trim();
      if (value.isEmpty()) {
        return null;
      }
      try {
        return (E) Enum.valueOf(this.enumType, value);
      }
      catch (Exception ex) {
        return findEnum(value);
      }
    }

    private E findEnum(String value) {
      String name = getCanonicalName(value);
      List<String> aliases = ALIASES.getOrDefault(name, Collections.emptyList());
      for (E candidate : (Set<E>) EnumSet.allOf(this.enumType)) {
        String candidateName = getCanonicalName(candidate.name());
        if (name.equals(candidateName) || aliases.contains(candidateName)) {
          return candidate;
        }
      }
      throw new IllegalArgumentException("No enum constant " + this.enumType.getCanonicalName() + "." + value);
    }

    private String getCanonicalName(String name) {
      StringBuilder canonicalName = new StringBuilder(name.length());
      name.chars()
              .filter(Character::isLetterOrDigit)
              .map(Character::toLowerCase)
              .forEach((c) -> canonicalName.append((char) c));
      return canonicalName.toString();
    }

  }

}
