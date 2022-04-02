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

package cn.taketoday.test.context.junit4.spr8849;

import org.junit.Test;
import org.junit.runner.RunWith;

import javax.sql.DataSource;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ImportResource;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.JUnit4ClassRunner;
import jakarta.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This name of this class intentionally does not end with "Test" or "Tests"
 * since it should only be run as part of the test suite: {@link Spr8849Tests}.
 *
 * @author Mickael Leduque
 * @author Sam Brannen
 * @see Spr8849Tests
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration
public class TestClass1 {

  @Configuration
  @ImportResource("classpath:/cn/taketoday/test/context/junit4/spr8849/datasource-config.xml")
  static class Config {
  }

  @Resource
  DataSource dataSource;

  @Test
  public void dummyTest() {
    assertThat(dataSource).isNotNull();
  }

}
