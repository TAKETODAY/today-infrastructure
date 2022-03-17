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

package cn.taketoday.orm.jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Phillip Webb
 */
@SuppressWarnings("rawtypes")
public class LocalEntityManagerFactoryBeanTests extends AbstractEntityManagerFactoryBeanTests {

  // Static fields set by inner class DummyPersistenceProvider

  private static String actualName;

  private static Map actualProps;

  @AfterEach
  public void verifyClosed() throws Exception {
    verify(mockEmf).close();
  }

  @Test
  public void testValidUsageWithDefaultProperties() throws Exception {
    testValidUsage(null);
  }

  @Test
  public void testValidUsageWithExplicitProperties() throws Exception {
    testValidUsage(new Properties());
  }

  protected void testValidUsage(Properties props) throws Exception {
    // This will be set by DummyPersistenceProvider
    actualName = null;
    actualProps = null;

    LocalEntityManagerFactoryBean lemfb = new LocalEntityManagerFactoryBean();
    String entityManagerName = "call me Bob";

    lemfb.setPersistenceUnitName(entityManagerName);
    lemfb.setPersistenceProviderClass(DummyPersistenceProvider.class);
    if (props != null) {
      lemfb.setJpaProperties(props);
    }
    lemfb.afterPropertiesSet();

    assertThat(actualName).isSameAs(entityManagerName);
    if (props != null) {
      assertThat((Object) actualProps).isEqualTo(props);
    }
    checkInvariants(lemfb);

    lemfb.destroy();
  }

  protected static class DummyPersistenceProvider implements PersistenceProvider {

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo pui, Map map) {
      throw new UnsupportedOperationException();
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emfName, Map properties) {
      actualName = emfName;
      actualProps = properties;
      return mockEmf;
    }

    @Override
    public ProviderUtil getProviderUtil() {
      throw new UnsupportedOperationException();
    }

    // JPA 2.1 method
    @Override
    public void generateSchema(PersistenceUnitInfo persistenceUnitInfo, Map map) {
      throw new UnsupportedOperationException();
    }

    // JPA 2.1 method
    @Override
    public boolean generateSchema(String persistenceUnitName, Map map) {
      throw new UnsupportedOperationException();
    }
  }

}
