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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ScopedProxyUtils}.
 *
 * @author Sam Brannen
 */
class ScopedProxyUtilsTests {

  @Test
  void getTargetBeanNameAndIsScopedTarget() {
    String originalBeanName = "myBean";
    String targetBeanName = ScopedProxyUtils.getTargetBeanName(originalBeanName);

    assertThat(targetBeanName).isNotEqualTo(originalBeanName).endsWith(originalBeanName);
    assertThat(ScopedProxyUtils.isScopedTarget(targetBeanName)).isTrue();
    assertThat(ScopedProxyUtils.isScopedTarget(originalBeanName)).isFalse();
  }

  @Test
  void getOriginalBeanNameAndIsScopedTarget() {
    String originalBeanName = "myBean";
    String targetBeanName = ScopedProxyUtils.getTargetBeanName(originalBeanName);
    String parsedOriginalBeanName = ScopedProxyUtils.getOriginalBeanName(targetBeanName);

    assertThat(parsedOriginalBeanName).isNotEqualTo(targetBeanName).isEqualTo(originalBeanName);
    assertThat(ScopedProxyUtils.isScopedTarget(targetBeanName)).isTrue();
    assertThat(ScopedProxyUtils.isScopedTarget(parsedOriginalBeanName)).isFalse();
  }

  @Test
  void getOriginalBeanNameForNullTargetBean() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ScopedProxyUtils.getOriginalBeanName(null))
            .withMessage("bean name 'null' does not refer to the target of a scoped proxy");
  }

  @Test
  void getOriginalBeanNameForNonScopedTarget() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ScopedProxyUtils.getOriginalBeanName("myBean"))
            .withMessage("bean name 'myBean' does not refer to the target of a scoped proxy");
  }

}
