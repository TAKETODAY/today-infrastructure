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

import org.jspecify.annotations.Nullable;

import javax.sql.DataSource;

import infra.beans.BeansException;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.Configuration;
import infra.context.annotation.Role;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnProperty;
import infra.core.Ordered;
import infra.jdbc.datasource.LazyConnectionDataSourceProxy;
import infra.jmx.export.MBeanExporter;
import infra.jmx.support.JmxUtils;
import infra.stereotype.Component;

/**
 * Replace the auto-configured {@link DataSource} by a
 * {@linkplain LazyConnectionDataSourceProxy lazy proxy} that fetches the underlying JDBC
 * connection as late as possible. Also make sure to register the target
 * {@link DataSource} in the JMX domain if necessary.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(name = "datasource.connection-fetch", havingValue = "lazy")
class LazyConnectionDataSourceConfiguration {

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static LazyConnectionDataSourceBeanPostProcessor lazyConnectionDataSourceBeanPostProcessor(ObjectProvider<MBeanExporter> mbeanExporter) {
    return new LazyConnectionDataSourceBeanPostProcessor(mbeanExporter);
  }

  static class LazyConnectionDataSourceBeanPostProcessor implements InitializationBeanPostProcessor, Ordered {

    private final ObjectProvider<MBeanExporter> mbeanExporter;

    LazyConnectionDataSourceBeanPostProcessor(ObjectProvider<MBeanExporter> mbeanExporter) {
      this.mbeanExporter = mbeanExporter;
    }

    @Override
    public @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
      if (beanName.equals("dataSource") && bean instanceof DataSource dataSource) {
        this.mbeanExporter.ifAvailable((exporter) -> {
          if (JmxUtils.isMBean(dataSource.getClass())) {
            exporter.registerManagedResource(dataSource, beanName);
          }
        });
        return new LazyConnectionDataSourceProxy(dataSource);
      }
      return bean;
    }

    @Override
    public int getOrder() {
      return 0;
    }

  }

}
