/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aop.aspectj;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.CountingTestBean;
import infra.beans.testfixture.beans.IOther;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.beans.testfixture.beans.subpkg.DeepBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for the {@link TypePatternClassFilter} class.
 *
 * @author Rod Johnson
 * @author Rick Evans
 * @author Chris Beams
 * @author Sam Brannen
 */
class TypePatternClassFilterTests {

  @Test
  void nullPattern() {
    assertThatIllegalArgumentException().isThrownBy(() -> new TypePatternClassFilter(null));
  }

  @Test
  void invalidPattern() {
    assertThatIllegalArgumentException().isThrownBy(() -> new TypePatternClassFilter("-"));
  }

  @Test
  void invocationOfMatchesMethodBlowsUpWhenNoTypePatternHasBeenSet() throws Exception {
    assertThatIllegalStateException().isThrownBy(() -> new TypePatternClassFilter().matches(String.class));
  }

  @Test
  void validPatternMatching() {
    TypePatternClassFilter tpcf = new TypePatternClassFilter("infra.beans.testfixture.beans.*");

    assertThat(tpcf.matches(TestBean.class)).as("Must match: in package").isTrue();
    assertThat(tpcf.matches(ITestBean.class)).as("Must match: in package").isTrue();
    assertThat(tpcf.matches(IOther.class)).as("Must match: in package").isTrue();

    assertThat(tpcf.matches(DeepBean.class)).as("Must be excluded: in wrong package").isFalse();
    assertThat(tpcf.matches(BeanFactory.class)).as("Must be excluded: in wrong package").isFalse();
    assertThat(tpcf.matches(StandardBeanFactory.class)).as("Must be excluded: in wrong package").isFalse();
  }

  @Test
  void subclassMatching() {
    TypePatternClassFilter tpcf = new TypePatternClassFilter("infra.beans.testfixture.beans.ITestBean+");

    assertThat(tpcf.matches(TestBean.class)).as("Must match: in package").isTrue();
    assertThat(tpcf.matches(ITestBean.class)).as("Must match: in package").isTrue();
    assertThat(tpcf.matches(CountingTestBean.class)).as("Must match: in package").isTrue();

    assertThat(tpcf.matches(IOther.class)).as("Must be excluded: not subclass").isFalse();
    assertThat(tpcf.matches(StandardBeanFactory.class)).as("Must be excluded: not subclass").isFalse();
  }

  @Test
  void andOrNotReplacement() {
    TypePatternClassFilter tpcf = new TypePatternClassFilter("java.lang.Object or java.lang.String");
    assertThat(tpcf.matches(Number.class)).as("matches Number").isFalse();
    assertThat(tpcf.matches(Object.class)).as("matches Object").isTrue();
    assertThat(tpcf.matches(String.class)).as("matchesString").isTrue();

    tpcf = new TypePatternClassFilter("java.lang.Number+ and java.lang.Float");
    assertThat(tpcf.matches(Float.class)).as("matches Float").isTrue();
    assertThat(tpcf.matches(Double.class)).as("matches Double").isFalse();

    tpcf = new TypePatternClassFilter("java.lang.Number+ and not java.lang.Float");
    assertThat(tpcf.matches(Float.class)).as("matches Float").isFalse();
    assertThat(tpcf.matches(Double.class)).as("matches Double").isTrue();
  }

  @Test
  void testEquals() {
    TypePatternClassFilter filter1 = new TypePatternClassFilter("infra.beans.testfixture.beans.*");
    TypePatternClassFilter filter2 = new TypePatternClassFilter("infra.beans.testfixture.beans.*");
    TypePatternClassFilter filter3 = new TypePatternClassFilter("infra.tests.*");

    assertThat(filter1).isEqualTo(filter2);
    assertThat(filter1).isNotEqualTo(filter3);
  }

  @Test
  void testHashCode() {
    TypePatternClassFilter filter1 = new TypePatternClassFilter("infra.beans.testfixture.beans.*");
    TypePatternClassFilter filter2 = new TypePatternClassFilter("infra.beans.testfixture.beans.*");
    TypePatternClassFilter filter3 = new TypePatternClassFilter("infra.tests.*");

    assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
    assertThat(filter1.hashCode()).isNotEqualTo(filter3.hashCode());
  }

  @Test
  void testToString() {
    TypePatternClassFilter filter1 = new TypePatternClassFilter("infra.beans.testfixture.beans.*");
    TypePatternClassFilter filter2 = new TypePatternClassFilter("infra.beans.testfixture.beans.*");

    assertThat(filter1.toString())
            .isEqualTo("infra.aop.aspectj.TypePatternClassFilter: infra.beans.testfixture.beans.*");
    assertThat(filter1.toString()).isEqualTo(filter2.toString());
  }

}
