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

package cn.taketoday.retry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Dave Syer
 */
public class AnyThrowTests {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public void testRuntimeException() throws Throwable {
    expected.expect(RuntimeException.class);
    AnyThrow.throwAny(new RuntimeException("planned"));
  }

  @Test
  public void testUncheckedRuntimeException() throws Throwable {
    expected.expect(RuntimeException.class);
    AnyThrow.throwUnchecked(new RuntimeException("planned"));
  }

  @Test
  public void testCheckedException() throws Throwable {
    expected.expect(Exception.class);
    AnyThrow.throwAny(new Exception("planned"));
  }

  private static class AnyThrow {

    private static void throwUnchecked(Throwable e) {
      AnyThrow.<RuntimeException>throwAny(e);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAny(Throwable e) throws E {
      throw (E) e;
    }

  }

}
