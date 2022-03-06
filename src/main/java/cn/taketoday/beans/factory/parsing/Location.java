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

package cn.taketoday.beans.factory.parsing;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Assert;

/**
 * Class that models an arbitrary location in a {@link Resource resource}.
 *
 * <p>Typically used to track the location of problematic or erroneous
 * metadata in XML configuration files. For example, a
 * {@link #getSource() source} location might be 'The bean defined on
 * line 76 of beans.properties has an invalid Class'; another source might
 * be the actual DOM Element from a parsed XML {@link org.w3c.dom.Document};
 * or the source object might simply be {@code null}.
 *
 * @author Rob Harrop
 * @since 4.0
 */
public class Location {

	private final Resource resource;

	@Nullable
	private final Object source;


	/**
	 * Create a new instance of the {@link Location} class.
	 * @param resource the resource with which this location is associated
	 */
	public Location(Resource resource) {
		this(resource, null);
	}

	/**
	 * Create a new instance of the {@link Location} class.
	 * @param resource the resource with which this location is associated
	 * @param source the actual location within the associated resource
	 * (may be {@code null})
	 */
	public Location(Resource resource, @Nullable Object source) {
		Assert.notNull(resource, "Resource must not be null");
		this.resource = resource;
		this.source = source;
	}


	/**
	 * Get the resource with which this location is associated.
	 */
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * Get the actual location within the associated {@link #getResource() resource}
	 * (may be {@code null}).
	 * <p>See the {@link Location class level javadoc for this class} for examples
	 * of what the actual type of the returned object may be.
	 */
	@Nullable
	public Object getSource() {
		return this.source;
	}

}
