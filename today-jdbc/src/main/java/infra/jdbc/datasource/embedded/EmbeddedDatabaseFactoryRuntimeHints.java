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

package infra.jdbc.datasource.embedded;

import java.util.Collections;

import infra.aot.hint.ExecutableMode;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers reflection hints for
 * {@code EmbeddedDataSourceProxy#shutdown} in order to allow it to be used as a bean
 * destroy method.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class EmbeddedDatabaseFactoryRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    hints.reflection().registerTypeIfPresent(classLoader,
            "infra.jdbc.datasource.embedded.EmbeddedDatabaseFactory$EmbeddedDataSourceProxy",
            builder -> builder
                    .onReachableType(EmbeddedDatabaseFactory.class)
                    .withMethod("shutdown", Collections.emptyList(), ExecutableMode.INVOKE));
  }

}
