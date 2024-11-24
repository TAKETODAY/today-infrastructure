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

package infra.context.properties.source;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import infra.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link MutuallyExclusiveConfigurationPropertiesException}.
 *
 * @author Phillip Webb
 */
class MutuallyExclusiveConfigurationPropertiesExceptionTests {

  @Test
  void createWhenConfiguredNamesIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new MutuallyExclusiveConfigurationPropertiesException(null, Arrays.asList("a", "b")))
            .withMessage("ConfiguredNames must contain 2 or more names");
  }

  @Test
  void createWhenConfiguredNamesContainsOneElementThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new MutuallyExclusiveConfigurationPropertiesException(Collections.singleton("a"),
                    Arrays.asList("a", "b")))
            .withMessage("ConfiguredNames must contain 2 or more names");
  }

  @Test
  void createWhenMutuallyExclusiveNamesIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new MutuallyExclusiveConfigurationPropertiesException(Arrays.asList("a", "b"), null))
            .withMessage("MutuallyExclusiveNames must contain 2 or more names");
  }

  @Test
  void createWhenMutuallyExclusiveNamesContainsOneElementThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new MutuallyExclusiveConfigurationPropertiesException(Arrays.asList("a", "b"),
                    Collections.singleton("a")))
            .withMessage("MutuallyExclusiveNames must contain 2 or more names");
  }

  @Test
  void createBuildsSensibleMessage() {
    List<String> names = Arrays.asList("a", "b");
    assertThat(new MutuallyExclusiveConfigurationPropertiesException(names, names))
            .hasMessage("The configuration properties 'a, b' are mutually exclusive "
                    + "and 'a, b' have been configured together");
  }

  @Test
  void getConfiguredNamesReturnsConfiguredNames() {
    List<String> configuredNames = Arrays.asList("a", "b");
    List<String> mutuallyExclusiveNames = Arrays.asList("a", "b", "c");
    MutuallyExclusiveConfigurationPropertiesException exception = new MutuallyExclusiveConfigurationPropertiesException(
            configuredNames, mutuallyExclusiveNames);
    assertThat(exception.getConfiguredNames()).hasSameElementsAs(configuredNames);
  }

  @Test
  void getMutuallyExclusiveNamesReturnsMutuallyExclusiveNames() {
    List<String> configuredNames = Arrays.asList("a", "b");
    List<String> mutuallyExclusiveNames = Arrays.asList("a", "b", "c");
    MutuallyExclusiveConfigurationPropertiesException exception = new MutuallyExclusiveConfigurationPropertiesException(
            configuredNames, mutuallyExclusiveNames);
    assertThat(exception.getMutuallyExclusiveNames()).hasSameElementsAs(mutuallyExclusiveNames);
  }

  @Test
  void throwIfMultipleNonNullValuesInWhenEntriesHasAllNullsDoesNotThrowException() {
    assertThatNoException().isThrownBy(
            () -> MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
              entries.put("a", null);
              entries.put("b", null);
              entries.put("c", null);
            }));
  }

  @Test
  void throwIfMultipleNonNullValuesInWhenEntriesHasSingleNonNullDoesNotThrowException() {
    assertThatNoException().isThrownBy(
            () -> MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
              entries.put("a", null);
              entries.put("b", "B");
              entries.put("c", null);
            }));
  }

  @Test
  void throwIfMultipleNonNullValuesInWhenEntriesHasTwoNonNullsThrowsException() {
    assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class).isThrownBy(
            () -> MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
              entries.put("a", "a");
              entries.put("b", "B");
              entries.put("c", null);
            })).satisfies((ex) -> {
      assertThat(ex.getConfiguredNames()).containsExactly("a", "b");
      assertThat(ex.getMutuallyExclusiveNames()).containsExactly("a", "b", "c");
    });
  }

}
