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

package cn.taketoday.buildpack.platform.docker.transport;

import org.junit.jupiter.api.Test;

import java.util.Iterator;

import cn.taketoday.buildpack.platform.docker.transport.Errors.Error;
import cn.taketoday.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Errors}.
 *
 * @author Phillip Webb
 */
class ErrorsTests extends AbstractJsonTests {

  @Test
  void readValueDeserializesJson() throws Exception {
    Errors errors = getObjectMapper().readValue(getContent("errors.json"), Errors.class);
    Iterator<Error> iterator = errors.iterator();
    Error error1 = iterator.next();
    Error error2 = iterator.next();
    assertThat(iterator.hasNext()).isFalse();
    assertThat(error1.getCode()).isEqualTo("TEST1");
    assertThat(error1.getMessage()).isEqualTo("Test One");
    assertThat(error2.getCode()).isEqualTo("TEST2");
    assertThat(error2.getMessage()).isEqualTo("Test Two");
  }

  @Test
  void toStringHasErrorDetails() throws Exception {
    Errors errors = getObjectMapper().readValue(getContent("errors.json"), Errors.class);
    assertThat(errors).hasToString("[TEST1: Test One, TEST2: Test Two]");
  }

}
