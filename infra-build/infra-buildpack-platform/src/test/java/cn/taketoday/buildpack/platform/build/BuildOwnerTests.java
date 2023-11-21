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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link BuildOwner}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class BuildOwnerTests {

  @Test
  void fromEnvReturnsOwner() {
    Map<String, String> env = new LinkedHashMap<>();
    env.put("CNB_USER_ID", "123");
    env.put("CNB_GROUP_ID", "456");
    BuildOwner owner = BuildOwner.fromEnv(env);
    assertThat(owner.getUid()).isEqualTo(123);
    assertThat(owner.getGid()).isEqualTo(456);
    assertThat(owner).hasToString("123/456");
  }

  @Test
  void fromEnvWhenEnvIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> BuildOwner.fromEnv(null))
            .withMessage("Env is required");
  }

  @Test
  void fromEnvWhenUserPropertyIsMissingThrowsException() {
    Map<String, String> env = new LinkedHashMap<>();
    env.put("CNB_GROUP_ID", "456");
    assertThatIllegalStateException().isThrownBy(() -> BuildOwner.fromEnv(env))
            .withMessage("Missing 'CNB_USER_ID' value from the builder environment '" + env + "'");
  }

  @Test
  void fromEnvWhenGroupPropertyIsMissingThrowsException() {
    Map<String, String> env = new LinkedHashMap<>();
    env.put("CNB_USER_ID", "123");
    assertThatIllegalStateException().isThrownBy(() -> BuildOwner.fromEnv(env))
            .withMessage("Missing 'CNB_GROUP_ID' value from the builder environment '" + env + "'");
  }

  @Test
  void fromEnvWhenUserPropertyIsMalformedThrowsException() {
    Map<String, String> env = new LinkedHashMap<>();
    env.put("CNB_USER_ID", "nope");
    env.put("CNB_GROUP_ID", "456");
    assertThatIllegalStateException().isThrownBy(() -> BuildOwner.fromEnv(env))
            .withMessage("Malformed 'CNB_USER_ID' value 'nope' in the builder environment '" + env + "'");
  }

  @Test
  void fromEnvWhenGroupPropertyIsMalformedThrowsException() {
    Map<String, String> env = new LinkedHashMap<>();
    env.put("CNB_USER_ID", "123");
    env.put("CNB_GROUP_ID", "nope");
    assertThatIllegalStateException().isThrownBy(() -> BuildOwner.fromEnv(env))
            .withMessage("Malformed 'CNB_GROUP_ID' value 'nope' in the builder environment '" + env + "'");
  }

}
