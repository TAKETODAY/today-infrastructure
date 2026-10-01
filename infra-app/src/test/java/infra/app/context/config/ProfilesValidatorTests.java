/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.context.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import infra.context.properties.bind.BindException;
import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.Binder;
import infra.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link ProfilesValidator}.
 *
 * @author Phillip Webb
 * @author Sijun Yang
 */
class ProfilesValidatorTests {

  private static final Bindable<String> STRING = Bindable.of(String.class);

  private static final Bindable<List<String>> STRING_LIST = Bindable.listOf(String.class);

  private static final Bindable<Map<String, String>> STRING_STRING_MAP = Bindable.mapOf(String.class, String.class);

  @Test
  void validateWhenValid() {
    for (String value : List.of("test", "dev-test", "dev-test_123", "dev-테스트_123", "d-e_v-t-.e_@@s+t")) {
      assertThatNoException().isThrownBy(() -> bind(Map.of("profile", value), STRING));
    }
  }

  @Test
  void validateWhenInvalidThrowsException() {
    for (String value : List.of("-dev", "_dev", "+dev", ".dev", "dev_", "dev-", "dev*test")) {
      assertInvalid(Map.of("profile", value), STRING);
    }
  }

  @Test
  void validateWhenInvalidBoundStringThrowsException() {
    assertInvalid(Map.of("profile", "dev*test"), STRING);
  }

  @Test
  void validateWhenInvalidBoundCollectionThrowsException() {
    assertInvalid(Map.of("profile", "dev*test"), STRING_LIST);
  }

  @Test
  void validateWhenInvalidBoundCollectionFromIndexedThrowsException() {
    assertInvalid(Map.of("profile[0]", "ok,", "profile[1]", "dev*test"), STRING_LIST);
  }

  @Test
  void validateWhenInvalidBoundMapFromIndexedThrowsException() {
    assertInvalid(Map.of("profile.foo", "dev*test"), STRING_STRING_MAP);
  }

  @Test
  void validateWhenInvalidThrowsUsefulExceptionMessage() {
    assertThatExceptionOfType(BindException.class).isThrownBy(() -> bind(Map.of("profile", "b*d"), STRING))
            .havingCause().withMessageContaining(
                    "Profile 'b*d' must contain a letter, digit or allowed char ('-', '_', '.', '+', '@')");
  }

  @Test
  void validateWhenInvalidStartCharacterThrowsUsefulExceptionMessage() {
    assertThatExceptionOfType(BindException.class).isThrownBy(() -> bind(Map.of("profile", "_bad"), STRING))
            .havingCause().withMessageContaining("Profile '_bad' must start and end with a letter or digit");
  }

  @Test
  void validateWithWrappedExceptionMessageWhenValid() {
    assertThatNoException().isThrownBy(() -> ProfilesValidator.get(new Binder()).validate("ok", () -> "context"));
  }

  @Test
  void validateWithWrappedExceptionMessageWhenInvalidThrowsException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> ProfilesValidator.get(new Binder()).validate("b*d", () -> "context"))
            .withMessage("context").havingCause().withMessageContaining("must contain a letter");
  }

  private <T> void assertInvalid(Map<String, String> map, Bindable<T> target) {
    assertThatExceptionOfType(BindException.class).isThrownBy(() -> bind(map, target));
  }

  private <T> void bind(Map<?, ?> map, Bindable<T> target) {
    Binder binder = new Binder(new MapConfigurationPropertySource(map));
    binder.bind("profile", target, ProfilesValidator.get(binder));
  }

}
