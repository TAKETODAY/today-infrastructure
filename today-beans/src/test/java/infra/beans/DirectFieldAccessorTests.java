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

package infra.beans;

import org.junit.jupiter.api.Test;

import infra.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Specific {@link DirectFieldAccessor} tests.
 *
 * @author Jose Luis Martin
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 22:03
 */

class DirectFieldAccessorTests extends AbstractPropertyAccessorTests {

  @Override
  protected DirectFieldAccessor createAccessor(Object target) {
    return new DirectFieldAccessor(target);
  }

  @Test
  void withShadowedField() {
    final StringBuilder sb = new StringBuilder();

    TestBean target = new TestBean() {
      @SuppressWarnings("unused")
      final
      StringBuilder name = sb;
    };

    DirectFieldAccessor dfa = createAccessor(target);
    assertThat(dfa.getPropertyType("name")).isEqualTo(StringBuilder.class);
    assertThat(dfa.getPropertyValue("name")).isEqualTo(sb);
  }

}

