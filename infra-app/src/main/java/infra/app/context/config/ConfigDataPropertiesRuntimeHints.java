/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.context.config;

import org.jspecify.annotations.Nullable;

import infra.aot.hint.ExecutableMode;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.context.properties.bind.BindableRuntimeHintsRegistrar;
import infra.util.ReflectionUtils;

/**
 * {@link RuntimeHintsRegistrar} for {@link ConfigDataProperties}.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigDataPropertiesRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  @SuppressWarnings("NullAway")
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    BindableRuntimeHintsRegistrar.forTypes(ConfigDataProperties.class).registerHints(hints);
    hints.reflection()
            .registerMethod(ReflectionUtils.findMethod(ConfigDataLocation.class, "valueOf", String.class),
                    ExecutableMode.INVOKE);
  }

}
