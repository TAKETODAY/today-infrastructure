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

package cn.taketoday.jdbc.core.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import cn.taketoday.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Juergen Hoeller
 * @since 30.07.2003
 */
public class JdbcDaoSupportTests {

  @Test
  public void testJdbcDaoSupportWithDataSource() throws Exception {
    DataSource ds = mock(DataSource.class);
    final List<String> test = new ArrayList<>();
    JdbcDataAccessObjectSupport dao = new JdbcDataAccessObjectSupport() {
      @Override
      protected void initDao() {
        test.add("test");
      }
    };
    dao.setDataSource(ds);
    dao.afterPropertiesSet();
    assertThat(dao.getDataSource()).as("Correct DataSource").isEqualTo(ds);
    assertThat(dao.getJdbcTemplate().getDataSource()).as("Correct JdbcTemplate").isEqualTo(ds);
    assertThat(test.size()).as("initDao called").isEqualTo(1);
  }

  @Test
  public void testJdbcDaoSupportWithJdbcTemplate() throws Exception {
    JdbcTemplate template = new JdbcTemplate();
    final List<String> test = new ArrayList<>();
    JdbcDataAccessObjectSupport dao = new JdbcDataAccessObjectSupport() {
      @Override
      protected void initDao() {
        test.add("test");
      }
    };
    dao.setJdbcTemplate(template);
    dao.afterPropertiesSet();
    assertThat(template).as("Correct JdbcTemplate").isEqualTo(dao.getJdbcTemplate());
    assertThat(test.size()).as("initDao called").isEqualTo(1);
  }

}
