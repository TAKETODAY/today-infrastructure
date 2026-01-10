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

