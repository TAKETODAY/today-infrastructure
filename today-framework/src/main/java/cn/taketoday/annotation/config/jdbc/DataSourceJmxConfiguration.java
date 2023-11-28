/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.annotation.config.jdbc;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;

import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;

import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.condition.ConditionalOnSingleCandidate;
import cn.taketoday.jdbc.config.DataSourceUnwrapper;
import cn.taketoday.jmx.export.MBeanExporter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

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

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(prefix = "datasource.tomcat", name = "jmx-enabled")
  @ConditionalOnClass(DataSourceProxy.class)
  @ConditionalOnSingleCandidate(DataSource.class)
  static class TomcatDataSourceJmxConfiguration {
    private final Logger logger = LoggerFactory.getLogger(DataSourceJmxConfiguration.class);

    @Bean
    @Nullable
    @ConditionalOnMissingBean(name = "dataSourceMBean")
    Object dataSourceMBean(DataSource dataSource) {
      DataSourceProxy dataSourceProxy = DataSourceUnwrapper.unwrap(
              dataSource, PoolConfiguration.class, DataSourceProxy.class);
      if (dataSourceProxy != null) {
        try {
          return dataSourceProxy.createPool().getJmxPool();
        }
        catch (SQLException ex) {
          logger.warn("Cannot expose DataSource to JMX (could not connect)");
        }
      }
      return null;
    }

  }

}
