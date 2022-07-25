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

package cn.taketoday.aop.aspectj;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class AfterThrowingAdviceBindingTests {

  private ITestBean testBean;

  private AfterThrowingAdviceBindingTestAspect afterThrowingAdviceAspect;

  private AfterThrowingAdviceBindingTestAspect.AfterThrowingAdviceBindingCollaborator mockCollaborator;

  @BeforeEach
  public void setup() {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

    testBean = (ITestBean) ctx.getBean("testBean");
    afterThrowingAdviceAspect = (AfterThrowingAdviceBindingTestAspect) ctx.getBean("testAspect");

    mockCollaborator = mock(AfterThrowingAdviceBindingTestAspect.AfterThrowingAdviceBindingCollaborator.class);
    afterThrowingAdviceAspect.setCollaborator(mockCollaborator);
  }

  @Test
  public void testSimpleAfterThrowing() throws Throwable {
    assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
            this.testBean.exceptional(new Throwable()));
    verify(mockCollaborator).noArgs();
  }

  @Test
  public void testAfterThrowingWithBinding() throws Throwable {
    Throwable t = new Throwable();
    assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
            this.testBean.exceptional(t));
    verify(mockCollaborator).oneThrowable(t);
  }

  @Test
  public void testAfterThrowingWithNamedTypeRestriction() throws Throwable {
    Throwable t = new Throwable();
    assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
            this.testBean.exceptional(t));
    verify(mockCollaborator).noArgs();
    verify(mockCollaborator).oneThrowable(t);
    verify(mockCollaborator).noArgsOnThrowableMatch();
  }

  @Test
  public void testAfterThrowingWithRuntimeExceptionBinding() throws Throwable {
    RuntimeException ex = new RuntimeException();
    assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
            this.testBean.exceptional(ex));
    verify(mockCollaborator).oneRuntimeException(ex);
  }

  @Test
  public void testAfterThrowingWithTypeSpecified() throws Throwable {
    assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
            this.testBean.exceptional(new Throwable()));
    verify(mockCollaborator).noArgsOnThrowableMatch();
  }

  @Test
  public void testAfterThrowingWithRuntimeTypeSpecified() throws Throwable {
    assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
            this.testBean.exceptional(new RuntimeException()));
    verify(mockCollaborator).noArgsOnRuntimeExceptionMatch();
  }

}

final class AfterThrowingAdviceBindingTestAspect {

  // collaborator interface that makes it easy to test this aspect is
  // working as expected through mocking.
  public interface AfterThrowingAdviceBindingCollaborator {
    void noArgs();

    void oneThrowable(Throwable t);

    void oneRuntimeException(RuntimeException re);

    void noArgsOnThrowableMatch();

    void noArgsOnRuntimeExceptionMatch();
  }

  protected AfterThrowingAdviceBindingCollaborator collaborator = null;

  public void setCollaborator(AfterThrowingAdviceBindingCollaborator aCollaborator) {
    this.collaborator = aCollaborator;
  }

  public void noArgs() {
    this.collaborator.noArgs();
  }

  public void oneThrowable(Throwable t) {
    this.collaborator.oneThrowable(t);
  }

  public void oneRuntimeException(RuntimeException ex) {
    this.collaborator.oneRuntimeException(ex);
  }

  public void noArgsOnThrowableMatch() {
    this.collaborator.noArgsOnThrowableMatch();
  }

  public void noArgsOnRuntimeExceptionMatch() {
    this.collaborator.noArgsOnRuntimeExceptionMatch();
  }
}
