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

package infra.aot.generate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MethodName}.
 *
 * @author Phillip Webb
 */
class MethodNameTests {

  @Test
  void ofWhenPartsIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> MethodName.of((String[]) null))
            .withMessage("'parts' is required");
  }

  @Test
  void ofReturnsMethodName() {
    assertThat(MethodName.of("get", "bean", "factory")).hasToString("getBeanFactory");
    assertThat(MethodName.of("get", null, "factory")).hasToString("getFactory");
    assertThat(MethodName.of(null, null)).hasToString("$$aot");
    assertThat(MethodName.of("", null)).hasToString("$$aot");
    assertThat(MethodName.of("get", "InputStream")).hasToString("getInputStream");
    assertThat(MethodName.of("register", "myBean123", "bean")).hasToString("registerMyBeanBean");
    assertThat(MethodName.of("register", "infra.example.bean"))
            .hasToString("registerInfraExampleBean");
  }

  @Test
  void andPartsWhenPartsIsNullThrowsException() {
    MethodName name = MethodName.of("myBean");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> name.and(((String[]) null)))
            .withMessage("'parts' is required");
  }

  @Test
  void andPartsReturnsMethodName() {
    MethodName name = MethodName.of("myBean");
    assertThat(name.and("test")).hasToString("myBeanTest");
    assertThat(name.and("test", null)).hasToString("myBeanTest");
    assertThat(name.and("getName")).hasToString("getMyBeanName");
    assertThat(name.and("setName")).hasToString("setMyBeanName");
    assertThat(name.and("isDoingOk")).hasToString("isMyBeanDoingOk");
    assertThat(name.and("this", "that", "the", "other")).hasToString("myBeanThisThatTheOther");
  }

  @Test
  void andNameWhenPartsIsNullThrowsException() {
    MethodName name = MethodName.of("myBean");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> name.and(((MethodName) null)))
            .withMessage("'name' is required");
  }

  @Test
  void andNameReturnsMethodName() {
    MethodName name = MethodName.of("myBean");
    assertThat(name.and(MethodName.of("test"))).hasToString("myBeanTest");
  }

  @Test
  void hashCodeAndEquals() {
    MethodName name1 = MethodName.of("myBean");
    MethodName name2 = MethodName.of("my", "bean");
    MethodName name3 = MethodName.of("myOtherBean");
    assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
    assertThat(name1).isEqualTo(name1).isEqualTo(name2).isNotEqualTo(name3);
  }

}
