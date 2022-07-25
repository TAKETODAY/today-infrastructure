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

package cn.taketoday.core;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;

import cn.taketoday.tests.sample.objects.TestObject;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/9/11 16:50
 */
class ParameterNameDiscovererTests {

  private static final String[] FOO_BAR = new String[] { "foo", "bar" };

  private static final String[] SOMETHING_ELSE = new String[] { "something", "else" };

  private final ParameterNameDiscoverer returnsFooBar = new ParameterNameDiscoverer() {

    @Override
    public String[] getParameterNames(Executable executable) {
      return FOO_BAR;
    }
  };

  private final ParameterNameDiscoverer returnsSomethingElse = new ParameterNameDiscoverer() {
    @Override
    public String[] getParameterNames(Executable executable) {
      return SOMETHING_ELSE;
    }
  };

  private final Method anyMethod;

  public ParameterNameDiscovererTests() throws SecurityException, NoSuchMethodException {
    anyMethod = TestObject.class.getMethod("getAge");
  }

  @Test
  void noParametersDiscoverers() {
    ParameterNameDiscoverer pnd = new CompositeParameterNameDiscoverer();
    assertThat(pnd.getParameterNames(anyMethod)).isNull();
    assertThat(pnd.getParameterNames(null)).isNull();
  }

  @Test
  void orderedParameterDiscoverers1() {
    CompositeParameterNameDiscoverer pnd = new CompositeParameterNameDiscoverer();
    pnd.addDiscoverer(returnsFooBar);
    assertThat(Arrays.equals(FOO_BAR, pnd.getParameterNames(anyMethod))).isTrue();
    assertThat(Arrays.equals(FOO_BAR, pnd.getParameterNames(null))).isTrue();
    pnd.addDiscoverer(returnsSomethingElse);
    assertThat(Arrays.equals(FOO_BAR, pnd.getParameterNames(anyMethod))).isTrue();
    assertThat(Arrays.equals(FOO_BAR, pnd.getParameterNames(null))).isTrue();
  }

  @Test
  void orderedParameterDiscoverers2() {
    CompositeParameterNameDiscoverer pnd = new CompositeParameterNameDiscoverer();
    pnd.addDiscoverer(returnsSomethingElse);
    assertThat(Arrays.equals(SOMETHING_ELSE, pnd.getParameterNames(anyMethod))).isTrue();
    assertThat(Arrays.equals(SOMETHING_ELSE, pnd.getParameterNames(null))).isTrue();
    pnd.addDiscoverer(returnsFooBar);
    assertThat(Arrays.equals(SOMETHING_ELSE, pnd.getParameterNames(anyMethod))).isTrue();
    assertThat(Arrays.equals(SOMETHING_ELSE, pnd.getParameterNames(null))).isTrue();
  }

  public void test(String name, Integer i) {

  }

  @Test
  void test_GetMethodArgsNames() throws NoSuchMethodException, SecurityException {
    final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    String[] methodArgsNames = discoverer.getParameterNames(
            ParameterNameDiscovererTests.class.getMethod("test", String.class, Integer.class));

    assert methodArgsNames.length > 0 : "Can't get Method Args Names";

    assert "name".equals(methodArgsNames[0]) : "Can't get Method Args Names";
    assert "i".equals(methodArgsNames[1]) : "Can't get Method Args Names";
  }

}
