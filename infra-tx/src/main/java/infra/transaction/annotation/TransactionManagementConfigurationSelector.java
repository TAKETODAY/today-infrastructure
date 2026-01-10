/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
