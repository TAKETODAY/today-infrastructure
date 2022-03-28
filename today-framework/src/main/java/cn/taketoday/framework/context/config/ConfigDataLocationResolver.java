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

import org.apache.commons.logging.Log;
import cn.taketoday.framework.BootstrapContext;
import cn.taketoday.framework.BootstrapRegistry;
import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.framework.context.properties.bind.Binder;
import cn.taketoday.framework.logging.DeferredLogFactory;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.ResourceLoader;

import java.util.Collections;
import java.util.List;

/**
 * Strategy interface used to resolve {@link ConfigDataLocation locations} into one or
 * more {@link ConfigDataResource resources}. Implementations should be added as a
 * {@code spring.factories} entries. The following constructor parameter types are
 * supported:
 * <ul>
 * <li>{@link Log} or {@link DeferredLogFactory} - if the resolver needs deferred
 * logging</li>
 * <li>{@link Binder} - if the resolver needs to obtain values from the initial
 * {@link Environment}</li>
 * <li>{@link ResourceLoader} - if the resolver needs a resource loader</li>
 * <li>{@link ConfigurableBootstrapContext} - A bootstrap context that can be used to
 * store objects that may be expensive to create, or need to be shared
 * ({@link BootstrapContext} or {@link BootstrapRegistry} may also be used).</li>
 * </ul>
 * <p>
 * Resolvers may implement {@link Ordered} or use the {@link Order @Order} annotation. The
 * first resolver that supports the given location will be used.
 *
 * @param <R> the location type
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
public interface ConfigDataLocationResolver<R extends ConfigDataResource> {

	/**
	 * Returns if the specified location address can be resolved by this resolver.
	 * @param context the location resolver context
	 * @param location the location to check.
	 * @return if the location is supported by this resolver
	 */
	boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location);

	/**
	 * Resolve a {@link ConfigDataLocation} into one or more {@link ConfigDataResource}
	 * instances.
	 * @param context the location resolver context
	 * @param location the location that should be resolved
	 * @return a list of {@link ConfigDataResource resources} in ascending priority order.
	 * @throws ConfigDataLocationNotFoundException on a non-optional location that cannot
	 * be found
	 * @throws ConfigDataResourceNotFoundException if a resolved resource cannot be found
	 */
	List<R> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location)
			throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException;

	/**
	 * Resolve a {@link ConfigDataLocation} into one or more {@link ConfigDataResource}
	 * instances based on available profiles. This method is called once profiles have
	 * been deduced from the contributed values. By default this method returns an empty
	 * list.
	 * @param context the location resolver context
	 * @param location the location that should be resolved
	 * @param profiles profile information
	 * @return a list of resolved locations in ascending priority order.
	 * @throws ConfigDataLocationNotFoundException on a non-optional location that cannot
	 * be found
	 */
	default List<R> resolveProfileSpecific(ConfigDataLocationResolverContext context, ConfigDataLocation location,
			Profiles profiles) throws ConfigDataLocationNotFoundException {
		return Collections.emptyList();
	}

}
