/*
 * Copyright 2012-present the original author or authors.
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

package infra.jdbc.config;

import infra.beans.BeansException;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.ObjectProvider;
import infra.core.Ordered;
import infra.core.PriorityOrdered;

/**
 * Abstract base class for DataSource bean post processors which apply values from
 * {@link JdbcConnectionDetails}. Property-based connection details
 * ({@link PropertiesJdbcConnectionDetails} are ignored as the expectation is that they
 * will have already been applied by configuration property binding. Acts on beans named
 * 'dataSource' of type {@code T}.
 *
 * @param <T> type of the datasource
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
abstract class JdbcConnectionDetailsBeanPostProcessor<T> implements InitializationBeanPostProcessor, PriorityOrdered {

  private final Class<T> dataSourceClass;

  private final ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider;

  JdbcConnectionDetailsBeanPostProcessor(Class<T> dataSourceClass, ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
    this.dataSourceClass = dataSourceClass;
    this.connectionDetailsProvider = connectionDetailsProvider;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (this.dataSourceClass.isAssignableFrom(bean.getClass()) && "dataSource".equals(beanName)) {
      JdbcConnectionDetails connectionDetails = this.connectionDetailsProvider.get();
      if (!(connectionDetails instanceof PropertiesJdbcConnectionDetails)) {
        return processDataSource((T) bean, connectionDetails);
      }
    }
    return bean;
  }

  protected abstract Object processDataSource(T dataSource, JdbcConnectionDetails connectionDetails);

  @Override
  public int getOrder() {
    // Runs after ConfigurationPropertiesBindingPostProcessor
    return Ordered.HIGHEST_PRECEDENCE + 2;
  }

}
