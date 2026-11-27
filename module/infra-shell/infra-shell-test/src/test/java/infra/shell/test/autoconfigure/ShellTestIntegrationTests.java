/*
 * Copyright 2022-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.shell.test.autoconfigure;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.shell.command.CommandNotFoundException;
import infra.shell.test.ShellAssertions;
import infra.shell.test.ShellScreen;
import infra.shell.test.ShellTestClient;
import infra.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ShellTest
@ContextConfiguration(classes = ExampleShellApplication.class)
class ShellTestIntegrationTests {

  @Test
  void testCommandExecution(@Autowired ShellTestClient client) throws Exception {
    // when
    ShellScreen shellScreen = client.sendCommand("hi");

    // then
    ShellAssertions.assertThat(shellScreen).containsText("hello");
  }

  @Test
  void testUnknownCommandExecution(@Autowired ShellTestClient client) {
    assertThatThrownBy(() -> client.sendCommand("foo")).isInstanceOf(CommandNotFoundException.class);
  }

}
