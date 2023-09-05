/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.annotation.config.context;

import cn.taketoday.beans.factory.annotation.DisableDependencyInjection;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.EnableConfigurationProperties;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link ConfigurationProperties @ConfigurationProperties} beans. Automatically binds and
 * validates any bean annotated with {@code @ConfigurationProperties}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableConfigurationProperties
 * @see ConfigurationProperties
 * @since 4.0
 */
@AutoConfiguration
@DisableDependencyInjection
@EnableConfigurationProperties
public class ConfigurationPropertiesAutoConfiguration {

}
