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

package cn.taketoday.context.weaving;

import cn.taketoday.beans.factory.Aware;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.instrument.classloading.LoadTimeWeaver;

/**
 * Interface to be implemented by any object that wishes to be notified
 * of the application context's default {@link LoadTimeWeaver}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see cn.taketoday.context.ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 * @since 4.0
 */
public interface LoadTimeWeaverAware extends Aware {

  /**
   * Set the {@link LoadTimeWeaver} of this object's containing
   * {@link cn.taketoday.context.ApplicationContext ApplicationContext}.
   * <p>Invoked after the population of normal bean properties but before an
   * initialization callback like
   * {@link cn.taketoday.beans.factory.InitializingBean InitializingBean's}
   * {@link cn.taketoday.beans.factory.InitializingBean#afterPropertiesSet() afterPropertiesSet()}
   * or a custom init-method. Invoked after
   * {@link ApplicationContextAware ApplicationContextAware's}
   * {@link ApplicationContextAware#setApplicationContext setApplicationContext(..)}.
   * <p><b>NOTE:</b> This method will only be called if there actually is a
   * {@code LoadTimeWeaver} available in the application context. If
   * there is none, the method will simply not get invoked, assuming that the
   * implementing object is able to activate its weaving dependency accordingly.
   *
   * @param loadTimeWeaver the {@code LoadTimeWeaver} instance (never {@code null})
   * @see cn.taketoday.beans.factory.InitializingBean#afterPropertiesSet
   * @see ApplicationContextAware#setApplicationContext
   */
  void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver);

}
