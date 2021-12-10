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

package cn.taketoday.transaction.support;

/**
 * Generic interface to be implemented by resource holders.
 * Allows Framework's transaction infrastructure to introspect
 * and reset the holder when necessary.
 *
 * @author Juergen Hoeller
 * @see ResourceHolderSupport
 * @see ResourceHolderSynchronization
 * @since 4.0
 */
public interface ResourceHolder {

  /**
   * Reset the transactional state of this holder.
   */
  void reset();

  /**
   * Notify this holder that it has been unbound from transaction synchronization.
   */
  void unbound();

  /**
   * Determine whether this holder is considered as 'void',
   * i.e. as a leftover from a previous thread.
   */
  boolean isVoid();

}
