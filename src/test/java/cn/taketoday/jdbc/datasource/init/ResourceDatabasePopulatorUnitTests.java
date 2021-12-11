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

package cn.taketoday.jdbc.datasource.init;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.mock;

/**
 * Unit tests for {@link ResourceDatabasePopulator}.
 *
 * @author Sam Brannen
 * @see AbstractDatabasePopulatorTests
 * @since 4.0
 */
class ResourceDatabasePopulatorUnitTests {

  private static final Resource script1 = mock(Resource.class);
  private static final Resource script2 = mock(Resource.class);
  private static final Resource script3 = mock(Resource.class);

  @Test
  void constructWithNullResource() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ResourceDatabasePopulator((Resource) null));
  }

  @Test
  void constructWithNullResourceArray() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ResourceDatabasePopulator((Resource[]) null));
  }

  @Test
  void constructWithResource() {
    ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator(script1);
    assertThat(databasePopulator.scripts).hasSize(1);
  }

  @Test
  void constructWithMultipleResources() {
    ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator(script1, script2);
    assertThat(databasePopulator.scripts).hasSize(2);
  }

  @Test
  void constructWithMultipleResourcesAndThenAddScript() {
    ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator(script1, script2);
    assertThat(databasePopulator.scripts).hasSize(2);

    databasePopulator.addScript(script3);
    assertThat(databasePopulator.scripts).hasSize(3);
  }

  @Test
  void addScriptsWithNullResource() {
    ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
    assertThatIllegalArgumentException().isThrownBy(() ->
            databasePopulator.addScripts((Resource) null));
  }

  @Test
  void addScriptsWithNullResourceArray() {
    ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
    assertThatIllegalArgumentException().isThrownBy(() ->
            databasePopulator.addScripts((Resource[]) null));
  }

  @Test
  void setScriptsWithNullResource() {
    ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
    assertThatIllegalArgumentException().isThrownBy(() ->
            databasePopulator.setScripts((Resource) null));
  }

  @Test
  void setScriptsWithNullResourceArray() {
    ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
    assertThatIllegalArgumentException().isThrownBy(() ->
            databasePopulator.setScripts((Resource[]) null));
  }

  @Test
  void setScriptsAndThenAddScript() {
    ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
    assertThat(databasePopulator.scripts).isEmpty();

    databasePopulator.setScripts(script1, script2);
    assertThat(databasePopulator.scripts).hasSize(2);

    databasePopulator.addScript(script3);
    assertThat(databasePopulator.scripts).hasSize(3);
  }

}
