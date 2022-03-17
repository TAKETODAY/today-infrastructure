/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.bytecode.beans;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author baliuka
 */
public class TestImmutableBean {

  public void testSimple() {
    MA bean = new MA();
    assertTrue(bean.getIntP() == 0);
    bean.setIntP(42);
    assertTrue(bean.getIntP() == 42);
    bean = (MA) ImmutableBean.create(bean);
    assertTrue(bean.getIntP() == 42);
    try {
      bean.setIntP(43);
      fail("expecting illegal state exception");
    }
    catch (IllegalStateException ignore) { }
  }

}
