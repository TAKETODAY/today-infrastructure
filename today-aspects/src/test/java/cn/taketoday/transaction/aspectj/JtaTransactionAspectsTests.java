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

import java.io.IOException;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.junit.jupiter.SpringJUnitConfig;
import cn.taketoday.transaction.testfixture.CallCountingTransactionManager;
import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * @author Stephane Nicoll
 */
@SpringJUnitConfig(JtaTransactionAspectsTests.Config.class)
public class JtaTransactionAspectsTests {

  @Autowired
  private CallCountingTransactionManager txManager;

  @BeforeEach
  public void setUp() {
    this.txManager.clear();
  }

  @Test
  public void commitOnAnnotatedPublicMethod() throws Throwable {
    assertThat(this.txManager.begun).isEqualTo(0);
    new JtaAnnotationPublicAnnotatedMember().echo(null);
    assertThat(this.txManager.commits).isEqualTo(1);
  }

  @Test
  public void matchingRollbackOnApplied() throws Throwable {
    assertThat(this.txManager.begun).isEqualTo(0);
    InterruptedException test = new InterruptedException();
    assertThatExceptionOfType(InterruptedException.class).isThrownBy(() ->
                    new JtaAnnotationPublicAnnotatedMember().echo(test))
            .isSameAs(test);
    assertThat(this.txManager.rollbacks).isEqualTo(1);
    assertThat(this.txManager.commits).isEqualTo(0);
  }

  @Test
  public void nonMatchingRollbackOnApplied() throws Throwable {
    assertThat(this.txManager.begun).isEqualTo(0);
    IOException test = new IOException();
    assertThatIOException().isThrownBy(() ->
                    new JtaAnnotationPublicAnnotatedMember().echo(test))
            .isSameAs(test);
    assertThat(this.txManager.commits).isEqualTo(1);
    assertThat(this.txManager.rollbacks).isEqualTo(0);
  }

  @Test
  public void commitOnAnnotatedProtectedMethod() {
    assertThat(this.txManager.begun).isEqualTo(0);
    new JtaAnnotationProtectedAnnotatedMember().doInTransaction();
    assertThat(this.txManager.commits).isEqualTo(1);
  }

  @Test
  public void nonAnnotatedMethodCallingProtectedMethod() {
    assertThat(this.txManager.begun).isEqualTo(0);
    new JtaAnnotationProtectedAnnotatedMember().doSomething();
    assertThat(this.txManager.commits).isEqualTo(1);
  }

  @Test
  public void commitOnAnnotatedPrivateMethod() {
    assertThat(this.txManager.begun).isEqualTo(0);
    new JtaAnnotationPrivateAnnotatedMember().doInTransaction();
    assertThat(this.txManager.commits).isEqualTo(1);
  }

  @Test
  public void nonAnnotatedMethodCallingPrivateMethod() {
    assertThat(this.txManager.begun).isEqualTo(0);
    new JtaAnnotationPrivateAnnotatedMember().doSomething();
    assertThat(this.txManager.commits).isEqualTo(1);
  }

  @Test
  public void notTransactional() {
    assertThat(this.txManager.begun).isEqualTo(0);
    new TransactionAspectTests.NotTransactional().noop();
    assertThat(this.txManager.begun).isEqualTo(0);
  }

  public static class JtaAnnotationPublicAnnotatedMember {

    @Transactional(rollbackOn = InterruptedException.class)
    public void echo(Throwable t) throws Throwable {
      if (t != null) {
        throw t;
      }
    }

  }

  protected static class JtaAnnotationProtectedAnnotatedMember {

    public void doSomething() {
      doInTransaction();
    }

    @Transactional
    protected void doInTransaction() {
    }
  }

  protected static class JtaAnnotationPrivateAnnotatedMember {

    public void doSomething() {
      doInTransaction();
    }

    @Transactional
    private void doInTransaction() {
    }
  }

  @Configuration
  protected static class Config {

    @Bean
    public CallCountingTransactionManager transactionManager() {
      return new CallCountingTransactionManager();
    }

    @Bean
    public JtaAnnotationTransactionAspect transactionAspect() {
      JtaAnnotationTransactionAspect aspect = JtaAnnotationTransactionAspect.aspectOf();
      aspect.setTransactionManager(transactionManager());
      return aspect;
    }
  }

}
