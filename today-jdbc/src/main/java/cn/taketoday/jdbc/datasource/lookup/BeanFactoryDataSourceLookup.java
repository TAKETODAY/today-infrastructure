/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.datasource.lookup;

import javax.sql.DataSource;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link DataSourceLookup} implementation based on a Framework {@link BeanFactory}.
 *
 * <p>Will lookup Framework managed beans identified by bean name,
 * expecting them to be of type {@code javax.sql.DataSource}.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @see cn.taketoday.beans.factory.BeanFactory
 * @since 4.0
 */
public class BeanFactoryDataSourceLookup implements DataSourceLookup, BeanFactoryAware {

  @Nullable
  private BeanFactory beanFactory;

  /**
   * Create a new instance of the {@link BeanFactoryDataSourceLookup} class.
   * <p>The BeanFactory to access must be set via {@code setBeanFactory}.
   *
   * @see #setBeanFactory
   */
  public BeanFactoryDataSourceLookup() { }

  /**
   * Create a new instance of the {@link BeanFactoryDataSourceLookup} class.
   * <p>Use of this constructor is redundant if this object is being created
   * by a Framework IoC container, as the supplied {@link BeanFactory} will be
   * replaced by the {@link BeanFactory} that creates it (c.f. the
   * {@link BeanFactoryAware} contract). So only use this constructor if you
   * are using this class outside the context of a Framework IoC container.
   *
   * @param beanFactory the bean factory to be used to lookup {@link DataSource DataSources}
   */
  public BeanFactoryDataSourceLookup(BeanFactory beanFactory) {
    Assert.notNull(beanFactory, "BeanFactory is required");
    this.beanFactory = beanFactory;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException {
    Assert.state(this.beanFactory != null, "BeanFactory is required");
    try {
      return beanFactory.getBean(dataSourceName, DataSource.class);
    }
    catch (BeansException ex) {
      throw new DataSourceLookupFailureException(
              "Failed to look up DataSource bean with name '" + dataSourceName + "'", ex);
    }
  }

}
