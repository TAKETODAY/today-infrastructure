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

package infra.core.conversion.support;

import infra.core.conversion.ConversionService;
import infra.core.conversion.Converter;
import infra.core.conversion.ConverterRegistry;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.ConfigurablePropertyResolver;

/**
 * Configuration interface to be implemented by most if not all {@link ConversionService}
 * types. Consolidates the read-only operations exposed by {@link ConversionService} and
 * the mutating operations of {@link ConverterRegistry} to allow for convenient ad-hoc
 * addition and removal of {@link Converter Converters}
 * through. The latter is particularly useful when working against a
 * {@link ConfigurableEnvironment ConfigurableEnvironment}
 * instance in application context bootstrapping code.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurablePropertyResolver#getConversionService()
 * @see ConfigurableEnvironment
 * @see infra.context.ConfigurableApplicationContext#getEnvironment()
 * @since 3.0 2021/3/21 17:57
 */
public interface ConfigurableConversionService extends ConversionService, ConverterRegistry {

}
