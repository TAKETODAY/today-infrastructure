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

package cn.taketoday.aot.hint.support;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.lang.Nullable;

/**
 * {@link RuntimeHintsRegistrar} to register hints for popular conventions in
 * {@code cn.taketoday.core.conversion.support.ObjectToObjectConverter}.
 *
 * @author Sebastien Deleuze
 * @since 4.0
 */
class ObjectToObjectConverterRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    ReflectionHints reflectionHints = hints.reflection();
    TypeReference sqlDateTypeReference = TypeReference.of("java.sql.Date");
    reflectionHints.registerTypeIfPresent(classLoader, sqlDateTypeReference.getName(), hint -> hint
            .withMethod("toLocalDate", Collections.emptyList(), ExecutableMode.INVOKE)
            .onReachableType(sqlDateTypeReference)
            .withMethod("valueOf", List.of(TypeReference.of(LocalDate.class)), ExecutableMode.INVOKE)
            .onReachableType(sqlDateTypeReference));

    reflectionHints.registerTypeIfPresent(classLoader, "cn.taketoday.http.HttpMethod",
            builder -> builder.withMethod("valueOf", List.of(TypeReference.of(String.class)), ExecutableMode.INVOKE));
    reflectionHints.registerTypeIfPresent(classLoader, "java.net.URI", MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
  }

}
