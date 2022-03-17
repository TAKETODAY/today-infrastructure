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

package cn.taketoday.transaction.interceptor;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.beans.BeansException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link RollbackRuleAttribute} class.
 *
 * @author Rod Johnson
 * @author Rick Evans
 * @author Chris Beams
 * @author Sam Brannen
 * @since 4.0
 */
public class RollbackRuleTests {

  @Test
  public void foundImmediatelyWithString() {
    RollbackRuleAttribute rr = new RollbackRuleAttribute(Exception.class.getName());
    assertThat(rr.getDepth(new Exception())).isEqualTo(0);
  }

  @Test
  public void foundImmediatelyWithClass() {
    RollbackRuleAttribute rr = new RollbackRuleAttribute(Exception.class);
    assertThat(rr.getDepth(new Exception())).isEqualTo(0);
  }

  @Test
  public void notFound() {
    RollbackRuleAttribute rr = new RollbackRuleAttribute(IOException.class.getName());
    assertThat(rr.getDepth(new MyRuntimeException(""))).isEqualTo(-1);
  }

  @Test
  public void ancestry() {
    RollbackRuleAttribute rr = new RollbackRuleAttribute(Exception.class.getName());
    // Exception -> Runtime -> NestedRuntime -> MyRuntimeException
    assertThat(rr.getDepth(new MyRuntimeException(""))).isEqualTo(3);
  }

  @Test
  public void alwaysTrueForThrowable() {
    RollbackRuleAttribute rr = new RollbackRuleAttribute(Throwable.class.getName());
    assertThat(rr.getDepth(new MyRuntimeException("")) > 0).isTrue();
    assertThat(rr.getDepth(new IOException()) > 0).isTrue();
    assertThat(rr.getDepth(new BeansException(null, null)) > 0).isTrue();
    assertThat(rr.getDepth(new RuntimeException()) > 0).isTrue();
  }

  @Test
  public void ctorArgMustBeAThrowableClassWithNonThrowableType() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new RollbackRuleAttribute(StringBuffer.class));
  }

  @Test
  public void ctorArgMustBeAThrowableClassWithNullThrowableType() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new RollbackRuleAttribute((Class<?>) null));
  }

  @Test
  public void ctorArgExceptionStringNameVersionWithNull() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new RollbackRuleAttribute((String) null));
  }

  @Test
  public void foundEnclosedExceptionWithEnclosingException() {
    RollbackRuleAttribute rr = new RollbackRuleAttribute(EnclosingException.class);
    assertThat(rr.getDepth(new EnclosingException.EnclosedException())).isEqualTo(0);
  }

  @SuppressWarnings("serial")
  static class EnclosingException extends RuntimeException {

    @SuppressWarnings("serial")
    static class EnclosedException extends RuntimeException {

    }
  }

}
