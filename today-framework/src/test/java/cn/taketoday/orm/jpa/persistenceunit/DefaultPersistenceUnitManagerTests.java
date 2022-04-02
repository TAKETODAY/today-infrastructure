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

import cn.taketoday.context.loader.CandidateComponentsTestClassLoader;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.orm.jpa.domain.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultPersistenceUnitManager}.
 *
 * @author Stephane Nicoll
 */
public class DefaultPersistenceUnitManagerTests {

  private final DefaultPersistenceUnitManager manager = new DefaultPersistenceUnitManager();

  @Test
  public void defaultDomainWithScan() {
    this.manager.setPackagesToScan("cn.taketoday.orm.jpa.domain");
    this.manager.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    testDefaultDomain();
  }

  @Test
  public void defaultDomainWithIndex() {
    this.manager.setPackagesToScan("cn.taketoday.orm.jpa.domain");
    this.manager.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.index(getClass().getClassLoader(),
                    new ClassPathResource("spring.components", Person.class))));
    testDefaultDomain();
  }

  private void testDefaultDomain() {
    JpaPersistenceUnitInfo puInfo = buildDefaultPersistenceUnitInfo();
    assertThat(puInfo.getManagedClassNames()).contains(
            "cn.taketoday.orm.jpa.domain.Person",
            "cn.taketoday.orm.jpa.domain.DriversLicense");
  }

  private JpaPersistenceUnitInfo buildDefaultPersistenceUnitInfo() {
    this.manager.preparePersistenceUnitInfos();
    return (JpaPersistenceUnitInfo) this.manager.obtainDefaultPersistenceUnitInfo();
  }

}
