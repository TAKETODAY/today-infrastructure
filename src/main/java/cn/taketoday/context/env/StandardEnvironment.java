/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.env;

import cn.taketoday.context.Constant;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.loader.BeanDefinitionLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today <br>
 * 
 *         2018-11-14 21:23
 */
@Slf4j
public class StandardEnvironment implements ConfigurableEnvironment {

	private String[] profiles = new String[0];

	private Properties properties = System.getProperties();

	/** resolve beanDefinition which It is marked annotation */
	protected BeanDefinitionLoader beanDefinitionLoader;
	/** storage BeanDefinition */
	protected BeanDefinitionRegistry beanDefinitionRegistry;

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
	public <T> T getProperty(String key, Class<T> targetType) {
		return targetType.cast(properties.get(key));
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
		return profiles;
	}

	@Override
	public boolean acceptsProfiles(String... profiles) {
		if (this.profiles.length == 0) {
			return true;
		}
		for (String activeProfile : this.profiles) {
			for (String profile : profiles) {
				if (profile.equals(activeProfile)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void setActiveProfiles(@NonNull String... profiles) {
		this.profiles = profiles;
	}

	@Override
	public void addActiveProfile(String profile) {
		String[] newArray = new String[profiles.length + 1];
		System.arraycopy(profiles, 0, newArray, 0, profiles.length);
		newArray[profiles.length] = profile;
		this.profiles = newArray;
	}

	/**
	 * load properties file with given path
	 */
	@Override
	public void loadProperties(File dir) throws IOException {

		File[] listFiles = dir.listFiles(//
				file -> (file.isDirectory()) || (file.getName().endsWith(Constant.PROPERTIES_SUFFIX))//
		);

		if (listFiles == null) {
			log.warn("The path: [{}] you provided that contains nothing", dir.getAbsolutePath());
			return;
		}

		for (File file : listFiles) {
			if (file.isDirectory()) { // recursive
				loadProperties(file);
				continue;
			}
			log.debug("Found Properties File: [{}]", file.getAbsolutePath());
			try (InputStream inputStream = new FileInputStream(file)) {
				properties.load(inputStream);
			}
		}
	}

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

}
