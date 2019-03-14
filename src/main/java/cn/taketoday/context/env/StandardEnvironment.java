/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.context.env;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.el.ELProcessor;
import javax.el.ExpressionFactory;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.Constant;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Standard implementation of {@link Environment}
 * 
 * @author Today <br>
 * 
 *         2018-11-14 21:23
 */
@Slf4j
public class StandardEnvironment implements ConfigurableEnvironment {

	private Set<String> activeProfiles = new HashSet<>();

	private final Properties properties = System.getProperties();

	private BeanNameCreator beanNameCreator;

	/** resolve beanDefinition which It is marked annotation */
	private BeanDefinitionLoader beanDefinitionLoader;
	/** storage BeanDefinition */
	private BeanDefinitionRegistry beanDefinitionRegistry;

	private ELProcessor elProcessor;

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public boolean containsProperty(String key) {
		return properties.containsKey(key);
	}

	@Override
	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	@Override
	public BeanDefinitionRegistry getBeanDefinitionRegistry() {
		return beanDefinitionRegistry;
	}

	@Override
	public BeanDefinitionLoader getBeanDefinitionLoader() {
		return beanDefinitionLoader;
	}

	@Override
	public String[] getActiveProfiles() {
		return StringUtils.toStringArray(activeProfiles);
	}

	// ---ConfigurableEnvironment

	@Override
	public void setActiveProfiles(String... profiles) {
		this.activeProfiles.addAll(Arrays.asList(profiles));
		log.info("Active profiles: {}", activeProfiles);
	}

	@Override
	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	@Override
	public void addActiveProfile(String profile) {
		log.info("Add active profile: [{}]", profile);
		this.activeProfiles.add(profile);
	}

	/**
	 * Load properties file with given path
	 */
	@Override
	public void loadProperties(String properties) throws IOException {

		Objects.requireNonNull(properties, "Properties dir can't be null");

		URL resource = ClassUtils.getClassLoader().getResource(properties);
		if (resource == null) {
			log.warn("The path: [{}] you provided that doesn't exist", properties);
			return;
		}
		final File dir = new File(resource.getPath());

		log.debug("Start loading Properties.");
		doLoad(dir, this.properties);

		final String profiles = getProperty(Constant.KEY_ACTIVE_PROFILES);
		if (StringUtils.isNotEmpty(profiles)) {
			setActiveProfiles(StringUtils.split(profiles));
		}
	}

	/**
	 * Do load
	 * 
	 * @param dir
	 *            base dir
	 * @param properties
	 *            properties
	 * @throws IOException
	 */
	static void doLoad(File dir, Properties properties) throws IOException {

		File[] listFiles = dir.listFiles(PROPERTIES_FILE_FILTER);

		if (listFiles == null) {
			log.warn("The path: [{}] you provided that contains nothing", dir.getAbsolutePath());
			return;
		}

		for (File file : listFiles) {
			if (file.isDirectory()) { // recursive
				doLoad(file, properties);
				continue;
			}
			log.debug("Found Properties File: [{}]", file.getAbsolutePath());
			try (InputStream inputStream = new FileInputStream(file)) {
				properties.load(inputStream);
			}
		}
	}

	/**
	 * Properties file filter
	 */
	private static final FileFilter PROPERTIES_FILE_FILTER = new FileFilter() {
		@Override
		public boolean accept(File file) {
			if (file.isDirectory()) {
				return true;
			}
			final String name = file.getName();
			return name.endsWith(Constant.PROPERTIES_SUFFIX) && !name.startsWith("pom"); // pom.properties
		}
	};

	@Override
	public ConfigurableEnvironment setBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) {
		this.beanDefinitionRegistry = beanDefinitionRegistry;
		return this;
	}

	@Override
	public ConfigurableEnvironment setBeanDefinitionLoader(BeanDefinitionLoader beanDefinitionLoader) {
		this.beanDefinitionLoader = beanDefinitionLoader;
		return this;
	}

	@Override
	public boolean acceptsProfiles(String... profiles) {

		for (String profile : profiles) {
			if (StringUtils.isNotEmpty(profile) && profile.charAt(0) == '!') {
				if (!activeProfiles.contains(profile.substring(1))) {
					return true;
				}
			}
			else if (activeProfiles.contains(profile)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ConfigurableEnvironment setBeanNameCreator(BeanNameCreator beanNameCreator) {
		this.beanNameCreator = beanNameCreator;
		return this;
	}

	@Override
	public BeanNameCreator getBeanNameCreator() {
		return beanNameCreator;
	}

	@Override
	public ELProcessor getELProcessor() {
		return elProcessor;
	}

	@Override
	public ConfigurableEnvironment setELProcessor(final ELProcessor elProcessor) {

		try {
			this.elProcessor = elProcessor;

			Field declaredField = ClassUtils.forName("javax.el.ELUtil").getDeclaredField("exprFactory");
			declaredField.setAccessible(true);

			declaredField.set(null, ExpressionFactory.newInstance(getProperties()));
		}
		catch (Throwable e) {
			e = ExceptionUtils.unwrapThrowable(e);
			log.error("An Exception Occurred When Loading Context, With Msg: [{}]", e.getMessage(), e);

			throw ExceptionUtils.newContextException(e);
		}
		return this;
	}

}
