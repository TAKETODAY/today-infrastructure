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

package cn.taketoday.transaction.config;

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
          "cn.taketoday.transaction.config.internalTransactionAdvisor";

  /**
   * The bean name of the internally managed transaction aspect (used when mode == ASPECTJ).
   */
  public static final String TRANSACTION_ASPECT_BEAN_NAME =
          "cn.taketoday.transaction.config.internalTransactionAspect";

  /**
   * The class name of the AspectJ transaction management aspect.
   */
  public static final String TRANSACTION_ASPECT_CLASS_NAME =
          "cn.taketoday.transaction.aspectj.AnnotationTransactionAspect";

  /**
   * The name of the AspectJ transaction management @{@code Configuration} class.
   */
  public static final String TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME =
          "cn.taketoday.transaction.aspectj.AspectJTransactionManagementConfiguration";

  /**
   * The bean name of the internally managed JTA transaction aspect (used when mode == ASPECTJ).
   */
  public static final String JTA_TRANSACTION_ASPECT_BEAN_NAME =
          "cn.taketoday.transaction.config.internalJtaTransactionAspect";

  /**
   * The class name of the AspectJ transaction management aspect.
   */
  public static final String JTA_TRANSACTION_ASPECT_CLASS_NAME =
          "cn.taketoday.transaction.aspectj.JtaAnnotationTransactionAspect";

  /**
   * The name of the AspectJ transaction management @{@code Configuration} class for JTA.
   */
  public static final String JTA_TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME =
          "cn.taketoday.transaction.aspectj.AspectJJtaTransactionManagementConfiguration";

  /**
   * The bean name of the internally managed TransactionalEventListenerFactory.
   */
  public static final String TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME =
          "cn.taketoday.transaction.config.internalTransactionalEventListenerFactory";

}
