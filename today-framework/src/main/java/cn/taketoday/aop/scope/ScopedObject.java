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

package cn.taketoday.aop.scope;

import cn.taketoday.aop.RawTargetAccess;

/**
 * An AOP introduction interface for scoped objects.
 *
 * <p>Objects created from the {@link ScopedProxyFactoryBean} can be cast
 * to this interface, enabling access to the raw target object
 * and programmatic removal of the target object.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ScopedProxyFactoryBean
 * @since 4.0
 */
public interface ScopedObject extends RawTargetAccess {

  /**
   * Return the current target object behind this scoped object proxy,
   * in its raw form (as stored in the target scope).
   * <p>The raw target object can for example be passed to persistence
   * providers which would not be able to handle the scoped proxy object.
   *
   * @return the current target object behind this scoped object proxy
   */
  Object getTargetObject();

  /**
   * Remove this object from its target scope, for example from
   * the backing session.
   * <p>Note that no further calls may be made to the scoped object
   * afterwards (at least within the current thread, that is, with
   * the exact same target object in the target scope).
   */
  void removeFromScope();

}
