/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.conversion.support;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * @author Today <br>
 * 2018-07-12 20:43:53
 */
class DurationConverterTests {


  @Test
  public void testParseDuration() {

    Duration s = DurationConverter.parseDuration("123s");
    Duration h = DurationConverter.parseDuration("123h");
    Duration ns = DurationConverter.parseDuration("123ns");
    Duration ms = DurationConverter.parseDuration("123ms");
    Duration min = DurationConverter.parseDuration("123min");
    Duration d = DurationConverter.parseDuration("123d");

    Duration convert = DurationConverter.parseDuration("PT20S");

    assert s.equals(Duration.of(123, ChronoUnit.SECONDS));
    assert h.equals(Duration.of(123, ChronoUnit.HOURS));
    assert ns.equals(Duration.of(123, ChronoUnit.NANOS));
    assert ms.equals(Duration.of(123, ChronoUnit.MILLIS));
    assert min.equals(Duration.of(123, ChronoUnit.MINUTES));
    assert d.equals(Duration.of(123, ChronoUnit.DAYS));

    assert convert.equals(Duration.of(20l, ChronoUnit.SECONDS));
  }
}
