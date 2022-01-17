/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.annotation;

/**
 * A {@link Condition} that offers more fine-grained control when used with
 * {@code @Configuration}. Allows certain conditions to adapt when they match
 * based on the configuration phase. For example, a condition that checks if a bean
 * has already been registered might choose to only be evaluated during the
 * {@link ConfigurationPhase#REGISTER_BEAN REGISTER_BEAN} {@link ConfigurationPhase}.
 *
 * @author Phillip Webb
 * @see Configuration
 * @since 4.0
 */
public interface ConfigurationCondition extends Condition {

  /**
   * Return the {@link ConfigurationPhase} in which the condition should be evaluated.
   */
  ConfigurationPhase getConfigurationPhase();

  /**
   * The various configuration phases where the condition could be evaluated.
   */
  enum ConfigurationPhase {

    /**
     * The {@link Condition} should be evaluated as a {@code @Configuration}
     * class is being parsed.
     * <p>If the condition does not match at this point, the {@code @Configuration}
     * class will not be added.
     */
    PARSE_CONFIGURATION,

    /**
     * The {@link Condition} should be evaluated when adding a regular
     * (non {@code @Configuration}) bean. The condition will not prevent
     * {@code @Configuration} classes from being added.
     * <p>At the time that the condition is evaluated, all {@code @Configuration}
     * classes will have been parsed.
     */
    REGISTER_BEAN
  }

}
