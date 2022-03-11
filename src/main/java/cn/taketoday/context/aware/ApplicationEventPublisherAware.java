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

package cn.taketoday.context.aware;

import cn.taketoday.beans.factory.Aware;
import cn.taketoday.context.ApplicationEventPublisher;

/**
 * Interface to be implemented by any object that wishes to be notified
 * of the ApplicationEventPublisher (typically the ApplicationContext)
 * that it runs in.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author TODAY 2021/10/7 17:03
 * @see ApplicationContextAware
 * @since 4.0
 */
public interface ApplicationEventPublisherAware extends Aware {

  /**
   * Set the ApplicationEventPublisher that this object runs in.
   * <p>Invoked after population of normal bean properties but before an init
   * callback like InitializingBean's afterPropertiesSet or a custom init-method.
   * Invoked before ApplicationContextAware's setApplicationContext.
   *
   * @param applicationEventPublisher event publisher to be used by this object
   */
  void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher);

}
