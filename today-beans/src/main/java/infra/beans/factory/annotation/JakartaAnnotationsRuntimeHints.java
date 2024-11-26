/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.beans.factory.annotation;

import java.util.stream.Stream;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.TypeReference;
import infra.lang.Nullable;

/**
 * {@link RuntimeHintsRegistrar} for Jakarta annotations.
 * <p>Hints are only registered if Jakarta inject is on the classpath.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/23 20:09
 */
class JakartaAnnotationsRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    Stream.of("jakarta.inject.Inject", "jakarta.inject.Provider", "jakarta.inject.Qualifier").forEach(typeName ->
            hints.reflection().registerType(TypeReference.of(typeName)));
  }

}

