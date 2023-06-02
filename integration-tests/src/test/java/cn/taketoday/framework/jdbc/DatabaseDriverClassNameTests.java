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

package cn.taketoday.framework.jdbc;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.sql.XADataSource;

import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.jdbc.config.DatabaseDriver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the class names in the {@link DatabaseDriver} enumeration.
 *
 * @author Andy Wilkinson
 */
class DatabaseDriverClassNameTests {

  private static final Set<DatabaseDriver> EXCLUDED_DRIVERS = EnumSet.of(
          DatabaseDriver.UNKNOWN,
          DatabaseDriver.DB2_AS400,
          DatabaseDriver.INFORMIX,
          DatabaseDriver.HANA,
          DatabaseDriver.PHOENIX,
          DatabaseDriver.TERADATA,
          DatabaseDriver.REDSHIFT
  );

  @ParameterizedTest(name = "{0} {2}")
  @MethodSource
  void databaseClassIsOfRequiredType(DatabaseDriver driver, String className, Class<?> requiredType)
          throws Exception {
    assertThat(getInterfaceNames(className.replace('.', '/'))).contains(requiredType.getName().replace('.', '/'));
  }

  private List<String> getInterfaceNames(String className) throws IOException {
    // Use ASM to avoid unwanted side effects of loading JDBC drivers
    InputStream resourceAsStream = getClass().getResourceAsStream("/" + className + ".class");
    ClassReader classReader = new ClassReader(resourceAsStream);
    List<String> interfaceNames = new ArrayList<>();
    for (String name : classReader.getInterfaces()) {
      interfaceNames.add(name);
      interfaceNames.addAll(getInterfaceNames(name));
    }
    String superName = classReader.getSuperName();
    if (superName != null) {
      interfaceNames.addAll(getInterfaceNames(superName));
    }
    return interfaceNames;
  }

  static Stream<? extends Arguments> databaseClassIsOfRequiredType() {
    return Stream.concat(argumentsForType(Driver.class, DatabaseDriver::getDriverClassName),
            argumentsForType(XADataSource.class,
                    (databaseDriver) -> databaseDriver.getXaDataSourceClassName() != null,
                    DatabaseDriver::getXaDataSourceClassName));
  }

  private static Stream<? extends Arguments> argumentsForType(Class<?> clazz,
          Function<DatabaseDriver, String> classNameExtractor) {
    return argumentsForType(clazz, (databaseDriver) -> true, classNameExtractor);
  }

  private static Stream<? extends Arguments> argumentsForType(Class<?> clazz,
          Predicate<DatabaseDriver> predicate,
          Function<DatabaseDriver, String> classNameExtractor) {
    return Stream.of(DatabaseDriver.values()).filter((databaseDriver) -> !EXCLUDED_DRIVERS.contains(databaseDriver))
            .filter(predicate)
            .map((databaseDriver) -> Arguments.of(databaseDriver, classNameExtractor.apply(databaseDriver), clazz));
  }

}
