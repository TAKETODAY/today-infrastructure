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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.context.properties.source.ConfigurationProperty;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.MockConfigurationPropertySource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link BoundPropertiesTrackingBindHandler}.
 *
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
class BoundPropertiesTrackingBindHandlerTests {

  private List<ConfigurationPropertySource> sources = new ArrayList<>();

  private BoundPropertiesTrackingBindHandler handler;

  private Binder binder;

  @Mock
  private Consumer<ConfigurationProperty> consumer;

  @BeforeEach
  void setup() {
    this.binder = new Binder(this.sources);
    this.handler = new BoundPropertiesTrackingBindHandler(this.consumer);
  }

  @Test
  void handlerShouldCallRecordBindingIfConfigurationPropertyIsNotNull() {
    this.sources.add(new MockConfigurationPropertySource("foo.age", 4));
    this.binder.bind("foo", Bindable.of(ExampleBean.class), this.handler);
    then(this.consumer).should().accept(any(ConfigurationProperty.class));
    then(this.consumer).should(never()).accept(null);
  }

  static class ExampleBean {

    private int age;

    int getAge() {
      return this.age;
    }

    void setAge(int age) {
      this.age = age;
    }

  }

}
