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

import org.apache.commons.logging.Log;
import cn.taketoday.framework.BootstrapContext;
import cn.taketoday.framework.BootstrapRegistry;
import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.framework.logging.DeferredLogFactory;
import cn.taketoday.framework.util.Instantiator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link EnvironmentPostProcessorsFactory} implementation that uses reflection to create
 * instances.
 *
 * @author Phillip Webb
 */
class ReflectionEnvironmentPostProcessorsFactory implements EnvironmentPostProcessorsFactory {

	private final List<Class<?>> classes;

	private ClassLoader classLoader;

	private final List<String> classNames;

	ReflectionEnvironmentPostProcessorsFactory(Class<?>... classes) {
		this.classes = new ArrayList<>(Arrays.asList(classes));
		this.classNames = null;
	}

	ReflectionEnvironmentPostProcessorsFactory(ClassLoader classLoader, String... classNames) {
		this(classLoader, Arrays.asList(classNames));
	}

	ReflectionEnvironmentPostProcessorsFactory(ClassLoader classLoader, List<String> classNames) {
		this.classes = null;
		this.classLoader = classLoader;
		this.classNames = classNames;
	}

	@Override
	public List<EnvironmentPostProcessor> getEnvironmentPostProcessors(DeferredLogFactory logFactory,
			ConfigurableBootstrapContext bootstrapContext) {
		Instantiator<EnvironmentPostProcessor> instantiator = new Instantiator<>(EnvironmentPostProcessor.class,
				(parameters) -> {
					parameters.add(DeferredLogFactory.class, logFactory);
					parameters.add(Log.class, logFactory::getLog);
					parameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
					parameters.add(BootstrapContext.class, bootstrapContext);
					parameters.add(BootstrapRegistry.class, bootstrapContext);
				});
		return (this.classes != null) ? instantiator.instantiateTypes(this.classes)
				: instantiator.instantiate(this.classLoader, this.classNames);
	}

}
