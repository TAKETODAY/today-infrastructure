/*
 * Copyright 2017 - 2026 the TODAY authors.
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
