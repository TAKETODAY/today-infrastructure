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

package cn.taketoday.annotation.config.jdbc;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.condition.ConditionalOnSingleCandidate;
import cn.taketoday.jdbc.config.DataSourceUnwrapper;
import cn.taketoday.jmx.export.MBeanExporter;

/**
 * Configures DataSource related MBeans.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "infra.jmx", name = "enabled", havingValue = "true", matchIfMissing = true)
class DataSourceJmxConfiguration {

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
      HikariDataSource hikariDataSource = DataSourceUnwrapper.unwrap(
              dataSource, HikariConfigMXBean.class, HikariDataSource.class);
      if (hikariDataSource != null && hikariDataSource.isRegisterMbeans()) {
        mBeanExporter.ifUnique(exporter -> exporter.addExcludedBean("dataSource"));
      }
    }

  }

}
