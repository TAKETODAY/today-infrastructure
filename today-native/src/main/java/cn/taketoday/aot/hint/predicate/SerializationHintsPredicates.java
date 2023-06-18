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

package cn.taketoday.aot.hint.predicate;

import java.util.function.Predicate;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.SerializationHints;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.lang.Assert;

/**
 * Generator of {@link SerializationHints} predicates, testing whether the
 * given hints match the expected behavior for serialization.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public class SerializationHintsPredicates {

  SerializationHintsPredicates() {
  }

  /**
   * Return a predicate that checks whether a {@link SerializationHints
   * serialization hint} is registered for the given type.
   *
   * @param type the type to check
   * @return the {@link RuntimeHints} predicate
   * @see java.lang.reflect.Proxy
   */
  public Predicate<RuntimeHints> onType(Class<?> type) {
    Assert.notNull(type, "'type' must not be null");
    return onType(TypeReference.of(type));
  }

  /**
   * Return a predicate that checks whether a {@link SerializationHints
   * serialization hint} is registered for the given type reference.
   *
   * @param typeReference the type to check
   * @return the {@link RuntimeHints} predicate
   * @see java.lang.reflect.Proxy
   */
  public Predicate<RuntimeHints> onType(TypeReference typeReference) {
    Assert.notNull(typeReference, "'typeReference' must not be null");
    return hints -> hints.serialization().javaSerializationHints().anyMatch(
            hint -> hint.getType().equals(typeReference));
  }

}
