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

package cn.taketoday.orm.jpa.eclipselink;

import org.eclipse.persistence.jpa.JpaEntityManager;
import org.junit.jupiter.api.Test;

import cn.taketoday.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import cn.taketoday.orm.jpa.EntityManagerFactoryInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EclipseLink-specific JPA tests.
 *
 * @author Juergen Hoeller
 */
public class EclipseLinkEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

  @Test
  public void testCanCastNativeEntityManagerFactoryToEclipseLinkEntityManagerFactoryImpl() {
    EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
    assertThat(emfi.getNativeEntityManagerFactory().getClass().getName().endsWith("EntityManagerFactoryImpl")).isTrue();
  }

  @Test
  public void testCanCastSharedEntityManagerProxyToEclipseLinkEntityManager() {
    boolean condition = sharedEntityManager instanceof JpaEntityManager;
    assertThat(condition).isTrue();
    JpaEntityManager eclipselinkEntityManager = (JpaEntityManager) sharedEntityManager;
    assertThat(eclipselinkEntityManager.getActiveSession()).isNotNull();
  }

}
