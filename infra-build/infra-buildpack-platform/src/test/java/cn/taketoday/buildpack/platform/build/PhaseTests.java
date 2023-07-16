/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.buildpack.platform.build;

import org.junit.jupiter.api.Test;

import cn.taketoday.buildpack.platform.docker.type.Binding;
import cn.taketoday.buildpack.platform.docker.type.ContainerConfig.Update;
import cn.taketoday.buildpack.platform.docker.type.VolumeName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Phase}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 */
class PhaseTests {

  private static final String[] NO_ARGS = {};

  @Test
  void getNameReturnsName() {
    Phase phase = new Phase("test", false);
    assertThat(phase.getName()).isEqualTo("test");
  }

  @Test
  void toStringReturnsName() {
    Phase phase = new Phase("test", false);
    assertThat(phase).hasToString("test");
  }

  @Test
  void applyUpdatesConfiguration() {
    Phase phase = new Phase("test", false);
    Update update = mock(Update.class);
    phase.apply(update);
    then(update).should().withCommand("/cnb/lifecycle/test", NO_ARGS);
    then(update).should().withLabel("author", "infra-app");
    then(update).shouldHaveNoMoreInteractions();
  }

  @Test
  void applyWhenWithDaemonAccessUpdatesConfigurationWithRootUser() {
    Phase phase = new Phase("test", false);
    phase.withDaemonAccess();
    Update update = mock(Update.class);
    phase.apply(update);
    then(update).should().withUser("root");
    then(update).should().withCommand("/cnb/lifecycle/test", NO_ARGS);
    then(update).should().withLabel("author", "infra-app");
    then(update).shouldHaveNoMoreInteractions();
  }

  @Test
  void applyWhenWithLogLevelArgAndVerboseLoggingUpdatesConfigurationWithLogLevel() {
    Phase phase = new Phase("test", true);
    phase.withLogLevelArg();
    Update update = mock(Update.class);
    phase.apply(update);
    then(update).should().withCommand("/cnb/lifecycle/test", "-log-level", "debug");
    then(update).should().withLabel("author", "infra-app");
    then(update).shouldHaveNoMoreInteractions();
  }

  @Test
  void applyWhenWithLogLevelArgAndNonVerboseLoggingDoesNotUpdateLogLevel() {
    Phase phase = new Phase("test", false);
    phase.withLogLevelArg();
    Update update = mock(Update.class);
    phase.apply(update);
    then(update).should().withCommand("/cnb/lifecycle/test");
    then(update).should().withLabel("author", "infra-app");
    then(update).shouldHaveNoMoreInteractions();
  }

  @Test
  void applyWhenWithArgsUpdatesConfigurationWithArguments() {
    Phase phase = new Phase("test", false);
    phase.withArgs("a", "b", "c");
    Update update = mock(Update.class);
    phase.apply(update);
    then(update).should().withCommand("/cnb/lifecycle/test", "a", "b", "c");
    then(update).should().withLabel("author", "infra-app");
    then(update).shouldHaveNoMoreInteractions();
  }

  @Test
  void applyWhenWithBindsUpdatesConfigurationWithBinds() {
    Phase phase = new Phase("test", false);
    VolumeName volumeName = VolumeName.of("test");
    phase.withBinding(Binding.from(volumeName, "/test"));
    Update update = mock(Update.class);
    phase.apply(update);
    then(update).should().withCommand("/cnb/lifecycle/test");
    then(update).should().withLabel("author", "infra-app");
    then(update).should().withBinding(Binding.from(volumeName, "/test"));
    then(update).shouldHaveNoMoreInteractions();
  }

  @Test
  void applyWhenWithEnvUpdatesConfigurationWithEnv() {
    Phase phase = new Phase("test", false);
    phase.withEnv("name1", "value1");
    phase.withEnv("name2", "value2");
    Update update = mock(Update.class);
    phase.apply(update);
    then(update).should().withCommand("/cnb/lifecycle/test");
    then(update).should().withLabel("author", "infra-app");
    then(update).should().withEnv("name1", "value1");
    then(update).should().withEnv("name2", "value2");
    then(update).shouldHaveNoMoreInteractions();
  }

  @Test
  void applyWhenWithNetworkModeUpdatesConfigurationWithNetworkMode() {
    Phase phase = new Phase("test", true);
    phase.withNetworkMode("test");
    Update update = mock(Update.class);
    phase.apply(update);
    then(update).should().withCommand("/cnb/lifecycle/test");
    then(update).should().withNetworkMode("test");
    then(update).should().withLabel("author", "infra-app");
    then(update).shouldHaveNoMoreInteractions();
  }

  @Test
  void applyWhenWithSecurityOptionsUpdatesConfigurationWithSecurityOptions() {
    Phase phase = new Phase("test", true);
    phase.withSecurityOption("option1=value1");
    phase.withSecurityOption("option2=value2");
    Update update = mock(Update.class);
    phase.apply(update);
    then(update).should().withCommand("/cnb/lifecycle/test");
    then(update).should().withLabel("author", "infra-app");
    then(update).should().withSecurityOption("option1=value1");
    then(update).should().withSecurityOption("option2=value2");
    then(update).shouldHaveNoMoreInteractions();
  }

}
