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
package cn.taketoday.util;

import org.junit.jupiter.api.Test;

/**
 * @author TODAY <br>
 * 2019-03-15 21:21
 */
public class DataSizeTest {

  @Test
  public void testDataSize() {

    DataSize parse = DataSize.parse("10MB");
    DataSize gb = DataSize.parse("1GB");

    DataSize.parse("1024"); // 1024b
    try {
      DataSize.parse("error"); // 1024b
      assert false;
    }
    catch (Exception e) {
      assert true;
    }
    try {
      DataUnit.fromSuffix("");
      assert false;
    }
    catch (Exception e) {
      assert true;
    }

    gb.hashCode();
    System.err.println(gb);
    assert !parse.equals(gb);
    assert gb.equals(gb);
    assert !gb.equals(null);

    assert !gb.isNegative();

    assert gb.toBytes() == DataSize.ofGigabytes(1).toBytes();
    assert gb.toGigabytes() == DataSize.ofGigabytes(1).toGigabytes();
    assert gb.toKilobytes() == DataSize.ofGigabytes(1).toKilobytes();
    assert gb.toMegabytes() == DataSize.ofGigabytes(1).toMegabytes();
    assert gb.toTerabytes() == DataSize.ofGigabytes(1).toTerabytes();

    assert gb.compareTo(DataSize.ofGigabytes(1)) == 0;
    assert gb.equals(DataSize.ofGigabytes(1));
    assert parse.equals(DataSize.ofMegabytes(10));
  }

}
