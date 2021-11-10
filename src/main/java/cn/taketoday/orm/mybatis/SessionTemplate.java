/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.orm.mybatis;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.DisposableBean;
import cn.taketoday.lang.Autowired;
import cn.taketoday.transaction.SynchronizationManager;
import cn.taketoday.transaction.SynchronizationManager.SynchronizationMetaData;
import cn.taketoday.util.ClassUtils;
import lombok.Getter;

/**
 * @author TODAY <br>
 * 2018-10-06 14:36
 */
@Getter
public class SessionTemplate implements SqlSession, DisposableBean {

  protected final SqlSession proxy;
  protected final Configuration configuration;

  @Autowired
  public SessionTemplate(SqlSessionFactory sqlSessionFactory) {
    this.configuration = sqlSessionFactory.getConfiguration();
    final ExecutorType executorType = configuration.getDefaultExecutorType();
    this.proxy = (SqlSession) Proxy.newProxyInstance(
            ClassUtils.getDefaultClassLoader(),
            new Class[] { SqlSession.class },
            new SqlSessionInterceptor(executorType, sqlSessionFactory));
  }

  @Override
  public <T> T selectOne(String statement) {
    return this.proxy.selectOne(statement);
  }

  @Override
  public <T> T selectOne(String statement, Object parameter) {
    return this.proxy.selectOne(statement, parameter);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
    return this.proxy.selectMap(statement, mapKey);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
    return this.proxy.selectMap(statement, parameter, mapKey);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    return this.proxy.selectMap(statement, parameter, mapKey, rowBounds);
  }

  @Override
  public <T> Cursor<T> selectCursor(String statement) {
    return this.proxy.selectCursor(statement);
  }

  @Override
  public <T> Cursor<T> selectCursor(String statement, Object parameter) {
    return this.proxy.selectCursor(statement, parameter);
  }

  @Override
  public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
    return this.proxy.selectCursor(statement, parameter, rowBounds);
  }

  @Override
  public <E> List<E> selectList(String statement) {
    return this.proxy.selectList(statement);
  }

  @Override
  public <E> List<E> selectList(String statement, Object parameter) {
    return this.proxy.selectList(statement, parameter);
  }

  @Override
  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    return this.proxy.<E>selectList(statement, parameter, rowBounds);
  }

  @Override
  public void select(String statement, @SuppressWarnings("rawtypes") ResultHandler handler) {
    this.proxy.select(statement, handler);
  }

  @Override
  public void select(String statement, Object parameter, @SuppressWarnings("rawtypes") ResultHandler handler) {
    this.proxy.select(statement, parameter, handler);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void select(String statement,
                     Object parameter,
                     RowBounds rowBounds,
                     ResultHandler handler) {
    this.proxy.select(statement, parameter, rowBounds, handler);
  }

  @Override
  public int insert(String statement) {
    return this.proxy.insert(statement);
  }

  @Override
  public int insert(String statement, Object parameter) {
    return this.proxy.insert(statement, parameter);
  }

  @Override
  public int update(String statement) {
    return this.proxy.update(statement);
  }

  @Override
  public int update(String statement, Object parameter) {
    return this.proxy.update(statement, parameter);
  }

  @Override
  public int delete(String statement) {
    return this.proxy.delete(statement);
  }

  @Override
  public int delete(String statement, Object parameter) {
    return this.proxy.delete(statement, parameter);
  }

  @Override
  public <T> T getMapper(Class<T> type) {
    return configuration.getMapper(type, this);
  }

  @Override
  public void commit() {
    throw new UnsupportedOperationException("Manual commit is not allowed over a TODAY managed SqlSession");
  }

  @Override
  public void commit(boolean force) {
    throw new UnsupportedOperationException("Manual commit is not allowed over a TODAY managed SqlSession");
  }

  @Override
  public void rollback() {
    throw new UnsupportedOperationException("Manual rollback is not allowed over a TODAY managed SqlSession");
  }

  @Override
  public void rollback(boolean force) {
    throw new UnsupportedOperationException("Manual rollback is not allowed over a TODAY managed SqlSession");
  }

  @Override
  public void close() {
    throw new UnsupportedOperationException("Manual close is not allowed over a TODAY managed SqlSession");
  }

  @Override
  public void clearCache() {
    this.proxy.clearCache();
  }

  @Override
  public Configuration getConfiguration() {
    return this.configuration;
  }

  @Override
  public Connection getConnection() {
    return this.proxy.getConnection();
  }

  @Override
  public List<BatchResult> flushStatements() {
    return this.proxy.flushStatements();
  }

  @Override
  public void destroy() throws Exception {

  }

  private record SqlSessionInterceptor(ExecutorType executorType, SqlSessionFactory sqlSessionFactory)
          implements InvocationHandler {

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      final SqlSessionFactory factory = this.sqlSessionFactory;

      final SynchronizationMetaData metaData = SynchronizationManager.getMetaData();
      final SqlSession session = SqlSessionUtils.getSqlSession(metaData, factory, executorType);

      try {
        return method.invoke(session, args);
      }
      finally {
        if (session != null) {
          SqlSessionUtils.closeSqlSession(metaData, session, factory);
        }
      }
    }
  }

}
