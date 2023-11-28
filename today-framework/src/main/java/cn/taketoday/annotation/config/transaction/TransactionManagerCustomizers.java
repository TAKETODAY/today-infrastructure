/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.util.LambdaSafe;

/**
 * A collection of {@link PlatformTransactionManagerCustomizer}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class TransactionManagerCustomizers {

  private final List<PlatformTransactionManagerCustomizer<?>> customizers;

  public TransactionManagerCustomizers(Collection<? extends PlatformTransactionManagerCustomizer<?>> customizers) {
    this.customizers = customizers != null ? new ArrayList<>(customizers) : Collections.emptyList();
  }

  @SuppressWarnings("unchecked")
  public void customize(PlatformTransactionManager transactionManager) {
    LambdaSafe.callbacks(PlatformTransactionManagerCustomizer.class, customizers, transactionManager)
            .withLogger(TransactionManagerCustomizers.class)
            .invoke((customizer) -> customizer.customize(transactionManager));
  }

}
