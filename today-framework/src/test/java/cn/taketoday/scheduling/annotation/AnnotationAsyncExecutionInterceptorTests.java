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

package cn.taketoday.scheduling.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AnnotationAsyncExecutionInterceptor}.
 *
 * @author Chris Beams
 * @since 4.0
 */
public class AnnotationAsyncExecutionInterceptorTests {

  @Test
  @SuppressWarnings("unused")
  public void testGetExecutorQualifier() throws SecurityException, NoSuchMethodException {
    AnnotationAsyncExecutionInterceptor i = new AnnotationAsyncExecutionInterceptor(null);
    { // method level
      class C {
        @Async("qMethod")
        void m() { }
      }
      assertThat(i.getExecutorQualifier(C.class.getDeclaredMethod("m"))).isEqualTo("qMethod");
    }
    { // class level
      @Async("qClass")
      class C {
        void m() { }
      }
      assertThat(i.getExecutorQualifier(C.class.getDeclaredMethod("m"))).isEqualTo("qClass");
    }
    { // method and class level -> method value overrides
      @Async("qClass")
      class C {
        @Async("qMethod")
        void m() { }
      }
      assertThat(i.getExecutorQualifier(C.class.getDeclaredMethod("m"))).isEqualTo("qMethod");
    }
    { // method and class level -> method value, even if empty, overrides
      @Async("qClass")
      class C {
        @Async
        void m() { }
      }
      assertThat(i.getExecutorQualifier(C.class.getDeclaredMethod("m"))).isEqualTo("");
    }
    { // meta annotation with qualifier
      @MyAsync
      class C {
        void m() { }
      }
      assertThat(i.getExecutorQualifier(C.class.getDeclaredMethod("m"))).isEqualTo("qMeta");
    }
  }

  @Async("qMeta")
  @Retention(RetentionPolicy.RUNTIME)
  @interface MyAsync { }
}
