/*
 * Copyright 2002-present the original author or authors.
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

package infra.jdbc.core.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import infra.jdbc.core.JdbcTemplate;

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
