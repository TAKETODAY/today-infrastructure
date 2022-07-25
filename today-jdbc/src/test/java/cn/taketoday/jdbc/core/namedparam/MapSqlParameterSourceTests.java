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

package cn.taketoday.jdbc.core.namedparam;

import org.junit.jupiter.api.Test;

import java.sql.Types;

import cn.taketoday.jdbc.core.SqlParameterValue;
import cn.taketoday.jdbc.support.JdbcUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rick Evans
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
public class MapSqlParameterSourceTests {

  @Test
  public void nullParameterValuesPassedToCtorIsOk() {
    new MapSqlParameterSource(null);
  }

  @Test
  public void getValueChokesIfParameterIsNotPresent() {
    MapSqlParameterSource source = new MapSqlParameterSource();
    assertThatIllegalArgumentException().isThrownBy(() ->
            source.getValue("pechorin was right!"));
  }

  @Test
  public void sqlParameterValueRegistersSqlType() {
    MapSqlParameterSource msps = new MapSqlParameterSource("FOO", new SqlParameterValue(Types.NUMERIC, "Foo"));
    assertThat(msps.getSqlType("FOO")).as("Correct SQL Type not registered").isEqualTo(2);
    MapSqlParameterSource msps2 = new MapSqlParameterSource();
    msps2.addValues(msps.getValues());
    assertThat(msps2.getSqlType("FOO")).as("Correct SQL Type not registered").isEqualTo(2);
  }

  @Test
  public void toStringShowsParameterDetails() {
    MapSqlParameterSource source = new MapSqlParameterSource("FOO", new SqlParameterValue(Types.NUMERIC, "Foo"));
    assertThat(source.toString()).isEqualTo("MapSqlParameterSource {FOO=Foo (type:NUMERIC)}");
  }

  @Test
  public void toStringShowsCustomSqlType() {
    MapSqlParameterSource source = new MapSqlParameterSource("FOO", new SqlParameterValue(Integer.MAX_VALUE, "Foo"));
    assertThat(source.toString()).isEqualTo(("MapSqlParameterSource {FOO=Foo (type:" + Integer.MAX_VALUE + ")}"));
  }

  @Test
  public void toStringDoesNotShowTypeUnknown() {
    MapSqlParameterSource source = new MapSqlParameterSource("FOO", new SqlParameterValue(JdbcUtils.TYPE_UNKNOWN, "Foo"));
    assertThat(source.toString()).isEqualTo("MapSqlParameterSource {FOO=Foo}");
  }

}
