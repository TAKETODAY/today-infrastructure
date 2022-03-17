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

package cn.taketoday.transaction.aspectj;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class TransactionAspectTests {

  private final CallCountingTransactionManager txManager = new CallCountingTransactionManager();

  private final TransactionalAnnotationOnlyOnClassWithNoInterface annotationOnlyOnClassWithNoInterface =
          new TransactionalAnnotationOnlyOnClassWithNoInterface();

  private final ClassWithProtectedAnnotatedMember beanWithAnnotatedProtectedMethod =
          new ClassWithProtectedAnnotatedMember();

  private final ClassWithPrivateAnnotatedMember beanWithAnnotatedPrivateMethod =
          new ClassWithPrivateAnnotatedMember();

  private final MethodAnnotationOnClassWithNoInterface methodAnnotationOnly =
          new MethodAnnotationOnClassWithNoInterface();

  @BeforeEach
  public void initContext() {
    AnnotationTransactionAspect.aspectOf().setTransactionManager(txManager);
  }

  @Test
  public void testCommitOnAnnotatedClass() throws Throwable {
    txManager.clear();
    assertThat(txManager.begun).isEqualTo(0);
    annotationOnlyOnClassWithNoInterface.echo(null);
    assertThat(txManager.commits).isEqualTo(1);
  }

  @Test
  public void commitOnAnnotatedProtectedMethod() throws Throwable {
    txManager.clear();
    assertThat(txManager.begun).isEqualTo(0);
    beanWithAnnotatedProtectedMethod.doInTransaction();
    assertThat(txManager.commits).isEqualTo(1);
  }

  @Test
  public void commitOnAnnotatedPrivateMethod() throws Throwable {
    txManager.clear();
    assertThat(txManager.begun).isEqualTo(0);
    beanWithAnnotatedPrivateMethod.doSomething();
    assertThat(txManager.commits).isEqualTo(1);
  }

  @Test
  public void commitOnNonAnnotatedNonPublicMethodInTransactionalType() throws Throwable {
    txManager.clear();
    assertThat(txManager.begun).isEqualTo(0);
    annotationOnlyOnClassWithNoInterface.nonTransactionalMethod();
    assertThat(txManager.begun).isEqualTo(0);
  }

  @Test
  public void commitOnAnnotatedMethod() throws Throwable {
    txManager.clear();
    assertThat(txManager.begun).isEqualTo(0);
    methodAnnotationOnly.echo(null);
    assertThat(txManager.commits).isEqualTo(1);
  }

  @Test
  public void notTransactional() throws Throwable {
    txManager.clear();
    assertThat(txManager.begun).isEqualTo(0);
    new NotTransactional().noop();
    assertThat(txManager.begun).isEqualTo(0);
  }

  @Test
  public void defaultCommitOnAnnotatedClass() throws Throwable {
    Exception ex = new Exception();
    assertThatExceptionOfType(Exception.class).isThrownBy(() ->
                    testRollback(() -> annotationOnlyOnClassWithNoInterface.echo(ex), false))
            .isSameAs(ex);
  }

  @Test
  public void defaultRollbackOnAnnotatedClass() throws Throwable {
    RuntimeException ex = new RuntimeException();
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
                    testRollback(() -> annotationOnlyOnClassWithNoInterface.echo(ex), true))
            .isSameAs(ex);
  }

  @Test
  public void defaultCommitOnSubclassOfAnnotatedClass() throws Throwable {
    Exception ex = new Exception();
    assertThatExceptionOfType(Exception.class).isThrownBy(() ->
                    testRollback(() -> new SubclassOfClassWithTransactionalAnnotation().echo(ex), false))
            .isSameAs(ex);
  }

  @Test
  public void defaultCommitOnSubclassOfClassWithTransactionalMethodAnnotated() throws Throwable {
    Exception ex = new Exception();
    assertThatExceptionOfType(Exception.class).isThrownBy(() ->
                    testRollback(() -> new SubclassOfClassWithTransactionalMethodAnnotation().echo(ex), false))
            .isSameAs(ex);
  }

  @Test
  public void noCommitOnImplementationOfAnnotatedInterface() throws Throwable {
    final Exception ex = new Exception();
    testNotTransactional(() -> new ImplementsAnnotatedInterface().echo(ex), ex);
  }

  @Test
  public void noRollbackOnImplementationOfAnnotatedInterface() throws Throwable {
    final Exception rollbackProvokingException = new RuntimeException();
    testNotTransactional(() -> new ImplementsAnnotatedInterface().echo(rollbackProvokingException),
            rollbackProvokingException);
  }

  protected void testRollback(TransactionOperationCallback toc, boolean rollback) throws Throwable {
    txManager.clear();
    assertThat(txManager.begun).isEqualTo(0);
    try {
      toc.performTransactionalOperation();
    }
    finally {
      assertThat(txManager.begun).isEqualTo(1);
      long expected1 = rollback ? 0 : 1;
      assertThat(txManager.commits).isEqualTo(expected1);
      long expected = rollback ? 1 : 0;
      assertThat(txManager.rollbacks).isEqualTo(expected);
    }
  }

  protected void testNotTransactional(TransactionOperationCallback toc, Throwable expected) throws Throwable {
    txManager.clear();
    assertThat(txManager.begun).isEqualTo(0);
    assertThatExceptionOfType(Throwable.class).isThrownBy(
            toc::performTransactionalOperation).isSameAs(expected);
    assertThat(txManager.begun).isEqualTo(0);
  }

  private interface TransactionOperationCallback {

    Object performTransactionalOperation() throws Throwable;
  }

  public static class SubclassOfClassWithTransactionalAnnotation extends TransactionalAnnotationOnlyOnClassWithNoInterface {
  }

  public static class SubclassOfClassWithTransactionalMethodAnnotation extends MethodAnnotationOnClassWithNoInterface {
  }

  public static class ImplementsAnnotatedInterface implements ITransactional {

    @Override
    public Object echo(Throwable t) throws Throwable {
      if (t != null) {
        throw t;
      }
      return t;
    }
  }

  public static class NotTransactional {

    public void noop() {
    }
  }

}
