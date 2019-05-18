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

import java.io.IOException;

import javax.el.ELProcessor;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.loader.BeanDefinitionLoader;

/**
 * Configurable {@link Environment}
 * 
 * @author Today <br>
 *         2018-11-14 19:35
 */
public interface ConfigurableEnvironment extends Environment {

    /**
     * Specify the set of profiles active for this {@code Environment}
     * 
     * @param profiles
     *            Setting active profiles
     */
    void setActiveProfiles(String... profiles);

    /**
     * Add a profile to the current set of active profiles.
     * 
     * @param profile
     *            add a active profile
     */
    void addActiveProfile(String profile);

    /**
     * Load properties configuration file. No specific name required.
     * 
     * @param properties
     *            properties directory
     * @throws IOException
     *             when could not access to a properties file
     */
    void loadProperties(String properties) throws IOException;

    /**
     * Set {@link Environment} property
     * 
     * @param key
     *            key
     * @param value
     *            value
     */
    void setProperty(String key, String value);

    /**
     * Set the bean definition registry
     * 
     * @param beanDefinitionRegistry
     *            {@link BeanDefinitionRegistry} instance
     * @return {@link ConfigurableEnvironment}
     */
    ConfigurableEnvironment setBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry);

    /**
     * set bean definition loader
     * 
     * @param beanDefinitionLoader
     *            {@link BeanDefinitionLoader} instance
     * @return {@link ConfigurableEnvironment}
     */
    ConfigurableEnvironment setBeanDefinitionLoader(BeanDefinitionLoader beanDefinitionLoader);

    /**
     * 
     * @param beanNameCreator
     *            {@link BeanNameCreator} instance
     * @return {@link ConfigurableEnvironment}
     * @since 2.1.1
     */
    ConfigurableEnvironment setBeanNameCreator(BeanNameCreator beanNameCreator);

    /**
     * Set {@link ELProcessor}
     * 
     * @return {@link ELProcessor}
     * @since 2.1.5
     */
    ConfigurableEnvironment setELProcessor(ELProcessor processor);

}
