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
/**
 * MyBatis @Configuration style sample
 */
package cn.taketoday.orm.mybatis.sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import cn.taketoday.orm.mybatis.sample.config.SampleConfig;
import cn.taketoday.orm.mybatis.sample.domain.User;
import cn.taketoday.orm.mybatis.sample.service.FooService;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = SampleConfig.class)
class SampleJavaConfigTest {

  @Autowired
  private FooService fooService;

  @Autowired
  private FooService fooServiceWithMapperFactoryBean;

  @Test
  void test() {
    User user = fooService.doSomeBusinessStuff("u1");
    assertThat(user.getName()).isEqualTo("Pocoyo");
  }

  @Test
  void testWithMapperFactoryBean() {
    User user = fooServiceWithMapperFactoryBean.doSomeBusinessStuff("u1");
    assertThat(user.getName()).isEqualTo("Pocoyo");
  }

}
