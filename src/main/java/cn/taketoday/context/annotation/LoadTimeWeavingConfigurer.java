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

import cn.taketoday.context.classloading.LoadTimeWeaver;
import cn.taketoday.scheduling.annotation.EnableAsync;

/**
 * Interface to be implemented by
 * {@link Configuration @Configuration}
 * classes annotated with {@link EnableLoadTimeWeaving @EnableLoadTimeWeaving} that wish to
 * customize the {@link LoadTimeWeaver} instance to be used.
 *
 * <p>See {@link EnableAsync @EnableAsync}
 * for usage examples and information on how a default {@code LoadTimeWeaver}
 * is selected when this interface is not used.
 *
 * @author Chris Beams
 * @see LoadTimeWeavingConfiguration
 * @see EnableLoadTimeWeaving
 * @since 4.0
 */
public interface LoadTimeWeavingConfigurer {

  /**
   * Create, configure and return the {@code LoadTimeWeaver} instance to be used. Note
   * that it is unnecessary to annotate this method with {@code @Bean}, because the
   * object returned will automatically be registered as a bean by
   * {@link LoadTimeWeavingConfiguration#loadTimeWeaver()}
   */
  LoadTimeWeaver getLoadTimeWeaver();

}
