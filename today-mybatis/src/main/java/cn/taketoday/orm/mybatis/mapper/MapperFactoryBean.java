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

package cn.taketoday.orm.mybatis.mapper;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.orm.mybatis.SqlSessionTemplate;
import cn.taketoday.orm.mybatis.support.SqlSessionDaoSupport;

/**
 * BeanFactory that enables injection of MyBatis mapper interfaces. It can
 * be set up with a SqlSessionFactory or a pre-configured SqlSessionTemplate.
 * <p>
 * Sample configuration:
 *
 * <pre>
 * {@code
 *   <bean id="baseMapper" class="mapper.MapperFactoryBean" abstract="true" lazy-init="true">
 *     <property name="sqlSessionFactory" ref="sqlSessionFactory" />
 *   </bean>
 *
 *   <bean id="oneMapper" parent="baseMapper">
 *     <property name="mapperInterface" value="my.package.MyMapperInterface" />
 *   </bean>
 *
 *   <bean id="anotherMapper" parent="baseMapper">
 *     <property name="mapperInterface" value="my.package.MyAnotherMapperInterface" />
 *   </bean>
 * }
 * </pre>
 * <p>
 * Note that this factory can only inject <em>interfaces</em>, not concrete classes.
 *
 * @author Eduardo Macarron
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SqlSessionTemplate
 * @since 4.0
 */
public class MapperFactoryBean<T> extends SqlSessionDaoSupport implements FactoryBean<T> {
  private static final Logger log = LoggerFactory.getLogger(MapperFactoryBean.class);

  private Class<T> mapperInterface;

  private boolean addToConfig = true;

  public MapperFactoryBean() { }

  public MapperFactoryBean(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void checkDaoConfig() {
    super.checkDaoConfig();
    Assert.notNull(mapperInterface, "Property 'mapperInterface' is required");

    Configuration configuration = getSqlSession().getConfiguration();
    if (addToConfig && !configuration.hasMapper(mapperInterface)) {
      try {
        configuration.addMapper(mapperInterface);
      }
      catch (Exception e) {
        log.error("Error while adding the mapper '{}' to configuration.", mapperInterface, e);
        throw new IllegalArgumentException(e);
      }
      finally {
        ErrorContext.instance().reset();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getObject() throws Exception {
    return getSqlSession().getMapper(mapperInterface);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<T> getObjectType() {
    return mapperInterface;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSingleton() {
    return true;
  }

  // ------------- mutators --------------

  /**
   * Sets the mapper interface of the MyBatis mapper
   *
   * @param mapperInterface class of the interface
   */
  public void setMapperInterface(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  /**
   * Return the mapper interface of the MyBatis mapper
   *
   * @return class of the interface
   */
  public Class<T> getMapperInterface() {
    return mapperInterface;
  }

  /**
   * If addToConfig is false the mapper will not be added to MyBatis. This means it must have been included in
   * mybatis-config.xml.
   * <p>
   * If it is true, the mapper will be added to MyBatis in the case it is not already registered.
   * <p>
   * By default addToConfig is true.
   *
   * @param addToConfig a flag that whether add mapper to MyBatis or not
   */
  public void setAddToConfig(boolean addToConfig) {
    this.addToConfig = addToConfig;
  }

  /**
   * Return the flag for addition into MyBatis config.
   *
   * @return true if the mapper will be added to MyBatis in the case it is not already registered.
   */
  public boolean isAddToConfig() {
    return addToConfig;
  }
}
