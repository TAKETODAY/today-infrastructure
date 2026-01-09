/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aot.hint.support;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import infra.aot.hint.BindingReflectionHintsRegistrar;
import infra.aot.hint.ExecutableMode;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.TypeReference;

/**
 * {@link RuntimeHintsRegistrar} to register hints for popular conventions in
 * {@code infra.core.conversion.support.ObjectToObjectConverter}.
 * Some dynamic hints registered by {@link BindingReflectionHintsRegistrar}.
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

    TypeReference sqlTimestampTypeReference = TypeReference.of("java.sql.Timestamp");
    reflectionHints.registerTypeIfPresent(classLoader, sqlTimestampTypeReference.getName(), hint -> hint
            .withMethod("from", List.of(TypeReference.of(Instant.class)), ExecutableMode.INVOKE)
            .onReachableType(sqlTimestampTypeReference));

    reflectionHints.registerTypeIfPresent(classLoader, "infra.http.HttpMethod",
            builder -> builder.withMethod("valueOf", List.of(TypeReference.of(String.class)), ExecutableMode.INVOKE));

    reflectionHints.registerTypeIfPresent(classLoader, "java.net.URI", MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
  }

}
