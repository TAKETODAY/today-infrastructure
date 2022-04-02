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

package cn.taketoday.test.context.junit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import cn.taketoday.test.context.TestExecutionListeners;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies support for JUnit 4.7 {@link Rule Rules} in conjunction with the
 * {@link Runner}. The body of this test class is taken from the
 * JUnit 4.7 release notes.
 *
 * @author JUnit 4.7 Team
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(Runner.class)
@TestExecutionListeners({})
public class SpringJUnit47ClassRunnerRuleTests {

  @Rule
  public TestName name = new TestName();

  @Test
  public void testA() {
    assertThat(name.getMethodName()).isEqualTo("testA");
  }

  @Test
  public void testB() {
    assertThat(name.getMethodName()).isEqualTo("testB");
  }
}
