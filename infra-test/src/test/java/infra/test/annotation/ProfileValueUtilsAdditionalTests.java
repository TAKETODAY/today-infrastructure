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

package infra.test.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ProfileValueUtilsAdditionalTests {

  @Test
  void brokenSourceFallsBackToSystemSource() {
    assertThat(ProfileValueUtils.retrieveProfileValueSource(BrokenSourceConfiguration.class))
            .isSameAs(SystemProfileValueSource.getInstance());
    assertThatIllegalArgumentException().isThrownBy(() -> ProfileValueUtils.retrieveProfileValueSource(null));
  }

  @Test
  void classAndMethodValuesControlEnablement() throws Exception {
    var method = ProfileMethods.class.getDeclaredMethod("multiple");
    assertThat(ProfileValueUtils.isTestEnabledInThisEnvironment(name -> "second", method, ProfileMethods.class)).isTrue();
    assertThat(ProfileValueUtils.isTestEnabledInThisEnvironment(name -> "other", method, ProfileMethods.class)).isFalse();
    assertThat(ProfileValueUtils.isTestEnabledInThisEnvironment(name -> null, method, ProfileMethods.class)).isFalse();
    assertThat(ProfileValueUtils.isTestEnabledInThisEnvironment(name -> "other", method, DisabledClass.class)).isFalse();
    assertThatIllegalArgumentException().isThrownBy(() -> ProfileValueUtils.isTestEnabledInThisEnvironment(
            name -> "first", ProfileMethods.class.getDeclaredMethod("conflicting"), ProfileMethods.class))
            .withMessageContaining("Setting both");
    assertThat(ProfileValueUtils.isTestEnabledInThisEnvironment(name -> "anything",
            ProfileMethods.class.getDeclaredMethod("unrestricted"), ProfileMethods.class)).isTrue();
  }

  @ProfileValueSourceConfiguration(BrokenSource.class)
  static class BrokenSourceConfiguration { }

  static class BrokenSource implements ProfileValueSource {
    BrokenSource() {
      throw new IllegalStateException("not available");
    }

    @Override
    public String get(String key) {
      return null;
    }
  }

  @IfProfileValue(name = "mode", value = "enabled")
  static class DisabledClass { }

  static class ProfileMethods {
    @IfProfileValue(name = "mode", values = { "first", "second" })
    void multiple() { }

    @IfProfileValue(name = "mode", value = "first", values = "second")
    void conflicting() { }

    void unrestricted() { }
  }

}
