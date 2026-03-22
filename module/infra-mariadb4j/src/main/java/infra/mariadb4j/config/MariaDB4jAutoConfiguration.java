/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.mariadb4j.config;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

import javax.sql.DataSource;

import ch.vorburger.exec.ManagedProcessException;
import infra.context.annotation.DependsOn;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.ApplicationTemp;
import infra.jdbc.config.DataSourceBuilder;
import infra.jdbc.config.DataSourceProperties;
import infra.mariadb4j.DB;
import infra.mariadb4j.DBConfigurationBuilder;
import infra.mariadb4j.MariaDB4jLifecycle;
import infra.stereotype.Component;
import infra.util.PropertyMapper;

/**
 * Auto-configuration for MariaDB4j embedded database.
 * <p>
 * This configuration sets up an embedded MariaDB instance using MariaDB4j,
 * along with the necessary {@link javax.sql.DataSource} bean for database access.
 * It is enabled when the relevant properties are configured and no existing
 * {@link infra.mariadb4j.DB} bean is present.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@AutoConfiguration
@EnableConfigurationProperties({ DataSourceProperties.class, MariaDB4jProperties.class })
public final class MariaDB4jAutoConfiguration {

  @Component
  @DependsOn("mariaDB4j")
  public static DataSource dataSource(DataSourceProperties properties) {
    return DataSourceBuilder.create()
            .driverClassName(properties.getDriverClassName())
            .url(properties.getUrl())
            .username(properties.getUsername())
            .password(properties.getPassword())
            .build();
  }

  @Component
  @ConditionalOnMissingBean
  public static DB mariaDB4j(MariaDB4jProperties properties, @Nullable ApplicationTemp applicationTemp) throws ManagedProcessException {
    if (applicationTemp == null) {
      applicationTemp = ApplicationTemp.instance;
    }

    Path basePath = applicationTemp.getDir("mariaDB4j");
    DBConfigurationBuilder builder = DBConfigurationBuilder.newBuilder();

    PropertyMapper mapper = PropertyMapper.get();
    mapper.from(properties.port).to(builder::setPort);
    mapper.from(properties.socket).to(builder::setSocket);
    mapper.from(properties.unpack).to(builder::setUnpackingFromClasspath);
    mapper.from(properties.defaultCharset).to(builder::setDefaultCharacterSet);
    mapper.from(properties.osUser).to(osUser -> builder.addArg("--user=" + osUser));
    mapper.from(properties.tmpDir).orFrom(() -> basePath.resolve("tmp").toString()).to(builder::setTmpDir);
    mapper.from(properties.dataDir).as(File::new).orFrom(() -> basePath.resolve("data").toFile()).to(builder::setDataDir);
    mapper.from(properties.libDir).as(File::new).to(builder::setLibDir);
    mapper.from(properties.baseDir).as(File::new).orFrom(basePath::toFile).to(builder::setBaseDir);

    return DB.newEmbeddedDB(builder.build());
  }

  @Component
  public static MariaDB4jLifecycle mariaDB4jLifecycle(DB mariaDB4j) {
    return new MariaDB4jLifecycle(mariaDB4j);
  }

}
