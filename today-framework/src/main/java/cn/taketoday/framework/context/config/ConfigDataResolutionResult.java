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

/**
 * Result returned from {@link ConfigDataLocationResolvers} containing both the
 * {@link ConfigDataResource} and the original {@link ConfigDataLocation}.
 *
 * @author Phillip Webb
 */
class ConfigDataResolutionResult {

	private final ConfigDataLocation location;

	private final ConfigDataResource resource;

	private final boolean profileSpecific;

	ConfigDataResolutionResult(ConfigDataLocation location, ConfigDataResource resource, boolean profileSpecific) {
		this.location = location;
		this.resource = resource;
		this.profileSpecific = profileSpecific;
	}

	ConfigDataLocation getLocation() {
		return this.location;
	}

	ConfigDataResource getResource() {
		return this.resource;
	}

	boolean isProfileSpecific() {
		return this.profileSpecific;
	}

}
