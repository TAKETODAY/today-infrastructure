/*
 * Copyright 2012-present the original author or authors.
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

package infra.transaction.config;

import java.util.Collection;

import infra.beans.factory.ObjectProvider;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.stereotype.Component;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.TransactionExecutionListener;
import infra.transaction.TransactionManager;

/**
 * Auto-configuration for the customization of a {@link TransactionManager}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@ConditionalOnClass(PlatformTransactionManager.class)
@AutoConfiguration(before = TransactionAutoConfiguration.class)
@EnableConfigurationProperties(TransactionProperties.class)
public final class TransactionManagerCustomizationAutoConfiguration {

  @Component
  @ConditionalOnMissingBean
  public static TransactionManagerCustomizers platformTransactionManagerCustomizers(
          Collection<TransactionManagerCustomizer<?>> customizers) {
    return new TransactionManagerCustomizers(customizers);
  }

  @Component
  static ExecutionListenersTransactionManagerCustomizer transactionExecutionListeners(
          ObjectProvider<TransactionExecutionListener> listeners) {
    return new ExecutionListenersTransactionManagerCustomizer(listeners.orderedStream().toList());
  }

}
