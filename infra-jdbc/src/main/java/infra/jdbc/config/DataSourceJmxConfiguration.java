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

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import infra.beans.factory.ObjectProvider;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnBooleanProperty;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnSingleCandidate;
import infra.jmx.export.MBeanExporter;
import infra.util.ClassUtils;

/**
 * Configures DataSource related MBeans.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("jmx.enabled")
class DataSourceJmxConfiguration {

  private static final boolean HIKARI_PRESENT = ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource",
          DataSourceJmxConfiguration.class.getClassLoader());

  /**
   * Return whether the given {@link DataSource} registers its own MBeans, in which case
   * it should not be exposed by the {@link MBeanExporter}.
   *
   * @param dataSource the data source to check
   * @return {@code true} if the data source registers its own MBeans
   */
  static boolean registersItsOwnMBeans(DataSource dataSource) {
    return HIKARI_PRESENT && Hikari.registersItsOwnMBeans(dataSource);
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(HikariDataSource.class)
  @ConditionalOnSingleCandidate(DataSource.class)
  static class Hikari {

    private final DataSource dataSource;

    private final ObjectProvider<MBeanExporter> mBeanExporter;

    Hikari(DataSource dataSource, ObjectProvider<MBeanExporter> mBeanExporter) {
      this.dataSource = dataSource;
      this.mBeanExporter = mBeanExporter;
      validateMBeans();
    }

    private void validateMBeans() {
      if (registersItsOwnMBeans(dataSource)) {
        mBeanExporter.ifUnique(exporter -> exporter.addExcludedBean("dataSource"));
      }
    }

    static boolean registersItsOwnMBeans(DataSource dataSource) {
      HikariDataSource hikariDataSource = DataSourceUnwrapper.unwrap(
              dataSource, HikariConfigMXBean.class, HikariDataSource.class);
      return hikariDataSource != null && hikariDataSource.isRegisterMbeans();
    }

  }

}
