/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.datasource.lookup;

import org.jspecify.annotations.Nullable;

import javax.sql.DataSource;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.lang.Assert;

/**
 * {@link DataSourceLookup} implementation based on a Framework {@link BeanFactory}.
 *
 * <p>Will lookup Framework managed beans identified by bean name,
 * expecting them to be of type {@code javax.sql.DataSource}.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @see BeanFactory
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
