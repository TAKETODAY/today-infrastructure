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

package cn.taketoday.annotation.config.transaction;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.format.annotation.DurationUnit;
import cn.taketoday.transaction.support.AbstractPlatformTransactionManager;

/**
 * Configuration properties that can be applied to an
 * {@link AbstractPlatformTransactionManager}.
 *
 * @author Kazuki Shimizu
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties(prefix = "transaction")
public class TransactionProperties implements PlatformTransactionManagerCustomizer<AbstractPlatformTransactionManager> {

  /**
   * Default transaction timeout. If a duration suffix is not specified, seconds will be
   * used.
   */
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration defaultTimeout;

  /**
   * Whether to roll back on commit failures.
   */
  private Boolean rollbackOnCommitFailure;

  public Duration getDefaultTimeout() {
    return this.defaultTimeout;
  }

  public void setDefaultTimeout(Duration defaultTimeout) {
    this.defaultTimeout = defaultTimeout;
  }

  public Boolean getRollbackOnCommitFailure() {
    return this.rollbackOnCommitFailure;
  }

  public void setRollbackOnCommitFailure(Boolean rollbackOnCommitFailure) {
    this.rollbackOnCommitFailure = rollbackOnCommitFailure;
  }

  @Override
  public void customize(AbstractPlatformTransactionManager transactionManager) {
    if (this.defaultTimeout != null) {
      transactionManager.setDefaultTimeout((int) this.defaultTimeout.getSeconds());
    }
    if (this.rollbackOnCommitFailure != null) {
      transactionManager.setRollbackOnCommitFailure(this.rollbackOnCommitFailure);
    }
  }

}
