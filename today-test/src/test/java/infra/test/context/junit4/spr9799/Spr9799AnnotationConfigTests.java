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

package infra.test.context.junit4.spr9799;

import org.junit.Test;
import org.junit.runner.RunWith;

import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit4.JUnit4ClassRunner;
import infra.test.context.web.WebAppConfiguration;
import infra.web.config.annotation.EnableWebMvc;

/**
 * Integration tests used to assess claims raised in
 * .
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
