/*
 * Copyright 2026-present the original author or authors.
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

package infra.shell.command.annotation.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.context.annotation.Profile;
import infra.core.annotation.AnnotatedElementUtils;
import infra.core.env.Profiles;
import infra.mock.env.MockEnvironment;
import infra.shell.command.annotation.Command;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnableCommandRegistrar} profile filtering support.
 *
 * @author David Pilar
 */
class EnableCommandRegistrarTests {

  @Test
  void commandWithoutProfileShouldAlwaysBeActive() throws Exception {
    Method method = TestCommands.class.getDeclaredMethod("noProfileCommand");
    Profile profile = AnnotatedElementUtils.findMergedAnnotation(method, Profile.class);
    assertThat(profile).as("Method without @Profile should not have a Profile annotation").isNull();
  }

  @Test
  void commandWithProfileShouldBeActiveWhenProfileMatches() throws Exception {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("greetings");

    Method method = TestCommands.class.getDeclaredMethod("greetingsProfileCommand");
    Profile profile = AnnotatedElementUtils.findMergedAnnotation(method, Profile.class);

    assertThat(profile).as("Method should have @Profile annotation").isNotNull();
    assertThat(environment.acceptsProfiles(Profiles.parse(profile.value()))).as("Command should be active when 'greetings' profile is active").isTrue();
  }

  @Test
  void commandWithProfileShouldNotBeActiveWhenProfileDoesNotMatch() throws Exception {
    MockEnvironment environment = new MockEnvironment();
    // no active profiles

    Method method = TestCommands.class.getDeclaredMethod("greetingsProfileCommand");
    Profile profile = AnnotatedElementUtils.findMergedAnnotation(method, Profile.class);

    assertThat(profile).as("Method should have @Profile annotation").isNotNull();
    assertThat(environment.acceptsProfiles(Profiles.parse(profile.value()))).as("Command should not be active when 'greetings' profile is not active").isFalse();
  }

  @Test
  void commandWithNegatedProfileShouldBeActiveWhenProfileIsNotSet() throws Exception {
    MockEnvironment environment = new MockEnvironment();
    // no active profiles

    Method method = TestCommands.class.getDeclaredMethod("notProductionCommand");
    Profile profile = AnnotatedElementUtils.findMergedAnnotation(method, Profile.class);

    assertThat(profile).as("Method should have @Profile annotation").isNotNull();
    assertThat(environment.acceptsProfiles(Profiles.parse(profile.value()))).as("Command with '!production' profile should be active when 'production' is not active").isTrue();
  }

  @Test
  void commandWithNegatedProfileShouldNotBeActiveWhenProfileIsSet() throws Exception {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("production");

    Method method = TestCommands.class.getDeclaredMethod("notProductionCommand");
    Profile profile = AnnotatedElementUtils.findMergedAnnotation(method, Profile.class);

    assertThat(profile).as("Method should have @Profile annotation").isNotNull();
    assertThat(environment.acceptsProfiles(Profiles.parse(profile.value()))).as("Command with '!production' profile should not be active when 'production' is active").isFalse();
  }

  static class TestCommands {

    @Command(name = "no-profile")
    public void noProfileCommand() {
    }

    @Profile("greetings")
    @Command(name = "hello")
    public void greetingsProfileCommand() {
    }

    @Profile("!production")
    @Command(name = "debug-info")
    public void notProductionCommand() {
    }

  }

}
