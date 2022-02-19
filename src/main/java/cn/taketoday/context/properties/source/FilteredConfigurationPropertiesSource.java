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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties.source;

import cn.taketoday.util.Assert;

import java.util.function.Predicate;

/**
 * A filtered {@link ConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class FilteredConfigurationPropertiesSource implements ConfigurationPropertySource {

	private final ConfigurationPropertySource source;

	private final Predicate<ConfigurationPropertyName> filter;

	FilteredConfigurationPropertiesSource(ConfigurationPropertySource source,
			Predicate<ConfigurationPropertyName> filter) {
		Assert.notNull(source, "Source must not be null");
		Assert.notNull(filter, "Filter must not be null");
		this.source = source;
		this.filter = filter;
	}

	@Override
	public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
		boolean filtered = getFilter().test(name);
		return filtered ? getSource().getConfigurationProperty(name) : null;
	}

	@Override
	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		ConfigurationPropertyState result = this.source.containsDescendantOf(name);
		if (result == ConfigurationPropertyState.PRESENT) {
			// We can't be sure a contained descendant won't be filtered
			return ConfigurationPropertyState.UNKNOWN;
		}
		return result;
	}

	@Override
	public Object getUnderlyingSource() {
		return this.source.getUnderlyingSource();
	}

	protected ConfigurationPropertySource getSource() {
		return this.source;
	}

	protected Predicate<ConfigurationPropertyName> getFilter() {
		return this.filter;
	}

	@Override
	public String toString() {
		return this.source.toString() + " (filtered)";
	}

}
