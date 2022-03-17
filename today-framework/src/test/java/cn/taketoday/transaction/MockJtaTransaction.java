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

package cn.taketoday.transaction;

import javax.transaction.xa.XAResource;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

/**
 * @author Juergen Hoeller
 * @since 31.08.2004
 */
public class MockJtaTransaction implements jakarta.transaction.Transaction {

  private Synchronization synchronization;

  @Override
  public int getStatus() {
    return Status.STATUS_ACTIVE;
  }

  @Override
  public void registerSynchronization(Synchronization synchronization) {
    this.synchronization = synchronization;
  }

  public Synchronization getSynchronization() {
    return synchronization;
  }

  @Override
  public boolean enlistResource(XAResource xaResource) {
    return false;
  }

  @Override
  public boolean delistResource(XAResource xaResource, int i) {
    return false;
  }

  @Override
  public void commit() {
  }

  @Override
  public void rollback() {
  }

  @Override
  public void setRollbackOnly() {
  }

}
