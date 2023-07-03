/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.env;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.env.SystemEnvironmentPropertySource;
import cn.taketoday.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link RandomValuePropertySource}.
 *
 * @author Dave Syer
 * @author Matt Benson
 */
class RandomValuePropertySourceTests {

  private final RandomValuePropertySource source = new RandomValuePropertySource();

  @Test
  void getPropertyWhenNotRandomReturnsNull() {
    assertThat(this.source.getProperty("foo")).isNull();
  }

  @Test
  void getPropertyWhenStringReturnsValue() {
    assertThat(this.source.getProperty("random.string")).isNotNull();
  }

  @Test
  void getPropertyWhenIntReturnsValue() {
    Integer value = (Integer) this.source.getProperty("random.int");
    assertThat(value).isNotNull();
  }

  @Test
  void getPropertyWhenUuidReturnsValue() {
    String value = (String) this.source.getProperty("random.uuid");
    assertThat(value).isNotNull();
    assertThat(UUID.fromString(value)).isNotNull();
  }

  @Test
  void getPropertyWhenIntRangeReturnsValue() {
    Integer value = (Integer) this.source.getProperty("random.int[4,10]");
    assertThat(value).isNotNull();
    assertThat(value).isGreaterThanOrEqualTo(4);
    assertThat(value).isLessThan(10);
  }

  @Test
  void intRangeWhenLowerBoundEqualsUpperBoundShouldFailWithIllegalArgumentException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.source.getProperty("random.int[4,4]"))
            .withMessage("Lower bound must be less than upper bound.");
  }

  @Test
  void intRangeWhenLowerBoundNegative() {
    Integer value = (Integer) this.source.getProperty("random.int[-4,4]");
    assertThat(value).isGreaterThanOrEqualTo(-4);
    assertThat(value).isLessThan(4);
  }

  @Test
  void getPropertyWhenIntMaxReturnsValue() {
    Integer value = (Integer) this.source.getProperty("random.int(10)");
    assertThat(value).isNotNull().isLessThan(10);
  }

  @Test
  void intMaxZero() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.source.getProperty("random.int(0)"))
            .withMessage("Bound must be positive.");
  }

  @Test
  void intNegativeBound() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.source.getProperty("random.int(-5)"))
            .withMessage("Bound must be positive.");
  }

  @Test
  void getPropertyWhenLongReturnsValue() {
    Long value = (Long) this.source.getProperty("random.long");
    assertThat(value).isNotNull();
  }

  @Test
  void getPropertyWhenLongRangeReturnsValue() {
    Long value = (Long) this.source.getProperty("random.long[4,10]");
    assertThat(value).isNotNull().isBetween(4L, 10L);
  }

  @Test
  void longRangeWhenLowerBoundEqualsUpperBoundShouldFailWithIllegalArgumentException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.source.getProperty("random.long[4,4]"))
            .withMessage("Lower bound must be less than upper bound.");
  }

  @Test
  void longRangeWhenLowerBoundNegative() {
    Long value = (Long) this.source.getProperty("random.long[-4,4]");
    assertThat(value).isGreaterThanOrEqualTo(-4);
    assertThat(value).isLessThan(4);
  }

  @Test
  void getPropertyWhenLongMaxReturnsValue() {
    Long value = (Long) this.source.getProperty("random.long(10)");
    assertThat(value).isNotNull().isLessThan(10L);
  }

  @Test
  void longMaxZero() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.source.getProperty("random.long(0)"))
            .withMessage("Bound must be positive.");
  }

  @Test
  void longNegativeBound() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.source.getProperty("random.long(-5)"))
            .withMessage("Bound must be positive.");
  }

  @Test
  void getPropertyWhenLongOverflowReturnsValue() {
    RandomValuePropertySource source = spy(this.source);
    given(source.getSource()).willReturn(new Random() {

      @Override
      public long nextLong() {
        // constant that used to become -8, now becomes 8
        return Long.MIN_VALUE;
      }

    });
    Long value = (Long) source.getProperty("random.long(10)");
    assertThat(value).isNotNull().isGreaterThanOrEqualTo(0L).isLessThan(10L);
    value = (Long) source.getProperty("random.long[4,10]");
    assertThat(value).isNotNull().isGreaterThanOrEqualTo(4L).isLessThan(10L);
  }

  @Test
  void addToEnvironmentAddsSource() {
    MockEnvironment environment = new MockEnvironment();
    RandomValuePropertySource.addToEnvironment(environment);
    Assertions.assertThat(environment.getProperty("random.string")).isNotNull();
  }

  @Test
  void addToEnvironmentWhenAlreadyAddedAddsSource() {
    MockEnvironment environment = new MockEnvironment();
    RandomValuePropertySource.addToEnvironment(environment);
    RandomValuePropertySource.addToEnvironment(environment);
    Assertions.assertThat(environment.getProperty("random.string")).isNotNull();
  }

  @Test
  void addToEnvironmentAddsAfterSystemEnvironment() {
    MockEnvironment environment = new MockEnvironment();
    environment.getPropertySources()
            .addFirst(new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    Collections.emptyMap()));
    RandomValuePropertySource.addToEnvironment(environment);
    Assertions.assertThat(environment.getPropertySources().stream().map(PropertySource::getName)).containsExactly(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            RandomValuePropertySource.RANDOM_PROPERTY_SOURCE_NAME, "mockProperties");
  }

}
