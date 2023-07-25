/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.orm.mybatis;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.dao.support.PersistenceExceptionTranslator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;

import static org.apache.ibatis.reflection.ExceptionUtil.unwrapThrowable;

/**
 * Thread safe, Infra managed, {@code SqlSession} that works with
 * Infra transaction management to ensure that that the actual
 * SqlSession used is the one associated with the current Infra
 * transaction. In addition, it manages the session life-cycle,
 * including closing, committing or rolling back the session as
 * necessary based on the Infra transaction configuration.
 * <p>
 * The template needs a SqlSessionFactory to create SqlSessions,
 * passed as a constructor argument. It also can be constructed
 * indicating the executor type to be used, if not, the default
 * executor type, defined in the session factory will be used.
 * <p>
 * This template converts MyBatis PersistenceExceptions into
 * unchecked DataAccessExceptions, using, by default, a
 * {@code MyBatisExceptionTranslator}.
 * <p>
 * Because SqlSessionTemplate is thread safe, a single instance
 * can be shared by all DAOs; there should also be a small
 * memory savings by doing this. This pattern can be used in
 * Infra configuration files as follows:
 *
 * <pre>{@code
 * <bean id="sqlSessionTemplate" class="cn.taketoday.orm.mybatis.SqlSessionTemplate">
 *   <constructor-arg ref="sqlSessionFactory" />
 * </bean>
 * }
 * </pre>
 *
 * @author Putthiphong Boonphong
 * @author Hunter Presnall
 * @author Eduardo Macarron
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SqlSessionFactory
 * @see MyBatisExceptionTranslator
 * @since 4.0
 */
public class SqlSessionTemplate implements SqlSession, DisposableBean {

  private final ExecutorType executorType;

  private final SqlSessionFactory sqlSessionFactory;

  @Nullable
  private final PersistenceExceptionTranslator exceptionTranslator;

  /**
   * Constructs a Framework managed SqlSession with the {@code SqlSessionFactory} provided as an argument.
   *
   * @param sqlSessionFactory a factory of SqlSession
   */
  public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
    this(sqlSessionFactory, sqlSessionFactory.getConfiguration().getDefaultExecutorType());
  }

  /**
   * Constructs a Framework managed SqlSession with the {@code SqlSessionFactory} provided as an argument and the given
   * {@code ExecutorType} {@code ExecutorType} cannot be changed once the {@code SqlSessionTemplate} is constructed.
   *
   * @param sqlSessionFactory a factory of SqlSession
   * @param executorType an executor type on session
   */
  public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType) {
    this(sqlSessionFactory, executorType,
            new MyBatisExceptionTranslator(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(), true));
  }

  /**
   * Constructs a Framework managed {@code SqlSession} with the given {@code SqlSessionFactory} and {@code ExecutorType}. A
   * custom {@code SQLExceptionTranslator} can be provided as an argument so any {@code PersistenceException} thrown by
   * MyBatis can be custom translated to a {@code RuntimeException} The {@code SQLExceptionTranslator} can also be null
   * and thus no exception translation will be done and MyBatis exceptions will be thrown
   *
   * @param sqlSessionFactory a factory of SqlSession
   * @param executorType an executor type on session
   * @param exceptionTranslator a translator of exception
   */
  public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType,
          @Nullable PersistenceExceptionTranslator exceptionTranslator) {

    Assert.notNull(executorType, "Property 'executorType' is required");
    Assert.notNull(sqlSessionFactory, "Property 'sqlSessionFactory' is required");

    this.executorType = executorType;
    this.sqlSessionFactory = sqlSessionFactory;
    this.exceptionTranslator = exceptionTranslator;
  }

  public SqlSessionFactory getSqlSessionFactory() {
    return this.sqlSessionFactory;
  }

  public ExecutorType getExecutorType() {
    return this.executorType;
  }

  @Nullable
  public PersistenceExceptionTranslator getPersistenceExceptionTranslator() {
    return this.exceptionTranslator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T selectOne(String statement) {
    return execute(session -> session.selectOne(statement));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T selectOne(String statement, Object parameter) {
    return execute(session -> session.selectOne(statement, parameter));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
    return execute(session -> session.selectMap(statement, mapKey));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
    return execute(session -> session.selectMap(statement, parameter, mapKey));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    return execute(session -> session.selectMap(statement, parameter, mapKey, rowBounds));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Cursor<T> selectCursor(String statement) {
    return execute(session -> session.selectCursor(statement));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Cursor<T> selectCursor(String statement, Object parameter) {
    return execute(session -> session.selectCursor(statement, parameter));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
    return execute(session -> session.selectCursor(statement, parameter, rowBounds));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <E> List<E> selectList(String statement) {
    return execute(session -> session.selectList(statement));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <E> List<E> selectList(String statement, Object parameter) {
    return execute(session -> session.selectList(statement, parameter));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    return execute(session -> session.selectList(statement, parameter, rowBounds));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void select(String statement, ResultHandler handler) {
    executeVoid(session -> session.select(statement, handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void select(String statement, Object parameter, ResultHandler handler) {
    executeVoid(session -> session.select(statement, parameter, handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    executeVoid(session -> session.select(statement, parameter, rowBounds, handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int insert(String statement) {
    return execute(session -> session.insert(statement));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int insert(String statement, Object parameter) {
    return execute(session -> session.insert(statement, parameter));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int update(String statement) {
    return execute(session -> session.update(statement));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int update(String statement, Object parameter) {
    return execute(session -> session.update(statement, parameter));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int delete(String statement) {
    return execute(session -> session.delete(statement));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int delete(String statement, Object parameter) {
    return execute(session -> session.delete(statement, parameter));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T getMapper(Class<T> type) {
    return getConfiguration().getMapper(type, this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void commit() {
    throw new UnsupportedOperationException("Manual commit is not allowed over a managed SqlSession");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void commit(boolean force) {
    throw new UnsupportedOperationException("Manual commit is not allowed over a managed SqlSession");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rollback() {
    throw new UnsupportedOperationException("Manual rollback is not allowed over a managed SqlSession");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rollback(boolean force) {
    throw new UnsupportedOperationException("Manual rollback is not allowed over a managed SqlSession");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    throw new UnsupportedOperationException("Manual close is not allowed over a managed SqlSession");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clearCache() {
    executeVoid(SqlSession::clearCache);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Configuration getConfiguration() {
    return this.sqlSessionFactory.getConfiguration();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getConnection() {
    return execute(SqlSession::getConnection);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<BatchResult> flushStatements() {
    return execute(SqlSession::flushStatements);
  }

  /**
   * Allow gently dispose bean:
   *
   * <pre>
   * {@code
   *
   * <bean id="sqlSession" class="cn.taketoday.orm.mybatis.SqlSessionTemplate">
   *  <constructor-arg index="0" ref="sqlSessionFactory" />
   * </bean>
   * }
   * </pre>
   *
   * The implementation of {@link DisposableBean} forces Framework context to use {@link DisposableBean#destroy()} method
   * instead of {@link SqlSessionTemplate#close()} to shutdown gently.
   *
   * @see SqlSessionTemplate#close()
   */
  @Override
  public void destroy() throws Exception {
    // This method forces Framework disposer to avoid call of SqlSessionTemplate.close() which gives
    // UnsupportedOperationException
  }

  private void executeVoid(Consumer<SqlSession> consumer) {
    execute(sqlSession -> {
      consumer.accept(sqlSession);
      return null;
    });
  }

  /**
   * Proxy needed to route MyBatis method calls to the proper SqlSession got
   * from Framework's Transaction Manager It also  unwraps exceptions thrown
   * by {@code Method#invoke(Object, Object...)} to pass a {@code PersistenceException} to the
   * {@code PersistenceExceptionTranslator}.
   */
  private <R> R execute(Function<SqlSession, R> function) {
    SqlSessionFactory sessionFactory = sqlSessionFactory;
    PersistenceExceptionTranslator translator = exceptionTranslator;
    SqlSession sqlSession = SqlSessionUtils.getSqlSession(sessionFactory, executorType, translator);
    try {
      R result = function.apply(sqlSession);
      if (!SqlSessionUtils.isSqlSessionTransactional(sqlSession, sessionFactory)) {
        // force commit even on non-dirty sessions because some databases require
        // a commit/rollback before calling close()
        sqlSession.commit(true);
      }
      return result;
    }
    catch (Throwable t) {
      Throwable unwrapped = unwrapThrowable(t);
      if (translator != null && unwrapped instanceof PersistenceException) {
        // release the connection to avoid a deadlock if the translator is no loaded. See issue #22
        SqlSessionUtils.closeSqlSession(sqlSession, sessionFactory);
        sqlSession = null;
        Throwable translated = translator.translateExceptionIfPossible((PersistenceException) unwrapped);
        if (translated != null) {
          unwrapped = translated;
        }
      }
      throw ExceptionUtils.sneakyThrow(unwrapped);
    }
    finally {
      if (sqlSession != null) {
        SqlSessionUtils.closeSqlSession(sqlSession, sessionFactory);
      }
    }
  }

}
