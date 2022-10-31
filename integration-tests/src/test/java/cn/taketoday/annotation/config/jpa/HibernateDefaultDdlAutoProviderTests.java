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

package cn.taketoday.annotation.config.jpa;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import javax.sql.DataSource;

import cn.taketoday.annotation.config.jdbc.DataSourceAutoConfiguration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.jdbc.SchemaManagement;
import cn.taketoday.framework.jdbc.SchemaManagementProvider;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HibernateDefaultDdlAutoProvider}.
 *
 * @author Stephane Nicoll
 */
class HibernateDefaultDdlAutoProviderTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(
                  AutoConfigurations.of(DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
          .withPropertyValues("spring.sql.init.mode:never");

  @Test
  void defaultDDlAutoForEmbedded() {
    this.contextRunner.run((context) -> {
      HibernateDefaultDdlAutoProvider ddlAutoProvider = new HibernateDefaultDdlAutoProvider(
              Collections.emptyList());
      assertThat(ddlAutoProvider.getDefaultDdlAuto(context.getBean(DataSource.class))).isEqualTo("create-drop");
    });
  }

  @Test
  void defaultDDlAutoForEmbeddedWithPositiveContributor() {
    this.contextRunner.run((context) -> {
      DataSource dataSource = context.getBean(DataSource.class);
      SchemaManagementProvider provider = mock(SchemaManagementProvider.class);
      given(provider.getSchemaManagement(dataSource)).willReturn(SchemaManagement.MANAGED);
      HibernateDefaultDdlAutoProvider ddlAutoProvider = new HibernateDefaultDdlAutoProvider(
              Collections.singletonList(provider));
      assertThat(ddlAutoProvider.getDefaultDdlAuto(dataSource)).isEqualTo("none");
    });
  }

  @Test
  void defaultDDlAutoForEmbeddedWithNegativeContributor() {
    this.contextRunner.run((context) -> {
      DataSource dataSource = context.getBean(DataSource.class);
      SchemaManagementProvider provider = mock(SchemaManagementProvider.class);
      given(provider.getSchemaManagement(dataSource)).willReturn(SchemaManagement.UNMANAGED);
      HibernateDefaultDdlAutoProvider ddlAutoProvider = new HibernateDefaultDdlAutoProvider(
              Collections.singletonList(provider));
      assertThat(ddlAutoProvider.getDefaultDdlAuto(dataSource)).isEqualTo("create-drop");
    });
  }

}
