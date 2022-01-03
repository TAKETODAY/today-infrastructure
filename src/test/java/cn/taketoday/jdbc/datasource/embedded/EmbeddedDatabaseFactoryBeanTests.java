/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.datasource.embedded;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import cn.taketoday.core.io.ClassRelativeResourceLoader;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Keith Donald
 */
public class EmbeddedDatabaseFactoryBeanTests {

  private final DefaultResourceLoader resourceLoader = new ClassRelativeResourceLoader(getClass());

  Resource resource(String path) {
    return resourceLoader.getResource(path);
  }

  @Test
  public void testFactoryBeanLifecycle() throws Exception {
    EmbeddedDatabaseFactoryBean bean = new EmbeddedDatabaseFactoryBean();
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator(resource("db-schema.sql"),
            resource("db-test-data.sql"));
    bean.setDatabasePopulator(populator);
    bean.afterPropertiesSet();
    DataSource ds = bean.getObject();
    JdbcTemplate template = new JdbcTemplate(ds);
    assertThat(template.queryForObject("select NAME from T_TEST", String.class)).isEqualTo("Keith");
    bean.destroy();
  }

}
