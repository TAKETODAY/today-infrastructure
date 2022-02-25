/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.core.conversion.support;

import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.ConverterRegistry;

/**
 * Configuration interface to be implemented by most if not all {@link ConversionService}
 * types. Consolidates the read-only operations exposed by {@link ConversionService} and
 * the mutating operations of {@link ConverterRegistry} to allow for convenient ad-hoc
 * addition and removal of {@link cn.taketoday.core.conversion.Converter Converters}
 * through. The latter is particularly useful when working against a
 * {@link cn.taketoday.core.env.ConfigurableEnvironment ConfigurableEnvironment}
 * instance in application context bootstrapping code.
 *
 * @author Chris Beams
 * @author TODAY 2021/3/21 17:57
 * @see cn.taketoday.core.env.ConfigurablePropertyResolver#getConversionService()
 * @see cn.taketoday.core.env.ConfigurableEnvironment
 * @see cn.taketoday.context.ConfigurableApplicationContext#getEnvironment()
 * @since 3.0
 */
public interface ConfigurableConversionService extends ConversionService, ConverterRegistry {

}
