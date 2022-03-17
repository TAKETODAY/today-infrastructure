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

package cn.taketoday.orm.jpa.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.orm.jpa.AbstractEntityManagerFactoryIntegrationTests;
import cn.taketoday.orm.jpa.support.PersistenceInjectionTests.DefaultPublicPersistenceContextSetter;
import cn.taketoday.orm.jpa.support.PersistenceInjectionTests.DefaultPublicPersistenceUnitSetterNamedPerson;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class PersistenceInjectionIntegrationTests extends AbstractEntityManagerFactoryIntegrationTests {

  @Autowired
  private DefaultPublicPersistenceContextSetter defaultSetterInjected;

  @Autowired
  private DefaultPublicPersistenceUnitSetterNamedPerson namedSetterInjected;

  @Test
  public void testDefaultPersistenceContextSetterInjection() {
    assertThat(defaultSetterInjected.getEntityManager()).isNotNull();
  }

  @Test
  public void testSetterInjectionOfNamedPersistenceContext() {
    assertThat(namedSetterInjected.getEntityManagerFactory()).isNotNull();
  }

}
