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

package cn.taketoday.jdbc.support;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.dao.DataRetrievalFailureException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link KeyHolder} and {@link GeneratedKeyHolder}.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @since July 18, 2004
 */
class KeyHolderTests {

  private final KeyHolder kh = new GeneratedKeyHolder();

  @Test
  void getKeyForSingleNumericKey() {
    kh.getKeyList().add(singletonMap("key", 1));

    assertThat(kh.getKey()).as("single key should be returned").isEqualTo(1);
  }

  @Test
  void getKeyForSingleNonNumericKey() {
    kh.getKeyList().add(singletonMap("key", "ABC"));

    assertThatExceptionOfType(DataRetrievalFailureException.class)
            .isThrownBy(() -> kh.getKey())
            .withMessage("The generated key type is not supported. Unable to cast [java.lang.String] to [java.lang.Number].");
  }

  @Test
  void getKeyWithNoKeysInMap() {
    kh.getKeyList().add(emptyMap());

    assertThatExceptionOfType(DataRetrievalFailureException.class)
            .isThrownBy(() -> kh.getKey())
            .withMessageStartingWith("Unable to retrieve the generated key.");
  }

  @Test
  void getKeyWithMultipleKeysInMap() {
    @SuppressWarnings("serial")
    Map<String, Object> m = new HashMap<String, Object>() {{
      put("key", 1);
      put("seq", 2);
    }};
    kh.getKeyList().add(m);

    assertThat(kh.getKeys()).as("two keys should be in the map").hasSize(2);
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(() -> kh.getKey())
            .withMessageStartingWith("The getKey method should only be used when a single key is returned.");
  }

  @Test
  void getKeyAsStringForSingleKey() {
    kh.getKeyList().add(singletonMap("key", "ABC"));

    assertThat(kh.getKeyAs(String.class)).as("single key should be returned").isEqualTo("ABC");
  }

  @Test
  void getKeyAsWrongType() {
    kh.getKeyList().add(singletonMap("key", "ABC"));

    assertThatExceptionOfType(DataRetrievalFailureException.class)
            .isThrownBy(() -> kh.getKeyAs(Integer.class))
            .withMessage("The generated key type is not supported. Unable to cast [java.lang.String] to [java.lang.Integer].");
  }

  @Test
  void getKeyAsIntegerWithNullValue() {
    kh.getKeyList().add(singletonMap("key", null));

    assertThatExceptionOfType(DataRetrievalFailureException.class)
            .isThrownBy(() -> kh.getKeyAs(Integer.class))
            .withMessage("The generated key type is not supported. Unable to cast [null] to [java.lang.Integer].");
  }

  @Test
  void getKeysWithMultipleKeyRows() {
    @SuppressWarnings("serial")
    Map<String, Object> m = new HashMap<String, Object>() {{
      put("key", 1);
      put("seq", 2);
    }};
    kh.getKeyList().addAll(asList(m, m));

    assertThat(kh.getKeyList()).as("two rows should be in the list").hasSize(2);
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(() -> kh.getKeys())
            .withMessageStartingWith("The getKeys method should only be used when keys for a single row are returned.");
  }

}
