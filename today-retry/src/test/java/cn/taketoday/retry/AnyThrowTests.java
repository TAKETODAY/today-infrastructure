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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 * @author Gary Russell
 */
public class AnyThrowTests {

  @Test
  public void testRuntimeException() {
    assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> AnyThrow.throwAny(new RuntimeException("planned")));
  }

  @Test
  public void testUncheckedRuntimeException() {
    assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> AnyThrow.throwUnchecked(new RuntimeException("planned")));
  }

  @Test
  public void testCheckedException() {
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> AnyThrow.throwAny(new Exception("planned")));
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
