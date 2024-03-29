/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.build;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BuilderException}.
 *
 * @author Scott Frederick
 */
class BuilderExceptionTests {

  @Test
  void create() {
    BuilderException exception = new BuilderException("detector", 1);
    assertThat(exception.getOperation()).isEqualTo("detector");
    assertThat(exception.getStatusCode()).isOne();
    assertThat(exception.getMessage()).isEqualTo("Builder lifecycle 'detector' failed with status code 1");
  }

  @Test
  void createWhenOperationIsNull() {
    BuilderException exception = new BuilderException(null, 1);
    assertThat(exception.getOperation()).isNull();
    assertThat(exception.getStatusCode()).isOne();
    assertThat(exception.getMessage()).isEqualTo("Builder failed with status code 1");
  }

}
