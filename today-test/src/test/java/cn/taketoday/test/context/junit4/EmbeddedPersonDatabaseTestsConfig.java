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

package cn.taketoday.test.context.junit4;

import javax.sql.DataSource;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.transaction.PlatformTransactionManager;

/**
 * Shared configuration for tests that need an embedded database pre-loaded
 * with the schema for the 'person' table.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Configuration
public class EmbeddedPersonDatabaseTestsConfig {

  @Bean
  public PlatformTransactionManager txMgr() {
    return new DataSourceTransactionManager(dataSource());
  }

  @Bean
  public DataSource dataSource() {
    return new EmbeddedDatabaseBuilder()//
            .generateUniqueName(true)//
            .addScript("classpath:/cn/taketoday/test/jdbc/schema.sql") //
            .build();
  }

}
