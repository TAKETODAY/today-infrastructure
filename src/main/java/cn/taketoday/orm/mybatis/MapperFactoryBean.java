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

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;

/**
 * @author TODAY <br>
 * 2018-10-06 14:56
 */
public class MapperFactoryBean<T> implements FactoryBean<T> {
  private static final Logger log = LoggerFactory.getLogger(MapperFactoryBean.class);

  private SqlSession sqlSession;

  private Class<T> mapperInterface;

  @Nullable
  private String className;

  public MapperFactoryBean() { }

  public MapperFactoryBean(@Nullable String className) {
    this.className = className;
  }

  public MapperFactoryBean(Class<T> mapperInterface) {
    setMapperInterface(mapperInterface);
  }

  @Override
  public T getObject() {
    Assert.state(sqlSession != null, "No SqlSession");
    return sqlSession.getMapper(getObjectType());
  }

  @Override
  public final Class<T> getObjectType() {
    if (mapperInterface == null) {
      if (className != null) {
        mapperInterface = ClassUtils.resolveClassName(className, null);
        className = null;
      }
      Assert.state(mapperInterface != null, "Mapper interface is required");
    }
    return mapperInterface;
  }

  public SqlSession getSqlSession() {
    return sqlSession;
  }

  public void setSqlSession(SqlSession sqlSession) {
    this.sqlSession = sqlSession;
  }

  public void setMapperInterface(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  public Class<T> getMapperInterface() {
    return getObjectType();
  }

  // afterPropertiesSet
  public void applySqlSession(SqlSession sqlSession) {
    setSqlSession(sqlSession);
    Class<T> mapperInterface = getObjectType();
    Configuration configuration = sqlSession.getConfiguration();

    if (configuration.hasMapper(mapperInterface)) {
      return;
    }

    log.debug("Add Mapper: [{}] To [{}]", mapperInterface.getSimpleName(), configuration.getMapperRegistry());
    try {
      configuration.addMapper(mapperInterface);
    }
    catch (Exception e) {
      log.error("Error while adding the mapper '{}' to configuration.", mapperInterface, e);
      throw e;
    }
    finally {
      ErrorContext.instance().reset();
    }
  }

}
