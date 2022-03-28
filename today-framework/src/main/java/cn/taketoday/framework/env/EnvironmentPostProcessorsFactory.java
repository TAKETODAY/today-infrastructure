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

package cn.taketoday.framework.env;

import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.framework.logging.DeferredLogFactory;
import cn.taketoday.core.io.support.SpringFactoriesLoader;

import java.util.List;

/**
 * Factory interface used by the {@link EnvironmentPostProcessorApplicationListener} to
 * create the {@link EnvironmentPostProcessor} instances.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
@FunctionalInterface
public interface EnvironmentPostProcessorsFactory {

	/**
	 * Create all requested {@link EnvironmentPostProcessor} instances.
	 * @param logFactory a deferred log factory
	 * @param bootstrapContext a bootstrap context
	 * @return the post processor instances
	 */
	List<EnvironmentPostProcessor> getEnvironmentPostProcessors(DeferredLogFactory logFactory,
			ConfigurableBootstrapContext bootstrapContext);

	/**
	 * Return a {@link EnvironmentPostProcessorsFactory} backed by
	 * {@code spring.factories}.
	 * @param classLoader the source class loader
	 * @return an {@link EnvironmentPostProcessorsFactory} instance
	 */
	static EnvironmentPostProcessorsFactory fromSpringFactories(ClassLoader classLoader) {
		return new ReflectionEnvironmentPostProcessorsFactory(classLoader,
				SpringFactoriesLoader.loadFactoryNames(EnvironmentPostProcessor.class, classLoader));
	}

	/**
	 * Return a {@link EnvironmentPostProcessorsFactory} that reflectively creates post
	 * processors from the given classes.
	 * @param classes the post processor classes
	 * @return an {@link EnvironmentPostProcessorsFactory} instance
	 */
	static EnvironmentPostProcessorsFactory of(Class<?>... classes) {
		return new ReflectionEnvironmentPostProcessorsFactory(classes);
	}

	/**
	 * Return a {@link EnvironmentPostProcessorsFactory} that reflectively creates post
	 * processors from the given class names.
	 * @param classNames the post processor class names
	 * @return an {@link EnvironmentPostProcessorsFactory} instance
	 */
	static EnvironmentPostProcessorsFactory of(String... classNames) {
		return of(null, classNames);
	}

	/**
	 * Return a {@link EnvironmentPostProcessorsFactory} that reflectively creates post
	 * processors from the given class names.
	 * @param classLoader the source class loader
	 * @param classNames the post processor class names
	 * @return an {@link EnvironmentPostProcessorsFactory} instance
	 * @since 2.4.8
	 */
	static EnvironmentPostProcessorsFactory of(ClassLoader classLoader, String... classNames) {
		return new ReflectionEnvironmentPostProcessorsFactory(classLoader, classNames);
	}

}
