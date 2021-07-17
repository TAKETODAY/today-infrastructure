/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.expression.ExpressionProcessor;

/**
 * Configurable {@link Environment}
 *
 * @author TODAY <br>
 * 2018-11-14 19:35
 */
public interface ConfigurableEnvironment extends Environment {

  /**
   * Specify the set of profiles active for this {@code Environment}
   *
   * @param profiles
   *         Setting active profiles
   */
  void setActiveProfiles(String... profiles);

  /**
   * Add a profile to the current set of active profiles.
   *
   * @param profile
   *         Add a active profile
   *
   * @deprecated since 3.0.6
   */
  @Deprecated
  void addActiveProfile(String profile);

  /**
   * Add profiles to the current set of active profiles.
   *
   * @param profiles
   *         Add active profiles
   *
   * @since 3.0.6
   */
  void addActiveProfile(String... profiles);

  /**
   * Load properties configuration file. No specific name required.
   *
   * @param propertiesLocation
   *         The properties file location
   *
   * @throws IOException
   *         When could not access to a properties file
   */
  void loadProperties(String propertiesLocation) throws IOException;

  /**
   * Load properties configuration file, and set active profiles.
   *
   * @throws IOException
   *         When could not access to a properties file
   * @since 2.1.6
   */
  void loadProperties() throws IOException;

  /**
   * Set {@link Environment} property
   *
   * @param key
   *         Key
   * @param value
   *         Value
   */
  void setProperty(String key, String value);

  /**
   * Configure the bean definition registry
   *
   * @param beanDefinitionRegistry
   *         {@link BeanDefinitionRegistry} instance
   *
   * @return {@link ConfigurableEnvironment}
   */
  ConfigurableEnvironment setBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry);

  /**
   * Configure bean definition loader
   *
   * @param beanDefinitionLoader
   *         {@link BeanDefinitionLoader} instance
   *
   * @return {@link ConfigurableEnvironment}
   */
  ConfigurableEnvironment setBeanDefinitionLoader(BeanDefinitionLoader beanDefinitionLoader);

  /**
   * Configure {@link BeanNameCreator}
   *
   * @param beanNameCreator
   *         {@link BeanNameCreator} instance
   *
   * @return {@link ConfigurableEnvironment}
   *
   * @since 2.1.1
   */
  ConfigurableEnvironment setBeanNameCreator(BeanNameCreator beanNameCreator);

  /**
   * Configure properties location
   *
   * @param propertiesLocation
   *         The location of properties file
   *
   * @return {@link ConfigurableEnvironment}
   */
  ConfigurableEnvironment setPropertiesLocation(String propertiesLocation);

  /**
   * Configure expression processor
   *
   * @param expressionProcessor
   *         {@link ExpressionProcessor} object
   *
   * @return {@link ConfigurableEnvironment}
   *
   * @since 2.1.7
   */
  ConfigurableEnvironment setExpressionProcessor(ExpressionProcessor expressionProcessor);

}
