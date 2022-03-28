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

package cn.taketoday.framework.context.config;

import cn.taketoday.framework.env.ConfigTreePropertySource;
import cn.taketoday.framework.env.ConfigTreePropertySource.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

/**
 * {@link ConfigDataLoader} for config tree locations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 4.0
 */
public class ConfigTreeConfigDataLoader implements ConfigDataLoader<ConfigTreeConfigDataResource> {

	@Override
	public ConfigData load(ConfigDataLoaderContext context, ConfigTreeConfigDataResource resource)
			throws IOException, ConfigDataResourceNotFoundException {
		Path path = resource.getPath();
		ConfigDataResourceNotFoundException.throwIfDoesNotExist(resource, path);
		String name = "Config tree '" + path + "'";
		ConfigTreePropertySource source = new ConfigTreePropertySource(name, path, Option.AUTO_TRIM_TRAILING_NEW_LINE);
		return new ConfigData(Collections.singletonList(source));
	}

}
