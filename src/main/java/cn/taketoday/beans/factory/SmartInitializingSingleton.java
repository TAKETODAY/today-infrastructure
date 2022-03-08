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

package cn.taketoday.beans.factory;

import cn.taketoday.beans.factory.support.AbstractBeanFactory;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.event.ApplicationListener;

/**
 * Callback interface triggered at the end of the singleton pre-instantiation phase
 * during {@link BeanFactory} bootstrap. This interface can be implemented by
 * singleton beans in order to perform some initialization after the regular
 * singleton instantiation algorithm, avoiding side effects with accidental early
 * initialization (e.g. from {@link ConfigurableBeanFactory#getBeansOfType} calls).
 * In that sense, it is an alternative to {@link InitializingBean} which gets
 * triggered right at the end of a bean's local construction phase.
 *
 * <p>This callback variant is somewhat similar to {@link cn.taketoday.context.event.ContextStartedEvent}
 * but doesn't require an implementation of {@link ApplicationListener},
 * with no need to filter context references across a context hierarchy etc.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/3/9 12:03
 * @see AbstractBeanFactory#preInstantiateSingletons()
 * @since 4.0
 */
public interface SmartInitializingSingleton {

  /**
   * Invoked right at the end of the singleton pre-instantiation phase,
   * with a guarantee that all regular singleton beans have been created
   * already. {@link ConfigurableBeanFactory#getBeansOfType} calls within
   * this method won't trigger accidental side effects during bootstrap.
   * <p><b>NOTE:</b> This callback won't be triggered for singleton beans
   * lazily initialized on demand after {@link BeanFactory} bootstrap,
   * and not for any other bean scope either. Carefully use it for beans
   * with the intended bootstrap semantics only.
   */
  void afterSingletonsInstantiated();
}
