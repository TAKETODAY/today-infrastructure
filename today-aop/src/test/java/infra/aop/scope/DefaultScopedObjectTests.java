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

package infra.aop.scope;

import org.junit.jupiter.api.Test;

import infra.beans.factory.config.ConfigurableBeanFactory;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the {@link DefaultScopedObject} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class DefaultScopedObjectTests {

  private static final String GOOD_BEAN_NAME = "foo";

  @Test
  public void testCtorWithNullBeanFactory() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new DefaultScopedObject(null, GOOD_BEAN_NAME));
  }

  @Test
  public void testCtorWithNullTargetBeanName() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            testBadTargetBeanName(null));
  }

  @Test
  public void testCtorWithEmptyTargetBeanName() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            testBadTargetBeanName(""));
  }

  @Test
  public void testCtorWithJustWhitespacedTargetBeanName() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            testBadTargetBeanName("   "));
  }

  private static void testBadTargetBeanName(final String badTargetBeanName) {
    ConfigurableBeanFactory factory = mock(ConfigurableBeanFactory.class);
    new DefaultScopedObject(factory, badTargetBeanName);
  }

}
