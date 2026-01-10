/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.transaction.interceptor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.beans.FatalBeanException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 15:31
 */
class RollbackRuleAttributeTests {

  @Test
  void constructorWithExceptionType() {
    RollbackRuleAttribute rule = new RollbackRuleAttribute(RuntimeException.class);
    assertThat(rule).isNotNull();
    assertThat(rule.getExceptionName()).isEqualTo(RuntimeException.class.getName());
  }

  @Test
  void constructorWithExceptionPattern() {
    String exceptionPattern = "java.lang.RuntimeException";
    RollbackRuleAttribute rule = new RollbackRuleAttribute(exceptionPattern);
    assertThat(rule).isNotNull();
    assertThat(rule.getExceptionName()).isEqualTo(exceptionPattern);
  }

  @Test
  void getExceptionNameReturnsPattern() {
    String exceptionPattern = "TestException";
    RollbackRuleAttribute rule = new RollbackRuleAttribute(exceptionPattern);
    assertThat(rule.getExceptionName()).isEqualTo(exceptionPattern);
  }

  @Test
  void equalsWithSameInstance() {
    RollbackRuleAttribute rule = new RollbackRuleAttribute(RuntimeException.class);
    assertThat(rule.equals(rule)).isTrue();
  }

  @Test
  void equalsWithDifferentType() {
    RollbackRuleAttribute rule = new RollbackRuleAttribute(RuntimeException.class);
    assertThat(rule.equals("different")).isFalse();
  }

  @Test
  void equalsWithNull() {
    RollbackRuleAttribute rule = new RollbackRuleAttribute(RuntimeException.class);
    assertThat(rule.equals(null)).isFalse();
  }

  @Test
  void equalsWithSamePattern() {
    RollbackRuleAttribute rule1 = new RollbackRuleAttribute("TestException");
    RollbackRuleAttribute rule2 = new RollbackRuleAttribute("TestException");
    assertThat(rule1.equals(rule2)).isTrue();
  }

  @Test
  void equalsWithDifferentPattern() {
    RollbackRuleAttribute rule1 = new RollbackRuleAttribute("TestException1");
    RollbackRuleAttribute rule2 = new RollbackRuleAttribute("TestException2");
    assertThat(rule1.equals(rule2)).isFalse();
  }

  @Test
  void hashCodeReturnsPatternHashCode() {
    String exceptionPattern = "TestException";
    RollbackRuleAttribute rule = new RollbackRuleAttribute(exceptionPattern);
    assertThat(rule.hashCode()).isEqualTo(exceptionPattern.hashCode());
  }

  @Test
  void toStringReturnsFormattedString() {
    RollbackRuleAttribute rule = new RollbackRuleAttribute(RuntimeException.class);
    assertThat(rule.toString()).isEqualTo("RollbackRuleAttribute with pattern [" + RuntimeException.class.getName() + "]");
  }

  @Test
  void getDepthWithExactMatchUsingExceptionType() {
    RollbackRuleAttribute rule = new RollbackRuleAttribute(Exception.class);
    Exception exception = new Exception();
    assertThat(rule.getDepth(exception)).isEqualTo(0);
  }

  @Test
  void getDepthWithSuperclassMatchUsingExceptionType() {
    RollbackRuleAttribute rule = new RollbackRuleAttribute(Exception.class);
    RuntimeException exception = new RuntimeException();
    assertThat(rule.getDepth(exception)).isEqualTo(1);
  }

  @Test
  void getDepthWithNoMatchUsingExceptionType() {
    RollbackRuleAttribute rule = new RollbackRuleAttribute(RuntimeException.class);
    Exception exception = new Exception();
    assertThat(rule.getDepth(exception)).isEqualTo(-1);
  }

  @Test
  void getDepthWithExactMatchUsingExceptionPattern() {
    RollbackRuleAttribute rule = new RollbackRuleAttribute("java.lang.Exception");
    Exception exception = new Exception();
    assertThat(rule.getDepth(exception)).isEqualTo(0);
  }

  @Test
  void getDepthWithPatternMatchUsingExceptionPattern() {
    RollbackRuleAttribute rule = new RollbackRuleAttribute("Exception");
    RuntimeException exception = new RuntimeException();
    assertThat(rule.getDepth(exception)).isEqualTo(0);
  }

  @Test
  void getDepthWithNoMatchUsingExceptionPattern() {
    RollbackRuleAttribute rule = new RollbackRuleAttribute("java.lang.RuntimeException");
    Exception exception = new Exception();
    assertThat(rule.getDepth(exception)).isEqualTo(-1);
  }

  @Test
  void rollbackOnAllExceptionsConstant() {
    assertThat(RollbackRuleAttribute.ROLLBACK_ON_ALL_EXCEPTIONS).isNotNull();
    assertThat(RollbackRuleAttribute.ROLLBACK_ON_ALL_EXCEPTIONS.getExceptionName()).isEqualTo(Exception.class.getName());
  }

  @Test
  void rollbackOnRuntimeExceptionsConstant() {
    assertThat(RollbackRuleAttribute.ROLLBACK_ON_RUNTIME_EXCEPTIONS).isNotNull();
    assertThat(RollbackRuleAttribute.ROLLBACK_ON_RUNTIME_EXCEPTIONS.getExceptionName()).isEqualTo(RuntimeException.class.getName());
  }

  @Nested
  class ExceptionPatternTests {

    @Test
    void constructorPreconditions() {
      assertThatIllegalArgumentException().isThrownBy(() -> new RollbackRuleAttribute((String) null));
    }

    @Test
    void notFound() {
      RollbackRuleAttribute rr = new RollbackRuleAttribute(IOException.class.getName());
      assertThat(rr.getDepth(new MyRuntimeException())).isEqualTo(-1);
    }

    @Test
    void foundImmediatelyWhenDirectMatch() {
      RollbackRuleAttribute rr = new RollbackRuleAttribute(Exception.class.getName());
      assertThat(rr.getDepth(new Exception())).isEqualTo(0);
    }

    @Test
    void foundImmediatelyWhenExceptionThrownIsNestedTypeOfRegisteredException() {
      RollbackRuleAttribute rr = new RollbackRuleAttribute(EnclosingException.class.getName());
      assertThat(rr.getDepth(new EnclosingException.NestedException())).isEqualTo(0);
    }

    @Test
    void foundImmediatelyWhenNameOfExceptionThrownStartsWithNameOfRegisteredException() {
      // Precondition for this use case.
      assertThat(MyException.class.isAssignableFrom(MyException2.class)).isFalse();

      RollbackRuleAttribute rr = new RollbackRuleAttribute(MyException.class.getName());
      assertThat(rr.getDepth(new MyException2())).isEqualTo(0);
    }

    @Test
    void foundInSuperclassHierarchy() {
      RollbackRuleAttribute rr = new RollbackRuleAttribute(Exception.class.getName());
      // Exception -> RuntimeException -> NestedRuntimeException -> MyRuntimeException
      assertThat(rr.getDepth(new MyRuntimeException())).isEqualTo(3);
    }

    @Test
    void alwaysFoundForThrowable() {
      RollbackRuleAttribute rr = new RollbackRuleAttribute(Throwable.class.getName());
      assertThat(rr.getDepth(new MyRuntimeException())).isGreaterThan(0);
      assertThat(rr.getDepth(new IOException())).isGreaterThan(0);
      assertThat(rr.getDepth(new FatalBeanException(null, null))).isGreaterThan(0);
      assertThat(rr.getDepth(new RuntimeException())).isGreaterThan(0);
    }

  }

  @Nested
  class ExceptionTypeTests {

    @Test
    void constructorPreconditions() {
      assertThatIllegalArgumentException().isThrownBy(() -> new RollbackRuleAttribute(Object.class));
      assertThatIllegalArgumentException().isThrownBy(() -> new RollbackRuleAttribute((Class<?>) null));
    }

    @Test
    void notFound() {
      RollbackRuleAttribute rr = new RollbackRuleAttribute(IOException.class);
      assertThat(rr.getDepth(new MyRuntimeException())).isEqualTo(-1);
    }

    @Test
    void notFoundWhenNameOfExceptionThrownStartsWithNameOfRegisteredException() {
      // Precondition for this use case.
      assertThat(MyException.class.isAssignableFrom(MyException2.class)).isFalse();

      RollbackRuleAttribute rr = new RollbackRuleAttribute(MyException.class);
      assertThat(rr.getDepth(new MyException2())).isEqualTo(-1);
    }

    @Test
    void notFoundWhenExceptionThrownIsNestedTypeOfRegisteredException() {
      RollbackRuleAttribute rr = new RollbackRuleAttribute(EnclosingException.class);
      assertThat(rr.getDepth(new EnclosingException.NestedException())).isEqualTo(-1);
    }

    @Test
    void foundImmediatelyWhenDirectMatch() {
      RollbackRuleAttribute rr = new RollbackRuleAttribute(Exception.class);
      assertThat(rr.getDepth(new Exception())).isEqualTo(0);
    }

    @Test
    void foundInSuperclassHierarchy() {
      RollbackRuleAttribute rr = new RollbackRuleAttribute(Exception.class);
      // Exception -> RuntimeException -> NestedRuntimeException -> MyRuntimeException
      assertThat(rr.getDepth(new MyRuntimeException())).isEqualTo(3);
    }

    @Test
    void alwaysFoundForThrowable() {
      RollbackRuleAttribute rr = new RollbackRuleAttribute(Throwable.class);
      assertThat(rr.getDepth(new MyRuntimeException())).isGreaterThan(0);
      assertThat(rr.getDepth(new IOException())).isGreaterThan(0);
      assertThat(rr.getDepth(new FatalBeanException(null, null))).isGreaterThan(0);
      assertThat(rr.getDepth(new RuntimeException())).isGreaterThan(0);
    }

  }

  @SuppressWarnings("serial")
  static class EnclosingException extends RuntimeException {

    @SuppressWarnings("serial")
    static class NestedException extends RuntimeException {
    }
  }

  @SuppressWarnings("serial")
  static class MyException extends RuntimeException {
  }

  // Name intentionally starts with MyException (including package) but does
  // NOT extend MyException.
  @SuppressWarnings("serial")
  static class MyException2 extends RuntimeException {
  }

}
