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

package cn.taketoday.aop.aspectj.generic;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AspectJ pointcut expression matching when working with bridge methods.
 *
 * <p>This class focuses on class proxying.
 *
 * <p>See GenericBridgeMethodMatchingTests for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class GenericBridgeMethodMatchingClassProxyTests extends GenericBridgeMethodMatchingTests {

  @Test
  public void testGenericDerivedInterfaceMethodThroughClass() {
    ((DerivedStringParameterizedClass) testBean).genericDerivedInterfaceMethod("");
    assertThat(counterAspect.count).isEqualTo(1);
  }

  @Test
  public void testGenericBaseInterfaceMethodThroughClass() {
    ((DerivedStringParameterizedClass) testBean).genericBaseInterfaceMethod("");
    assertThat(counterAspect.count).isEqualTo(1);
  }

}
