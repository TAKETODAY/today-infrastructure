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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedDataSourceConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class EmbeddedDataSourceConfigurationTests {

  private AnnotationConfigApplicationContext context;

  @AfterEach
  void closeContext() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void defaultEmbeddedDatabase() {
    this.context = load();
    assertThat(this.context.getBean(DataSource.class)).isNotNull();
  }

  @Test
  void generateUniqueName() throws Exception {
    this.context = load("datasource.generate-unique-name=true");
    var context2 = load("datasource.generate-unique-name=true");

    DataSource dataSource = this.context.getBean(DataSource.class);
    DataSource dataSource2 = context2.getBean(DataSource.class);
    assertThat(getDatabaseName(dataSource)).isNotEqualTo(getDatabaseName(dataSource2));
    context2.close();
  }

  private String getDatabaseName(DataSource dataSource) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      ResultSet catalogs = connection.getMetaData().getCatalogs();
      if (catalogs.next()) {
        return catalogs.getString(1);
      }
      else {
        throw new IllegalStateException("Unable to get database name");
      }
    }
  }

  private AnnotationConfigApplicationContext load(String... environment) {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    TestPropertyValues.of(environment).applyTo(ctx);
    ctx.register(EmbeddedDataSourceConfiguration.class);
    ctx.refresh();
    return ctx;
  }

}
