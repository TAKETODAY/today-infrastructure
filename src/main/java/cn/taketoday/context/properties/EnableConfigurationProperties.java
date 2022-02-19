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

package cn.taketoday.context.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Import;

/**
 * Enable support for {@link ConfigurationProperties @ConfigurationProperties} annotated
 * beans. {@code @ConfigurationProperties} beans can be registered in the standard way
 * (for example using {@link Bean @Bean} methods) or, for convenience, can be specified
 * directly on this annotation.
 *
 * @author Dave Syer
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EnableConfigurationPropertiesRegistrar.class)
public @interface EnableConfigurationProperties {

  /**
   * The bean name of the configuration properties validator.
   *
   * @since 4.0
   */
  String VALIDATOR_BEAN_NAME = "configurationPropertiesValidator";

  /**
   * Convenient way to quickly register
   * {@link ConfigurationProperties @ConfigurationProperties} annotated beans with
   * Spring. Standard Spring Beans will also be scanned regardless of this value.
   *
   * @return {@code @ConfigurationProperties} annotated beans to register
   */
  Class<?>[] value() default {};

}
