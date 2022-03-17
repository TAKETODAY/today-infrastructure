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
package cn.taketoday.orm.mybatis.submitted.webapp_placeholder;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.context.junit.jupiter.SpringExtension;
import cn.taketoday.test.context.junit.jupiter.SpringJUnitConfig;
import cn.taketoday.test.context.web.WebAppConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@SpringJUnitConfig(locations = "file:src/test/java/cn/taketoday/orm/mybatis/submitted/webapp_placeholder/spring.xml")
class WebappPlaceholderTest {

  @Autowired
  private SqlSessionFactory sqlSessionFactory;

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void testName() {
    Assertions.assertEquals(0, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());
    Mapper mapper = applicationContext.getBean(Mapper.class);
    assertThat(mapper).isNotNull();
    Assertions.assertEquals(1, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());
  }
}
