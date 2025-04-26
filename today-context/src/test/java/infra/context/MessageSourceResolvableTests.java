/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

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