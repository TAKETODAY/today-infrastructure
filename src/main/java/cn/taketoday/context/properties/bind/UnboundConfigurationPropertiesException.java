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

package cn.taketoday.context.properties.bind;

import cn.taketoday.boot.context.properties.source.ConfigurationProperty;
import cn.taketoday.boot.context.properties.source.ConfigurationPropertySource;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link BindException} thrown when {@link ConfigurationPropertySource} elements were
 * left unbound.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
public class UnboundConfigurationPropertiesException extends RuntimeException {

	private final Set<ConfigurationProperty> unboundProperties;

	public UnboundConfigurationPropertiesException(Set<ConfigurationProperty> unboundProperties) {
		super(buildMessage(unboundProperties));
		this.unboundProperties = Collections.unmodifiableSet(unboundProperties);
	}

	public Set<ConfigurationProperty> getUnboundProperties() {
		return this.unboundProperties;
	}

	private static String buildMessage(Set<ConfigurationProperty> unboundProperties) {
		StringBuilder builder = new StringBuilder();
		builder.append("The elements [");
		String message = unboundProperties.stream().map((p) -> p.getName().toString()).collect(Collectors.joining(","));
		builder.append(message).append("] were left unbound.");
		return builder.toString();
	}

}
