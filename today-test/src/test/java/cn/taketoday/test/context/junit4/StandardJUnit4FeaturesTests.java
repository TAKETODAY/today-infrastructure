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

package cn.taketoday.test.context.junit4;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Simple unit test to verify the expected functionality of standard JUnit 4.4+
 * testing features.
 * <p>
 * Currently testing: {@link Test @Test} (including expected exceptions and
 * timeouts), {@link BeforeClass @BeforeClass}, {@link Before @Before}, and
 * <em>assumptions</em>.
 * </p>
 * <p>
 * Due to the fact that JUnit does not guarantee a particular ordering of test
 * method execution, the following are currently not tested:
 * {@link org.junit.AfterClass @AfterClass} and {@link org.junit.After @After}.
 * </p>
 *
 * @author Sam Brannen
 * @see StandardJUnit4FeaturesInfraRunnerTests
 * @since 4.0
 */
public class StandardJUnit4FeaturesTests {

  private static int staticBeforeCounter = 0;

  @BeforeClass
  public static void incrementStaticBeforeCounter() {
    StandardJUnit4FeaturesTests.staticBeforeCounter++;
  }

  private int beforeCounter = 0;

  @Test
  @Ignore
  public void alwaysFailsButShouldBeIgnored() {
    fail("The body of an ignored test should never be executed!");
  }

  @Test
  public void alwaysSucceeds() {
    assertThat(true).isTrue();
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void expectingAnIndexOutOfBoundsException() {
    new ArrayList<>().get(1);
  }

  @Test
  public void failedAssumptionShouldPrecludeImminentFailure() {
    assumeTrue(false);
    fail("A failed assumption should preclude imminent failure!");
  }

  @Before
  public void incrementBeforeCounter() {
    this.beforeCounter++;
  }

  @Test(timeout = 10000)
  public void noOpShouldNotTimeOut() {
    /* no-op */
  }

  @Test
  public void verifyBeforeAnnotation() {
    assertThat(this.beforeCounter).isEqualTo(1);
  }

  @Test
  public void verifyBeforeClassAnnotation() {
    // Instead of testing for equality to 1, we just assert that the value
    // was incremented at least once, since this test class may serve as a
    // parent class to other tests in a suite, etc.
    assertThat(StandardJUnit4FeaturesTests.staticBeforeCounter > 0).isTrue();
  }

}
