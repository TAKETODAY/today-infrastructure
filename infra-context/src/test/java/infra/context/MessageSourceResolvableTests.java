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

package infra.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/2 19:55
 */
class MessageSourceResolvableTests {


  @Test
  void defaultImplementationReturnsNullForArguments() {
    MessageSourceResolvable resolvable = new TestMessageSourceResolvable();
    assertThat(resolvable.getArguments()).isNull();
  }

  @Test
  void defaultImplementationReturnsNullForDefaultMessage() {
    MessageSourceResolvable resolvable = new TestMessageSourceResolvable();
    assertThat(resolvable.getDefaultMessage()).isNull();
  }

  @Test
  void multipleCodesAreReturnedInOrder() {
    MessageSourceResolvable resolvable = new TestMessageSourceResolvable("code1", "code2", "code3");
    assertThat(resolvable.getCodes()).containsExactly("code1", "code2", "code3");
  }

  @Test
  void nullCodesArrayIsValid() {
    MessageSourceResolvable resolvable = new TestMessageSourceResolvable((String[]) null);
    assertThat(resolvable.getCodes()).isNull();
  }

  @Test
  void emptyCodesArrayIsValid() {
    MessageSourceResolvable resolvable = new TestMessageSourceResolvable(new String[0]);
    assertThat(resolvable.getCodes()).isEmpty();
  }

  private static class TestMessageSourceResolvable implements MessageSourceResolvable {
    private final String[] codes;

    TestMessageSourceResolvable(String... codes) {
      this.codes = codes;
    }

    @Override
    public String[] getCodes() {
      return this.codes;
    }
  }

}