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

package infra.test.context.aot.samples.xml;

import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Value;
import infra.context.ApplicationContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.TestPropertySource;
import infra.test.context.aot.samples.common.MessageService;
import infra.test.context.junit4.InfraRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@ContextConfiguration("test-config.xml")
@TestPropertySource(properties = "test.engine = vintage")
public class XmlInfraVintageTests {

  @Autowired
  ApplicationContext context;

  @Autowired
  MessageService messageService;

  @Value("${test.engine}")
  String testEngine;

  @org.junit.Test
  public void test() {
    assertThat(testEngine)
            .as("@Value").isEqualTo("vintage");
    assertThat(context.getEnvironment().getProperty("test.engine"))
            .as("Environment").isEqualTo("vintage");

    assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
  }

}
