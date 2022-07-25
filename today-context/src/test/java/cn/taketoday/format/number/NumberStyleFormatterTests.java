/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.format.number;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Keith Donald
 */
public class NumberStyleFormatterTests {

  private final NumberStyleFormatter formatter = new NumberStyleFormatter();

  @Test
  public void formatValue() {
    assertThat(formatter.print(new BigDecimal("23.56"), Locale.US)).isEqualTo("23.56");
  }

  @Test
  public void parseValue() throws ParseException {
    assertThat(formatter.parse("23.56", Locale.US)).isEqualTo(new BigDecimal("23.56"));
  }

  @Test
  public void parseBogusValue() throws ParseException {
    assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
            formatter.parse("bogus", Locale.US));
  }

  @Test
  public void parsePercentValueNotLenientFailure() throws ParseException {
    assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
            formatter.parse("23.56bogus", Locale.US));
  }

}
