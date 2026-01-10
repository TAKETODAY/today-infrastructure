/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.util;

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
