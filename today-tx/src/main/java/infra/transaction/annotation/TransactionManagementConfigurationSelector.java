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

package infra.transaction.annotation;

import infra.context.annotation.AdviceMode;
import infra.context.annotation.AdviceModeImportSelector;
import infra.context.annotation.AutoProxyRegistrar;
import infra.transaction.config.TransactionManagementConfigUtils;
import infra.util.ClassUtils;

/**
 * Selects which implementation of {@link AbstractTransactionManagementConfiguration}
 * should be used based on the value of {@link EnableTransactionManagement#mode} on the
 * importing {@code @Configuration} class.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableTransactionManagement
 * @see ProxyTransactionManagementConfiguration
 * @see TransactionManagementConfigUtils#TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME
 * @see TransactionManagementConfigUtils#JTA_TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME
 * @since 4.0
 */
public class TransactionManagementConfigurationSelector
        extends AdviceModeImportSelector<EnableTransactionManagement> {

  /**
   * Returns {@link ProxyTransactionManagementConfiguration} or
   * {@code AspectJ(Jta)TransactionManagementConfiguration} for {@code PROXY}
   * and {@code ASPECTJ} values of {@link EnableTransactionManagement#mode()},
   * respectively.
   */
  @Override
  protected String[] selectImports(AdviceMode adviceMode) {
    return switch (adviceMode) {
      case PROXY -> new String[] {
              AutoProxyRegistrar.class.getName(),
              ProxyTransactionManagementConfiguration.class.getName()
      };
      case ASPECTJ -> new String[] { determineTransactionAspectClass() };
    };
  }

  private String determineTransactionAspectClass() {
    return ClassUtils.isPresent("jakarta.transaction.Transactional", getClass().getClassLoader())
           ? TransactionManagementConfigUtils.JTA_TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME
           : TransactionManagementConfigUtils.TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME;
  }

}
