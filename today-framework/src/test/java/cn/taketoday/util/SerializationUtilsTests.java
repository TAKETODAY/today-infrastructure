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

package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/9 21:17
 */
class SerializationUtilsTests {

  private static final BigInteger FOO = new BigInteger(
          "-9702942423549012526722364838327831379660941553432801565505143675386108883970811292563757558516603356009681061" +
                  "5697574744209306031461371833798723505120163874786203211176873686513374052845353833564048");

  @Test
  void serializeCycleSunnyDay() throws Exception {
    assertThat(SerializationUtils.deserialize(SerializationUtils.serialize("foo"))).isEqualTo("foo");
  }

  @Test
  void deserializeUndefined() throws Exception {
    assertThatIllegalStateException().isThrownBy(() -> SerializationUtils.deserialize(FOO.toByteArray()));
  }

  @Test
  void serializeNonSerializable() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() -> SerializationUtils.serialize(new Object()));
  }

  @Test
  void deserializeNonSerializable() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() -> SerializationUtils.deserialize("foo".getBytes()));
  }

  @Test
  void serializeNull() throws Exception {
    assertThat(SerializationUtils.serialize(null)).isNull();
  }

  @Test
  void deserializeNull() throws Exception {
    assertThat(SerializationUtils.deserialize(null)).isNull();
  }

  @Test
  void cloneException() {
    IllegalArgumentException ex = new IllegalArgumentException("foo");
    assertThat(SerializationUtils.clone(ex)).hasMessage("foo").isNotSameAs(ex);
  }

}
