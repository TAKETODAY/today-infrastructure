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

package cn.taketoday.test.context.jdbc;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.jdbc.datasource.init.ResourceDatabasePopulator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestContextAnnotationUtils;
import cn.taketoday.test.context.jdbc.SqlMergeMode.MergeMode;
import cn.taketoday.test.context.support.AbstractTestExecutionListener;
import cn.taketoday.test.context.transaction.TestContextTransactionUtils;
import cn.taketoday.test.context.transaction.TransactionalTestExecutionListener;
import cn.taketoday.test.context.util.TestContextResourceUtils;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.interceptor.DefaultTransactionAttribute;
import cn.taketoday.transaction.interceptor.TransactionAttribute;
import cn.taketoday.transaction.support.TransactionSynchronizationUtils;
import cn.taketoday.transaction.support.TransactionTemplate;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@code TestExecutionListener} that provides support for executing SQL
 * {@link Sql#scripts scripts} and inlined {@link Sql#statements statements}
 * configured via the {@link Sql @Sql} annotation.
 *
 * <p>Scripts and inlined statements will be executed {@linkplain #beforeTestMethod(TestContext) before}
 * or {@linkplain #afterTestMethod(TestContext) after} execution of the corresponding
 * {@linkplain Method test method}, depending on the configured
 * value of the {@link Sql#executionPhase executionPhase} flag.
 *
 * <p>Scripts and inlined statements will be executed without a transaction,
 * within an existing Spring-managed transaction, or within an isolated transaction,
 * depending on the configured value of {@link SqlConfig#transactionMode} and the
 * presence of a transaction manager.
 *
 * <h3>Script Resources</h3>
 * <p>For details on default script detection and how script resource locations
 * are interpreted, see {@link Sql#scripts}.
 *
 * <h3>Required Spring Beans</h3>
 * <p>A {@link PlatformTransactionManager} <em>and</em> a {@link DataSource},
 * just a {@link PlatformTransactionManager}, or just a {@link DataSource}
 * must be defined as beans in the Spring {@link ApplicationContext} for the
 * corresponding test. Consult the javadocs for {@link SqlConfig#transactionMode},
 * {@link SqlConfig#transactionManager}, {@link SqlConfig#dataSource},
 * {@link TestContextTransactionUtils#retrieveDataSource}, and
 * {@link TestContextTransactionUtils#retrieveTransactionManager} for details
 * on permissible configuration constellations and on the algorithms used to
 * locate these beans.
 *
 * @author Sam Brannen
 * @author Dmitry Semukhin
 * @see Sql
 * @see SqlConfig
 * @see SqlGroup
 * @see TestContextTransactionUtils
 * @see TransactionalTestExecutionListener
 * @see cn.taketoday.jdbc.datasource.init.ResourceDatabasePopulator
 * @see cn.taketoday.jdbc.datasource.init.ScriptUtils
 * @since 4.0
 */
public class SqlScriptsTestExecutionListener extends AbstractTestExecutionListener {

  private static final Logger logger = LoggerFactory.getLogger(SqlScriptsTestExecutionListener.class);

  /**
   * Returns {@code 5000}.
   */
  @Override
  public final int getOrder() {
    return 5000;
  }

  /**
   * Execute SQL scripts configured via {@link Sql @Sql} for the supplied
   * {@link TestContext} <em>before</em> the current test method.
   */
  @Override
  public void beforeTestMethod(TestContext testContext) {
    executeSqlScripts(testContext, Sql.ExecutionPhase.BEFORE_TEST_METHOD);
  }

  /**
   * Execute SQL scripts configured via {@link Sql @Sql} for the supplied
   * {@link TestContext} <em>after</em> the current test method.
   */
  @Override
  public void afterTestMethod(TestContext testContext) {
    executeSqlScripts(testContext, Sql.ExecutionPhase.AFTER_TEST_METHOD);
  }

  /**
   * Execute SQL scripts configured via {@link Sql @Sql} for the supplied
   * {@link TestContext} and {@link Sql.ExecutionPhase}.
   */
  private void executeSqlScripts(TestContext testContext, Sql.ExecutionPhase executionPhase) {
    Method testMethod = testContext.getTestMethod();
    Class<?> testClass = testContext.getTestClass();

    if (mergeSqlAnnotations(testContext)) {
      executeSqlScripts(getSqlAnnotationsFor(testClass), testContext, executionPhase, true);
      executeSqlScripts(getSqlAnnotationsFor(testMethod), testContext, executionPhase, false);
    }
    else {
      Set<Sql> methodLevelSqlAnnotations = getSqlAnnotationsFor(testMethod);
      if (!methodLevelSqlAnnotations.isEmpty()) {
        executeSqlScripts(methodLevelSqlAnnotations, testContext, executionPhase, false);
      }
      else {
        executeSqlScripts(getSqlAnnotationsFor(testClass), testContext, executionPhase, true);
      }
    }
  }

  /**
   * Determine if method-level {@code @Sql} annotations should be merged with
   * class-level {@code @Sql} annotations.
   */
  private boolean mergeSqlAnnotations(TestContext testContext) {
    SqlMergeMode sqlMergeMode = getSqlMergeModeFor(testContext.getTestMethod());
    if (sqlMergeMode == null) {
      sqlMergeMode = getSqlMergeModeFor(testContext.getTestClass());
    }
    return (sqlMergeMode != null && sqlMergeMode.value() == MergeMode.MERGE);
  }

  /**
   * Get the {@code @SqlMergeMode} annotation declared on the supplied class.
   */
  @Nullable
  private SqlMergeMode getSqlMergeModeFor(Class<?> clazz) {
    return TestContextAnnotationUtils.findMergedAnnotation(clazz, SqlMergeMode.class);
  }

  /**
   * Get the {@code @SqlMergeMode} annotation declared on the supplied method.
   */
  @Nullable
  private SqlMergeMode getSqlMergeModeFor(Method method) {
    return AnnotatedElementUtils.findMergedAnnotation(method, SqlMergeMode.class);
  }

  /**
   * Get the {@code @Sql} annotations declared on the supplied class.
   */
  private Set<Sql> getSqlAnnotationsFor(Class<?> clazz) {
    return TestContextAnnotationUtils.getMergedRepeatableAnnotations(clazz, Sql.class);
  }

  /**
   * Get the {@code @Sql} annotations declared on the supplied method.
   */
  private Set<Sql> getSqlAnnotationsFor(Method method) {
    return AnnotatedElementUtils.getMergedRepeatableAnnotations(method, Sql.class, SqlGroup.class);
  }

  /**
   * Execute SQL scripts for the supplied {@link Sql @Sql} annotations.
   */
  private void executeSqlScripts(
          Set<Sql> sqlAnnotations, TestContext testContext, Sql.ExecutionPhase executionPhase, boolean classLevel) {

    sqlAnnotations.forEach(sql -> executeSqlScripts(sql, executionPhase, testContext, classLevel));
  }

  /**
   * Execute the SQL scripts configured via the supplied {@link Sql @Sql}
   * annotation for the given {@link Sql.ExecutionPhase} and {@link TestContext}.
   * <p>Special care must be taken in order to properly support the configured
   * {@link SqlConfig#transactionMode}.
   *
   * @param sql the {@code @Sql} annotation to parse
   * @param executionPhase the current execution phase
   * @param testContext the current {@code TestContext}
   * @param classLevel {@code true} if {@link Sql @Sql} was declared at the class level
   */
  private void executeSqlScripts(
          Sql sql, Sql.ExecutionPhase executionPhase, TestContext testContext, boolean classLevel) {

    if (executionPhase != sql.executionPhase()) {
      return;
    }

    MergedSqlConfig mergedSqlConfig = new MergedSqlConfig(sql.config(), testContext.getTestClass());
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Processing %s for execution phase [%s] and test context %s.",
              mergedSqlConfig, executionPhase, testContext));
    }

    String[] scripts = getScripts(sql, testContext, classLevel);
    scripts = TestContextResourceUtils.convertToClasspathResourcePaths(testContext.getTestClass(), scripts);
    List<Resource> scriptResources = TestContextResourceUtils.convertToResourceList(
            testContext.getApplicationContext(), scripts);
    for (String stmt : sql.statements()) {
      if (StringUtils.hasText(stmt)) {
        stmt = stmt.trim();
        scriptResources.add(new ByteArrayResource(stmt.getBytes(), "from inlined SQL statement: " + stmt));
      }
    }

    ResourceDatabasePopulator populator = createDatabasePopulator(mergedSqlConfig);
    populator.setScripts(scriptResources.toArray(Resource.EMPTY_ARRAY));
    if (logger.isDebugEnabled()) {
      logger.debug("Executing SQL scripts: " + ObjectUtils.nullSafeToString(scriptResources));
    }

    String dsName = mergedSqlConfig.getDataSource();
    String tmName = mergedSqlConfig.getTransactionManager();
    DataSource dataSource = TestContextTransactionUtils.retrieveDataSource(testContext, dsName);
    PlatformTransactionManager txMgr = TestContextTransactionUtils.retrieveTransactionManager(testContext, tmName);
    boolean newTxRequired = (mergedSqlConfig.getTransactionMode() == SqlConfig.TransactionMode.ISOLATED);

    if (txMgr == null) {
      Assert.state(!newTxRequired, () -> String.format("Failed to execute SQL scripts for test context %s: " +
              "cannot execute SQL scripts using Transaction Mode " +
              "[%s] without a PlatformTransactionManager.", testContext, SqlConfig.TransactionMode.ISOLATED));
      Assert.state(dataSource != null, () -> String.format("Failed to execute SQL scripts for test context %s: " +
              "supply at least a DataSource or PlatformTransactionManager.", testContext));
      // Execute scripts directly against the DataSource
      populator.execute(dataSource);
    }
    else {
      DataSource dataSourceFromTxMgr = getDataSourceFromTransactionManager(txMgr);
      // Ensure user configured an appropriate DataSource/TransactionManager pair.
      if (dataSource != null && dataSourceFromTxMgr != null && !sameDataSource(dataSource, dataSourceFromTxMgr)) {
        throw new IllegalStateException(String.format("Failed to execute SQL scripts for test context %s: " +
                        "the configured DataSource [%s] (named '%s') is not the one associated with " +
                        "transaction manager [%s] (named '%s').", testContext, dataSource.getClass().getName(),
                dsName, txMgr.getClass().getName(), tmName));
      }
      if (dataSource == null) {
        dataSource = dataSourceFromTxMgr;
        Assert.state(dataSource != null, () -> String.format("Failed to execute SQL scripts for " +
                        "test context %s: could not obtain DataSource from transaction manager [%s] (named '%s').",
                testContext, txMgr.getClass().getName(), tmName));
      }
      final DataSource finalDataSource = dataSource;
      int propagation = (newTxRequired ? TransactionDefinition.PROPAGATION_REQUIRES_NEW :
                         TransactionDefinition.PROPAGATION_REQUIRED);
      TransactionAttribute txAttr = TestContextTransactionUtils.createDelegatingTransactionAttribute(
              testContext, new DefaultTransactionAttribute(propagation));
      new TransactionTemplate(txMgr, txAttr).executeWithoutResult(s -> populator.execute(finalDataSource));
    }
  }

  @NonNull
  private ResourceDatabasePopulator createDatabasePopulator(MergedSqlConfig mergedSqlConfig) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setSqlScriptEncoding(mergedSqlConfig.getEncoding());
    populator.setSeparator(mergedSqlConfig.getSeparator());
    populator.setCommentPrefixes(mergedSqlConfig.getCommentPrefixes());
    populator.setBlockCommentStartDelimiter(mergedSqlConfig.getBlockCommentStartDelimiter());
    populator.setBlockCommentEndDelimiter(mergedSqlConfig.getBlockCommentEndDelimiter());
    populator.setContinueOnError(mergedSqlConfig.getErrorMode() == SqlConfig.ErrorMode.CONTINUE_ON_ERROR);
    populator.setIgnoreFailedDrops(mergedSqlConfig.getErrorMode() == SqlConfig.ErrorMode.IGNORE_FAILED_DROPS);
    return populator;
  }

  /**
   * Determine if the two data sources are effectively the same, unwrapping
   * proxies as necessary to compare the target instances.
   *
   * @see TransactionSynchronizationUtils#unwrapResourceIfNecessary(Object)
   * @since 4.0
   */
  private static boolean sameDataSource(DataSource ds1, DataSource ds2) {
    return TransactionSynchronizationUtils.unwrapResourceIfNecessary(ds1)
            .equals(TransactionSynchronizationUtils.unwrapResourceIfNecessary(ds2));
  }

  @Nullable
  private DataSource getDataSourceFromTransactionManager(PlatformTransactionManager transactionManager) {
    try {
      Method getDataSourceMethod = transactionManager.getClass().getMethod("getDataSource");
      Object obj = ReflectionUtils.invokeMethod(getDataSourceMethod, transactionManager);
      if (obj instanceof DataSource) {
        return (DataSource) obj;
      }
    }
    catch (Exception ex) {
      // ignore
    }
    return null;
  }

  private String[] getScripts(Sql sql, TestContext testContext, boolean classLevel) {
    String[] scripts = sql.scripts();
    if (ObjectUtils.isEmpty(scripts) && ObjectUtils.isEmpty(sql.statements())) {
      scripts = new String[] { detectDefaultScript(testContext, classLevel) };
    }
    return scripts;
  }

  /**
   * Detect a default SQL script by implementing the algorithm defined in
   * {@link Sql#scripts}.
   */
  private String detectDefaultScript(TestContext testContext, boolean classLevel) {
    Class<?> clazz = testContext.getTestClass();
    Method method = testContext.getTestMethod();
    String elementType = (classLevel ? "class" : "method");
    String elementName = (classLevel ? clazz.getName() : method.toString());

    String resourcePath = ClassUtils.convertClassNameToResourcePath(clazz.getName());
    if (!classLevel) {
      resourcePath += "." + method.getName();
    }
    resourcePath += ".sql";

    String prefixedResourcePath = ResourceLoader.CLASSPATH_URL_PREFIX + resourcePath;
    ClassPathResource classPathResource = new ClassPathResource(resourcePath);

    if (classPathResource.exists()) {
      if (logger.isInfoEnabled()) {
        logger.info(String.format("Detected default SQL script \"%s\" for test %s [%s]",
                prefixedResourcePath, elementType, elementName));
      }
      return prefixedResourcePath;
    }
    else {
      String msg = String.format("Could not detect default SQL script for test %s [%s]: " +
              "%s does not exist. Either declare statements or scripts via @Sql or make the " +
              "default SQL script available.", elementType, elementName, classPathResource);
      logger.error(msg);
      throw new IllegalStateException(msg);
    }
  }

}
