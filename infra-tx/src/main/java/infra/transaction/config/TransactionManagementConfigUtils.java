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

package infra.transaction.config;

/**
 * Configuration constants for internal sharing across subpackages.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 4.0
 */
public abstract class TransactionManagementConfigUtils {

  /**
   * The bean name of the internally managed transaction advisor (used when mode == PROXY).
   */
  public static final String TRANSACTION_ADVISOR_BEAN_NAME =
          "infra.transaction.config.internalTransactionAdvisor";

  /**
   * The bean name of the internally managed transaction aspect (used when mode == ASPECTJ).
   */
  public static final String TRANSACTION_ASPECT_BEAN_NAME =
          "infra.transaction.config.internalTransactionAspect";

  /**
   * The class name of the AspectJ transaction management aspect.
   */
  public static final String TRANSACTION_ASPECT_CLASS_NAME =
          "infra.transaction.aspectj.AnnotationTransactionAspect";

  /**
   * The name of the AspectJ transaction management @{@code Configuration} class.
   */
  public static final String TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME =
          "infra.transaction.aspectj.AspectJTransactionManagementConfiguration";

  /**
   * The bean name of the internally managed JTA transaction aspect (used when mode == ASPECTJ).
   */
  public static final String JTA_TRANSACTION_ASPECT_BEAN_NAME =
          "infra.transaction.config.internalJtaTransactionAspect";

  /**
   * The class name of the AspectJ transaction management aspect.
   */
  public static final String JTA_TRANSACTION_ASPECT_CLASS_NAME =
          "infra.transaction.aspectj.JtaAnnotationTransactionAspect";

  /**
   * The name of the AspectJ transaction management @{@code Configuration} class for JTA.
   */
  public static final String JTA_TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME =
          "infra.transaction.aspectj.AspectJJtaTransactionManagementConfiguration";

  /**
   * The bean name of the internally managed TransactionalEventListenerFactory.
   */
  public static final String TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME =
          "infra.transaction.config.internalTransactionalEventListenerFactory";

}
