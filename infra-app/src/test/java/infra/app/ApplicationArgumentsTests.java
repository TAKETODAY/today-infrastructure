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

package infra.app;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/2 21:28
 */
class ApplicationArgumentsTests {

  private static final String[] ARGS = new String[] { "--foo=bar", "--foo=baz", "--debug", "spring", "boot" };

  @Test
  void argumentsMustNotBeNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ApplicationArguments((String[]) null))
            .withMessageContaining("Args is required");
  }

  @Test
  void getArgs() {
    ApplicationArguments arguments = new ApplicationArguments(ARGS);
    assertThat(arguments.getSourceArgs()).isEqualTo(ARGS);
  }

  @Test
  void optionNames() {
    ApplicationArguments arguments = new ApplicationArguments(ARGS);
    Set<String> expected = new HashSet<>(Arrays.asList("foo", "debug"));
    assertThat(arguments.getOptionNames()).isEqualTo(expected);
  }

  @Test
  void containsOption() {
    ApplicationArguments arguments = new ApplicationArguments(ARGS);
    assertThat(arguments.containsOption("foo")).isTrue();
    assertThat(arguments.containsOption("debug")).isTrue();
    assertThat(arguments.containsOption("spring")).isFalse();
  }

  @Test
  void getOptionValues() {
    ApplicationArguments arguments = new ApplicationArguments(ARGS);
    assertThat(arguments.getOptionValues("foo")).isEqualTo(Arrays.asList("bar", "baz"));
    assertThat(arguments.getOptionValues("debug")).isEmpty();
    assertThat(arguments.getOptionValues("spring")).isNull();
  }

  @Test
  void getNonOptionArgs() {
    ApplicationArguments arguments = new ApplicationArguments(ARGS);
    assertThat(arguments.getNonOptionArgs()).containsExactly("spring", "boot");
  }

  @Test
  void getNoNonOptionArgs() {
    ApplicationArguments arguments = new ApplicationArguments("--debug");
    assertThat(arguments.getNonOptionArgs()).isEmpty();
  }

}