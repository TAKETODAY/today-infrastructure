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

package cn.taketoday.aop;

import cn.taketoday.aop.framework.AdvisedSupport;
import cn.taketoday.aop.framework.ProxyCreatorSupport;

/**
 * Listener to be registered on {@link ProxyCreatorSupport} objects
 * Allows for receiving callbacks on activation and change of advice.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/4/11 18:04
 * @see ProxyCreatorSupport#addListener
 * @since 3.0
 */
public interface AdvisedSupportListener {

  /**
   * Invoked when the first proxy is created.
   *
   * @param advised the AdvisedSupport object
   */
  void activated(AdvisedSupport advised);

  /**
   * Invoked when advice is changed after a proxy is created.
   *
   * @param advised the AdvisedSupport object
   */
  void adviceChanged(AdvisedSupport advised);
}
