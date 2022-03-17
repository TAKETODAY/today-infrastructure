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

package cn.taketoday.jndi;

import org.junit.jupiter.api.Test;

import javax.naming.Context;
import javax.naming.NamingException;

import cn.taketoday.testfixture.jndi.SimpleNamingContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JndiPropertySource}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 4.0
 */
public class JndiPropertySourceTests {

  @Test
  public void nonExistentProperty() {
    JndiPropertySource ps = new JndiPropertySource("jndiProperties");
    assertThat(ps.getProperty("bogus")).isNull();
  }

  @Test
  public void nameBoundWithoutPrefix() {
    final SimpleNamingContext context = new SimpleNamingContext();
    context.bind("p1", "v1");

    JndiTemplate jndiTemplate = new JndiTemplate() {
      @Override
      protected Context createInitialContext() throws NamingException {
        return context;
      }
    };
    JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();
    jndiLocator.setResourceRef(true);
    jndiLocator.setJndiTemplate(jndiTemplate);

    JndiPropertySource ps = new JndiPropertySource("jndiProperties", jndiLocator);
    assertThat(ps.getProperty("p1")).isEqualTo("v1");
  }

  @Test
  public void nameBoundWithPrefix() {
    final SimpleNamingContext context = new SimpleNamingContext();
    context.bind("java:comp/env/p1", "v1");

    JndiTemplate jndiTemplate = new JndiTemplate() {
      @Override
      protected Context createInitialContext() throws NamingException {
        return context;
      }
    };
    JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();
    jndiLocator.setResourceRef(true);
    jndiLocator.setJndiTemplate(jndiTemplate);

    JndiPropertySource ps = new JndiPropertySource("jndiProperties", jndiLocator);
    assertThat(ps.getProperty("p1")).isEqualTo("v1");
  }

  @Test
  public void propertyWithDefaultClauseInResourceRefMode() {
    JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate() {
      @Override
      public Object lookup(String jndiName) throws NamingException {
        throw new IllegalStateException("Should not get called");
      }
    };
    jndiLocator.setResourceRef(true);

    JndiPropertySource ps = new JndiPropertySource("jndiProperties", jndiLocator);
    assertThat(ps.getProperty("propertyKey:defaultValue")).isNull();
  }

  @Test
  public void propertyWithColonInNonResourceRefMode() {
    JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate() {
      @Override
      public Object lookup(String jndiName) throws NamingException {
        assertThat(jndiName).isEqualTo("my:key");
        return "my:value";
      }
    };
    jndiLocator.setResourceRef(false);

    JndiPropertySource ps = new JndiPropertySource("jndiProperties", jndiLocator);
    assertThat(ps.getProperty("my:key")).isEqualTo("my:value");
  }

}
