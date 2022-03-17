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

package cn.taketoday.test.context.transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.annotation.TransactionManagementConfigurer;
import cn.taketoday.transaction.interceptor.DelegatingTransactionAttribute;
import cn.taketoday.transaction.interceptor.TransactionAttribute;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * Utility methods for working with transactions and data access related beans
 * within the <em>Spring TestContext Framework</em>.
 *
 * <p>Mainly for internal use within the framework.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class TestContextTransactionUtils {

  /**
   * Default bean name for a {@link DataSource}: {@code "dataSource"}.
   */
  public static final String DEFAULT_DATA_SOURCE_NAME = "dataSource";

  /**
   * Default bean name for a {@link PlatformTransactionManager}:
   * {@code "transactionManager"}.
   */
  public static final String DEFAULT_TRANSACTION_MANAGER_NAME = "transactionManager";

  private static final Log logger = LogFactory.getLog(TestContextTransactionUtils.class);

  /**
   * Retrieve the {@link DataSource} to use for the supplied {@linkplain TestContext
   * test context}.
   * <p>The following algorithm is used to retrieve the {@code DataSource} from
   * the {@link cn.taketoday.context.ApplicationContext ApplicationContext}
   * of the supplied test context:
   * <ol>
   * <li>Look up the {@code DataSource} by type and name, if the supplied
   * {@code name} is non-empty, throwing a {@link BeansException} if the named
   * {@code DataSource} does not exist.
   * <li>Attempt to look up the single {@code DataSource} by type.
   * <li>Attempt to look up the <em>primary</em> {@code DataSource} by type.
   * <li>Attempt to look up the {@code DataSource} by type and the
   * {@linkplain #DEFAULT_DATA_SOURCE_NAME default data source name}.
   * </ol>
   *
   * @param testContext the test context for which the {@code DataSource}
   * should be retrieved; never {@code null}
   * @param name the name of the {@code DataSource} to retrieve
   * (may be {@code null} or <em>empty</em>)
   * @return the {@code DataSource} to use, or {@code null} if not found
   * @throws BeansException if an error occurs while retrieving an explicitly
   * named {@code DataSource}
   */
  @Nullable
  public static DataSource retrieveDataSource(TestContext testContext, @Nullable String name) {
    Assert.notNull(testContext, "TestContext must not be null");
    BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();

    try {
      // Look up by type and explicit name
      if (StringUtils.hasText(name)) {
        return bf.getBean(name, DataSource.class);
      }
    }
    catch (BeansException ex) {
      logger.error(String.format("Failed to retrieve DataSource named '%s' for test context %s",
              name, testContext), ex);
      throw ex;
    }

    try {
      // Look up single bean by type
      Map<String, DataSource> dataSources =
              BeanFactoryUtils.beansOfTypeIncludingAncestors(bf, DataSource.class);
      if (dataSources.size() == 1) {
        return dataSources.values().iterator().next();
      }

      try {
        // look up single bean by type, with support for 'primary' beans
        return bf.getBean(DataSource.class);
      }
      catch (BeansException ex) {
        logBeansException(testContext, ex, PlatformTransactionManager.class);
      }

      // look up by type and default name
      return bf.getBean(DEFAULT_DATA_SOURCE_NAME, DataSource.class);
    }
    catch (BeansException ex) {
      logBeansException(testContext, ex, DataSource.class);
      return null;
    }
  }

  /**
   * Retrieve the {@linkplain PlatformTransactionManager transaction manager}
   * to use for the supplied {@linkplain TestContext test context}.
   * <p>The following algorithm is used to retrieve the transaction manager
   * from the {@link cn.taketoday.context.ApplicationContext ApplicationContext}
   * of the supplied test context:
   * <ol>
   * <li>Look up the transaction manager by type and explicit name, if the supplied
   * {@code name} is non-empty, throwing a {@link BeansException} if the named
   * transaction manager does not exist.
   * <li>Attempt to look up the transaction manager via a
   * {@link TransactionManagementConfigurer}, if present.
   * <li>Attempt to look up the single transaction manager by type.
   * <li>Attempt to look up the <em>primary</em> transaction manager by type.
   * <li>Attempt to look up the transaction manager by type and the
   * {@linkplain #DEFAULT_TRANSACTION_MANAGER_NAME default transaction manager
   * name}.
   * </ol>
   *
   * @param testContext the test context for which the transaction manager
   * should be retrieved; never {@code null}
   * @param name the name of the transaction manager to retrieve
   * (may be {@code null} or <em>empty</em>)
   * @return the transaction manager to use, or {@code null} if not found
   * @throws BeansException if an error occurs while retrieving an explicitly
   * named transaction manager
   * @throws IllegalStateException if more than one TransactionManagementConfigurer
   * exists in the ApplicationContext
   */
  @Nullable
  public static PlatformTransactionManager retrieveTransactionManager(TestContext testContext, @Nullable String name) {
    Assert.notNull(testContext, "TestContext must not be null");
    BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();

    try {
      // Look up by type and explicit name
      if (StringUtils.hasText(name)) {
        return bf.getBean(name, PlatformTransactionManager.class);
      }
    }
    catch (BeansException ex) {
      logger.error(String.format("Failed to retrieve transaction manager named '%s' for test context %s",
              name, testContext), ex);
      throw ex;
    }

    try {
      // Look up single TransactionManagementConfigurer
      Map<String, TransactionManagementConfigurer> configurers =
              BeanFactoryUtils.beansOfTypeIncludingAncestors(bf, TransactionManagementConfigurer.class);
      Assert.state(configurers.size() <= 1,
              "Only one TransactionManagementConfigurer may exist in the ApplicationContext");
      if (configurers.size() == 1) {
        return configurers.values().iterator().next().annotationDrivenTransactionManager();
      }

      // Look up single bean by type
      Map<String, PlatformTransactionManager> txMgrs =
              BeanFactoryUtils.beansOfTypeIncludingAncestors(bf, PlatformTransactionManager.class);
      if (txMgrs.size() == 1) {
        return txMgrs.values().iterator().next();
      }

      try {
        // Look up single bean by type, with support for 'primary' beans
        return bf.getBean(PlatformTransactionManager.class);
      }
      catch (BeansException ex) {
        logBeansException(testContext, ex, PlatformTransactionManager.class);
      }

      // look up by type and default name
      return bf.getBean(DEFAULT_TRANSACTION_MANAGER_NAME, PlatformTransactionManager.class);
    }
    catch (BeansException ex) {
      logBeansException(testContext, ex, PlatformTransactionManager.class);
      return null;
    }
  }

  private static void logBeansException(TestContext testContext, BeansException ex, Class<?> beanType) {
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Caught exception while retrieving %s for test context %s",
              beanType.getSimpleName(), testContext), ex);
    }
  }

  /**
   * Create a delegating {@link TransactionAttribute} for the supplied target
   * {@link TransactionAttribute} and {@link TestContext}, using the names of
   * the test class and test method to build the name of the transaction.
   *
   * @param testContext the {@code TestContext} upon which to base the name
   * @param targetAttribute the {@code TransactionAttribute} to delegate to
   * @return the delegating {@code TransactionAttribute}
   */
  public static TransactionAttribute createDelegatingTransactionAttribute(
          TestContext testContext, TransactionAttribute targetAttribute) {

    Assert.notNull(testContext, "TestContext must not be null");
    Assert.notNull(targetAttribute, "Target TransactionAttribute must not be null");
    return new TestContextTransactionAttribute(targetAttribute, testContext);
  }

  @SuppressWarnings("serial")
  private static class TestContextTransactionAttribute extends DelegatingTransactionAttribute {

    private final String name;

    public TestContextTransactionAttribute(TransactionAttribute targetAttribute, TestContext testContext) {
      super(targetAttribute);
      this.name = ClassUtils.getQualifiedMethodName(testContext.getTestMethod(), testContext.getTestClass());
    }

    @Override
    @Nullable
    public String getName() {
      return this.name;
    }
  }

}
