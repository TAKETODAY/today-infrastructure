/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */
package infra.orm.mybatis.type;

import org.apache.ibatis.session.SqlSessionFactory;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.orm.mybatis.mapper.MapperFactoryBean;

public class DummyMapperFactoryBean<T> extends MapperFactoryBean<T> {

  public DummyMapperFactoryBean() {
    super();
  }

  public DummyMapperFactoryBean(Class<T> mapperInterface) {
    super(mapperInterface);
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(DummyMapperFactoryBean.class);

  private static final AtomicInteger mapperInstanceCount = new AtomicInteger(0);

  @Override
  protected void checkDaoConfig() {
    super.checkDaoConfig();
    // make something more
    if (isAddToConfig()) {
      LOGGER.debug("register mapper for interface : " + getMapperInterface());
    }
  }

  @Override
  public T getObject() throws Exception {
    MapperFactoryBean<T> mapperFactoryBean = new MapperFactoryBean<>();
    mapperFactoryBean.setMapperInterface(getMapperInterface());
    mapperFactoryBean.setAddToConfig(isAddToConfig());
    mapperFactoryBean.setSqlSessionFactory(getCustomSessionFactoryForClass());
    T object = mapperFactoryBean.getObject();
    mapperInstanceCount.incrementAndGet();
    return object;
  }

  private SqlSessionFactory getCustomSessionFactoryForClass() {
    // can for example read a custom annotation to set a custom sqlSessionFactory

    // just a dummy implementation example
    return (SqlSessionFactory) Proxy.newProxyInstance(SqlSessionFactory.class.getClassLoader(),
            new Class[] { SqlSessionFactory.class }, (proxy, method, args) -> {
              if ("getConfiguration".equals(method.getName())) {
                return getSqlSession().getConfiguration();
              }
              // dummy
              return null;
            });
  }

  public static int getMapperCount() {
    return mapperInstanceCount.get();
  }

  public static void clear() {
    mapperInstanceCount.set(0);
  }

}
