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

package cn.taketoday.test.context.junit4.spr9799;

import org.junit.Test;
import org.junit.runner.RunWith;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.JUnit4ClassRunner;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.web.config.EnableWebMvc;

/**
 * Integration tests used to assess claims raised in
 * <a href="https://jira.spring.io/browse/SPR-9799" target="_blank">SPR-9799</a>.
 *
 * @author Sam Brannen
 * @see Spr9799XmlConfigTests
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration
// NOTE: if we omit the @WebAppConfiguration declaration, the ApplicationContext will fail
// to load since @EnableWebMvc requires that the context be a WebApplicationContext.
@WebAppConfiguration
public class Spr9799AnnotationConfigTests {

  @Configuration
  @EnableWebMvc
  static class Config {
    /* intentionally no beans defined */
  }

  @Test
  public void applicationContextLoads() {
    // no-op
  }

}
