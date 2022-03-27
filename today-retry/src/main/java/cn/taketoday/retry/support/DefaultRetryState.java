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
package cn.taketoday.retry.support;

import cn.taketoday.classify.Classifier;
import cn.taketoday.retry.RecoveryCallback;
import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryOperations;
import cn.taketoday.retry.RetryState;

/**
 * @author Dave Syer
 * @since 4.0
 */
public class DefaultRetryState implements RetryState {

  final private Object key;

  final private boolean forceRefresh;

  final private Classifier<? super Throwable, Boolean> rollbackClassifier;

  /**
   * Create a {@link DefaultRetryState} representing the state for a new retry attempt.
   *
   * @param key the key for the state to allow this retry attempt to be recognised
   * @param forceRefresh true if the attempt is known to be a brand new state (could not
   * have previously failed)
   * @param rollbackClassifier the rollback classifier to set. The rollback classifier
   * answers true if the exception provided should cause a rollback.
   * @see RetryOperations#execute(RetryCallback, RetryState)
   * @see RetryOperations#execute(RetryCallback, RecoveryCallback, RetryState)
   */
  public DefaultRetryState(Object key, boolean forceRefresh,
          Classifier<? super Throwable, Boolean> rollbackClassifier) {
    this.key = key;
    this.forceRefresh = forceRefresh;
    this.rollbackClassifier = rollbackClassifier;
  }

  /**
   * Defaults the force refresh flag to false.
   *
   * @param key the key
   * @param rollbackClassifier the rollback {@link Classifier}
   * @see DefaultRetryState#DefaultRetryState(Object, boolean, Classifier)
   */
  public DefaultRetryState(Object key, Classifier<? super Throwable, Boolean> rollbackClassifier) {
    this(key, false, rollbackClassifier);
  }

  /**
   * Defaults the rollback classifier to null.
   *
   * @param key the key
   * @param forceRefresh whether to force a refresh
   * @see DefaultRetryState#DefaultRetryState(Object, boolean, Classifier)
   */
  public DefaultRetryState(Object key, boolean forceRefresh) {
    this(key, forceRefresh, null);
  }

  /**
   * Defaults the force refresh flag (to false) and the rollback classifier (to null).
   *
   * @param key the key to use
   * @see DefaultRetryState#DefaultRetryState(Object, boolean, Classifier)
   */
  public DefaultRetryState(Object key) {
    this(key, false, null);
  }

  /*
   * (non-Javadoc)
   *
   * @see cn.taketoday.batch.retry.IRetryState#getKey()
   */
  public Object getKey() {
    return key;
  }

  /*
   * (non-Javadoc)
   *
   * @see cn.taketoday.batch.retry.IRetryState#isForceRefresh()
   */
  public boolean isForceRefresh() {
    return forceRefresh;
  }

  /*
   * (non-Javadoc)
   *
   * @see cn.taketoday.batch.retry.RetryState#rollbackFor(java.lang.Throwable )
   */
  public boolean rollbackFor(Throwable exception) {
    if (rollbackClassifier == null) {
      return true;
    }
    return rollbackClassifier.classify(exception);
  }

  @Override
  public String toString() {
    return String.format("[%s: key=%s, forceRefresh=%b]", getClass().getSimpleName(), key, forceRefresh);
  }

}
