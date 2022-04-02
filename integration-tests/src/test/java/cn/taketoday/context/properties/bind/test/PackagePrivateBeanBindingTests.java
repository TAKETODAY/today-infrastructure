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

package cn.taketoday.context.properties.bind.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.MockConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Binder} using package private Java beans.
 *
 * @author Madhura Bhave
 */
class PackagePrivateBeanBindingTests {

  private List<ConfigurationPropertySource> sources = new ArrayList<>();

  private Binder binder;

  private ConfigurationPropertyName name;

  @BeforeEach
  void setup() {
    this.binder = new Binder(this.sources);
    this.name = ConfigurationPropertyName.of("foo");
  }

  @Test
  void bindToPackagePrivateClassShouldBindToInstance() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.bar", "999");
    this.sources.add(source);
    ExamplePackagePrivateBean bean = this.binder.bind(this.name, Bindable.of(ExamplePackagePrivateBean.class))
            .get();
    assertThat(bean.getBar()).isEqualTo(999);
  }

  static class ExamplePackagePrivateBean {

    private int bar;

    int getBar() {
      return this.bar;
    }

    void setBar(int bar) {
      this.bar = bar;
    }

  }

}
