/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.jdbc.performance;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import infra.util.StringUtils;

/**
 * @author aldenquimby@gmail.com
 */
public class UnderscoreToCamelCasePerformanceTests {
  private static final int ITERATIONS = 1000;

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  @Test
  public void run() throws SQLException {
    PerformanceTestList tests = new PerformanceTestList();
    tests.add(new Sql2oUnderscoreToCamelCase());
    tests.add(new GuavaUnderscoreToCamelCase());

    tests.run(ITERATIONS);
    tests.printResults("");
  }

  private List<String> toConvert = ImmutableList.of("my_string_variable", "string", "my_really_long_string_variable_name",
          "my_string2_with_numbers_4", "my_string_with_MixED_CaSe",
          "", " ", "\t", "\n\n");

  //----------------------------------------
  //          performance tests
  // ---------------------------------------

  class Sql2oUnderscoreToCamelCase extends PerformanceTestBase {
    @Override
    public void init() { }

    @Override
    public void close() { }

    @Override
    public void run(int input) {
      for (String s : toConvert) {
        StringUtils.underscoreToCamelCase(s);
      }
    }
  }

  class GuavaUnderscoreToCamelCase extends PerformanceTestBase {
    @Override
    public void init() { }

    @Override
    public void close() { }

    @Override
    public void run(int input) {
      final CaseFormat lowerCamel = CaseFormat.LOWER_CAMEL;
      final CaseFormat lowerUnderscore = CaseFormat.LOWER_UNDERSCORE;
      for (String s : toConvert) {
        lowerUnderscore.to(lowerCamel, s);
      }
    }
  }
}
