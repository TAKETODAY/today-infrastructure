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

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;

import cn.taketoday.dao.support.PersistenceExceptionTranslator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.support.ResourceHolderSupport;

/**
 * Used to keep current {@code SqlSession} in {@code TransactionSynchronizationManager}.
 * The {@code SqlSessionFactory} that created that {@code SqlSession} is used as a key.
 * {@code ExecutorType} is also kept to be able to check if the user is trying to change
 * it during a TX (that is not allowed) and throw a Exception in that case.
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class SqlSessionHolder extends ResourceHolderSupport {

  private final SqlSession sqlSession;

  private final ExecutorType executorType;

  @Nullable
  private final PersistenceExceptionTranslator exceptionTranslator;

  /**
   * Creates a new holder instance.
   *
   * @param sqlSession the {@code SqlSession} has to be hold.
   * @param executorType the {@code ExecutorType} has to be hold.
   * @param exceptionTranslator the {@code PersistenceExceptionTranslator} has to be hold.
   */
  public SqlSessionHolder(SqlSession sqlSession, ExecutorType executorType,
          @Nullable PersistenceExceptionTranslator exceptionTranslator) {
    Assert.notNull(sqlSession, "SqlSession is required");
    Assert.notNull(executorType, "ExecutorType is required");

    this.sqlSession = sqlSession;
    this.executorType = executorType;
    this.exceptionTranslator = exceptionTranslator;
  }

  public SqlSession getSqlSession() {
    return sqlSession;
  }

  public ExecutorType getExecutorType() {
    return executorType;
  }

  @Nullable
  public PersistenceExceptionTranslator getPersistenceExceptionTranslator() {
    return exceptionTranslator;
  }

}
