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

package cn.taketoday.orm.jpa;

import jakarta.persistence.EntityManager;

/**
 * Subinterface of {@link EntityManager} to be implemented by
 * EntityManager proxies. Allows access to the underlying target EntityManager.
 *
 * <p>This interface is mainly intended for framework usage. Application code
 * should prefer the use of the {@link EntityManager#getDelegate()}
 * method to access native functionality of the underlying resource.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface EntityManagerProxy extends EntityManager {

  /**
   * Return the underlying EntityManager that this proxy will delegate to.
   * <p>In case of an extended EntityManager, this will be the associated
   * raw EntityManager.
   * <p>In case of a shared ("transactional") EntityManager, this will be
   * the raw EntityManager that is currently associated with the transaction.
   * Outside of a transaction, an IllegalStateException will be thrown.
   *
   * @return the underlying raw EntityManager (never {@code null})
   * @throws IllegalStateException if no underlying EntityManager is available
   */
  EntityManager getTargetEntityManager() throws IllegalStateException;

}
