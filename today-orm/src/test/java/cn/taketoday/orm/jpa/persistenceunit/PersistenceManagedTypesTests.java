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

package cn.taketoday.orm.jpa.persistenceunit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/31 10:26
 */
class PersistenceManagedTypesTests {

  @Test
  void createWithManagedClassNames() {
    PersistenceManagedTypes managedTypes = PersistenceManagedTypes.of(
            "com.example.One", "com.example.Two");
    assertThat(managedTypes.getManagedClassNames()).containsExactly(
            "com.example.One", "com.example.Two");
    assertThat(managedTypes.getManagedPackages()).isEmpty();
    assertThat(managedTypes.getPersistenceUnitRootUrl()).isNull();
  }

  @Test
  void createWithNullManagedClasses() {
    assertThatIllegalArgumentException().isThrownBy(() -> PersistenceManagedTypes.of((String[]) null));
  }

  @Test
  void createWithManagedClassNamesAndPackages() {
    PersistenceManagedTypes managedTypes = PersistenceManagedTypes.of(
            List.of("com.example.One", "com.example.Two"), List.of("com.example"));
    assertThat(managedTypes.getManagedClassNames()).containsExactly(
            "com.example.One", "com.example.Two");
    assertThat(managedTypes.getManagedPackages()).containsExactly("com.example");
    assertThat(managedTypes.getPersistenceUnitRootUrl()).isNull();
  }

}