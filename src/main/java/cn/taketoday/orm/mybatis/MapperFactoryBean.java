/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import javax.annotation.PostConstruct;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;

/**
 * @author TODAY <br>
 * 2018-10-06 14:56
 */
public class MapperFactoryBean<T> implements FactoryBean<T> {

  private SqlSession sqlSession;

  private Class<T> mapperInterface;

  public MapperFactoryBean() {}

  public MapperFactoryBean(Class<T> mapperInterface) {
    this.setMapperInterface(mapperInterface);
  }

  @Override
  public T getBean() {
    return getSqlSession().getMapper(getBeanClass());
  }

  @Override
  public final Class<T> getBeanClass() {
    if (mapperInterface == null) {
      throw new ConfigurationException("Mapper interface must not be null");
    }
    return mapperInterface;
  }

  public SqlSession getSqlSession() {
    if (sqlSession == null) {
      throw new ConfigurationException("Sql Session must not be null");
    }
    return sqlSession;
  }

  public void setSqlSession(SqlSession sqlSession) {
    this.sqlSession = sqlSession;
  }

  public void setMapperInterface(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  @PostConstruct
  public void afterPropertiesSet(SqlSession sqlSession) {
    setSqlSession(sqlSession);
    final Class<T> mapperInterface = getBeanClass();
    final Configuration configuration = sqlSession.getConfiguration();

    if (configuration.hasMapper(mapperInterface)) {
      return;
    }

    final Logger log = LoggerFactory.getLogger(mapperInterface);
    log.debug("Add Mapper: [{}] To [{}]", mapperInterface.getSimpleName(), configuration.getMapperRegistry());
    try {
      configuration.addMapper(mapperInterface);
    }
    catch (Exception e) {
      log.error("Error while adding the mapper '" + mapperInterface + "' to configuration.", e);
      throw e;
    }
    finally {
      ErrorContext.instance().reset();
    }
  }

}
