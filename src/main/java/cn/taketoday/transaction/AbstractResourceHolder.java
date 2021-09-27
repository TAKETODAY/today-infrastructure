/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.transaction;

/**
 * @author TODAY <br>
 * 2018-10-09 11:08
 */
public abstract class AbstractResourceHolder implements ResourceHolder {

  private long deadline;
  private int referenceCount = 0;
  private boolean isVoid = false;
  private boolean rollbackOnly = false;

  private boolean synchronizedWithTransaction = false;

  /**
   * Mark the resource as synchronized with a transaction.
   */
  public void setSynchronizedWithTransaction(boolean synchronizedWithTransaction) {
    this.synchronizedWithTransaction = synchronizedWithTransaction;
  }

  /**
   * Return whether the resource is synchronized with a transaction.
   */
  public boolean isSynchronizedWithTransaction() {
    return this.synchronizedWithTransaction;
  }

  /**
   * Mark the resource transaction as rollback-only.
   */
  public void setRollbackOnly() {
    this.rollbackOnly = true;
  }

  /**
   * Reset the rollback-only status for this resource transaction.
   * <p>
   * Only really intended to be called after custom rollback steps which keep the
   * original resource in action, e.g. in case of a savepoint.
   */
  public void resetRollbackOnly() {
    this.rollbackOnly = false;
  }

  /**
   * Return whether the resource transaction is marked as rollback-only.
   */
  public boolean isRollbackOnly() {
    return this.rollbackOnly;
  }

  /**
   * Set the timeout for this object in seconds.
   *
   * @param seconds
   *         number of seconds until expiration
   */
  public void setTimeoutInSeconds(int seconds) {
    setTimeoutInMillis(seconds * 1000L);
  }

  /**
   * Set the timeout for this object in milliseconds.
   *
   * @param millis
   *         number of milliseconds until expiration
   */
  public void setTimeoutInMillis(long millis) {
    this.deadline = System.currentTimeMillis() + millis;
  }

  /**
   * Return whether this object has an associated timeout.
   */
  public boolean hasTimeout() {
    return (this.deadline != 0);
  }

  /**
   * Return the expiration deadline of this object.
   *
   * @return the deadline as time object
   */
  public long getDeadline() {
    return this.deadline;
  }

  /**
   * Return the time to live for this object in seconds. Rounds up eagerly, e.g.
   * 9.00001 still to 10.
   *
   * @return number of seconds until expiration
   *
   * @throws InvalidTimeoutException
   *         if the deadline has already been reached
   */
  public int getTimeToLiveInSeconds() {
    double diff = ((double) getTimeToLiveInMillis()) / 1000;
    int secs = (int) Math.ceil(diff);
    checkTransactionTimeout(secs <= 0);
    return secs;
  }

  /**
   * Return the time to live for this object in milliseconds.
   *
   * @return number of millseconds until expiration
   *
   * @throws InvalidTimeoutException
   *         if the deadline has already been reached
   */
  public long getTimeToLiveInMillis() throws InvalidTimeoutException {
    if (this.deadline == 0) {
      throw new IllegalStateException("No timeout specified for this resource holder");
    }
    long timeToLive = this.deadline - System.currentTimeMillis();
    checkTransactionTimeout(timeToLive <= 0);
    return timeToLive;
  }

  /**
   * Set the transaction rollback-only if the deadline has been reached, and throw
   * a TransactionTimedOutException.
   */
  private void checkTransactionTimeout(boolean deadlineReached) throws InvalidTimeoutException {
    if (deadlineReached) {
      setRollbackOnly();
      throw new TransactionTimedOutException("Transaction timed out: deadline was " + this.deadline);
    }
  }

  /**
   * Increase the reference count by one because the holder has been requested
   * (i.e. someone requested the resource held by it).
   */
  public void requested() {
    this.referenceCount++;
  }

  /**
   * Decrease the reference count by one because the holder has been released
   * (i.e. someone released the resource held by it).
   */
  public void released() {
    this.referenceCount--;
  }

  /**
   * Return whether there are still open references to this holder.
   */
  public boolean isOpen() {
    return referenceCount > 0;
  }

  /**
   * Clear the transactional state of this resource holder.
   */
  public void clear() {
    this.synchronizedWithTransaction = false;
    this.rollbackOnly = false;
    this.deadline = 0;
  }

  /**
   * Reset this resource holder - transactional state as well as reference count.
   */
  @Override
  public void reset() {
    clear();
    this.referenceCount = 0;
  }

  @Override
  public void unbound() {
    this.isVoid = true;
  }

  @Override
  public boolean isVoid() {
    return this.isVoid;
  }

}
