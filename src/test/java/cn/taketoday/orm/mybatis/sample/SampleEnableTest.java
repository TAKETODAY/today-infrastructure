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
package cn.taketoday.orm.mybatis.sample;

import cn.taketoday.orm.mybatis.annotation.MapperScan;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ImportResource;
import cn.taketoday.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Test to ensure that the {@link MapperScan} annotation works as expected.
 *
 * @since 1.2.0
 */
@SpringJUnitConfig
class SampleEnableTest extends AbstractSampleTest {

  @Configuration
  @ImportResource("classpath:org/mybatis/spring/sample/config/applicationContext-infrastructure.xml")
  @MapperScan("cn.taketoday.orm.mybatis.sample.mapper")
  static class AppConfig {
  }
}
