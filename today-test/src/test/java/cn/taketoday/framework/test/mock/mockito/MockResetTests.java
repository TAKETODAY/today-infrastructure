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

package cn.taketoday.framework.test.mock.mockito;

import org.junit.jupiter.api.Test;

import cn.taketoday.framework.test.mock.mockito.example.ExampleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/11 17:27
 */
class MockResetTests {

  @Test
  void noneAttachesReset() {
    ExampleService mock = mock(ExampleService.class);
    assertThat(MockReset.get(mock)).isEqualTo(MockReset.NONE);
  }

  @Test
  void withSettingsOfNoneAttachesReset() {
    ExampleService mock = mock(ExampleService.class, MockReset.withSettings(MockReset.NONE));
    assertThat(MockReset.get(mock)).isEqualTo(MockReset.NONE);
  }

  @Test
  void beforeAttachesReset() {
    ExampleService mock = mock(ExampleService.class, MockReset.before());
    assertThat(MockReset.get(mock)).isEqualTo(MockReset.BEFORE);
  }

  @Test
  void afterAttachesReset() {
    ExampleService mock = mock(ExampleService.class, MockReset.after());
    assertThat(MockReset.get(mock)).isEqualTo(MockReset.AFTER);
  }

  @Test
  void withSettingsAttachesReset() {
    ExampleService mock = mock(ExampleService.class, MockReset.withSettings(MockReset.BEFORE));
    assertThat(MockReset.get(mock)).isEqualTo(MockReset.BEFORE);
  }

  @Test
  void apply() {
    ExampleService mock = mock(ExampleService.class, MockReset.apply(MockReset.AFTER, withSettings()));
    assertThat(MockReset.get(mock)).isEqualTo(MockReset.AFTER);
  }

}