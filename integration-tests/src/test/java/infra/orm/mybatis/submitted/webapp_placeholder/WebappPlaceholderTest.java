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
package infra.orm.mybatis.submitted.webapp_placeholder;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.web.WebAppConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
@ExtendWith(InfraExtension.class)
@WebAppConfiguration
@JUnitConfig(locations = "file:src/test/java/infra/orm/mybatis/submitted/webapp_placeholder/spring.xml")
class WebappPlaceholderTest {

  @Autowired
  private SqlSessionFactory sqlSessionFactory;

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void testName() {
    assertEquals(0, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());
    Mapper mapper = applicationContext.getBean(Mapper.class);
    assertThat(mapper).isNotNull();
    assertEquals(1, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());
  }
}
