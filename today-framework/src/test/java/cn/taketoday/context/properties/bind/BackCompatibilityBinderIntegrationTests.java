/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.context.properties.bind;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.env.SystemEnvironmentPropertySource;
import cn.taketoday.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to ensure that the {@link Binder} offers at least some support for
 * Boot 1.5 style binding.
 *
 * @author Phillip Webb
 */
class BackCompatibilityBinderIntegrationTests {

  @Test
  void bindWhenBindingCamelCaseToEnvironmentWithExtractUnderscore() {
    // gh-10873
    MockEnvironment environment = new MockEnvironment();
    SystemEnvironmentPropertySource propertySource = new SystemEnvironmentPropertySource(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            Collections.singletonMap("FOO_ZK_NODES", "foo"));
    environment.getPropertySources().addFirst(propertySource);
    ExampleCamelCaseBean result = Binder.get(environment).bind("foo", Bindable.of(ExampleCamelCaseBean.class))
            .get();
    assertThat(result.getZkNodes()).isEqualTo("foo");
  }

  @Test
  void bindWhenUsingSystemEnvironmentToOverride() {
    MockEnvironment environment = new MockEnvironment();
    SystemEnvironmentPropertySource propertySource = new SystemEnvironmentPropertySource("override",
            Collections.singletonMap("foo.password", "test"));
    environment.getPropertySources().addFirst(propertySource);
    PasswordProperties result = Binder.get(environment).bind("foo", Bindable.of(PasswordProperties.class)).get();
    assertThat(result.getPassword()).isEqualTo("test");
  }

  static class ExampleCamelCaseBean {

    private String zkNodes;

    String getZkNodes() {
      return this.zkNodes;
    }

    void setZkNodes(String zkNodes) {
      this.zkNodes = zkNodes;
    }

  }

  static class PasswordProperties {

    private String password;

    String getPassword() {
      return this.password;
    }

    void setPassword(String password) {
      this.password = password;
    }

  }

}
