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

package infra.beans.factory.xml.support;

import org.junit.jupiter.api.Test;

import infra.beans.factory.xml.DefaultNamespaceHandlerResolver;
import infra.beans.factory.xml.NamespaceHandler;
import infra.beans.factory.xml.UtilNamespaceHandler;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Unit and integration tests for the {@link DefaultNamespaceHandlerResolver} class.
 *
 * @author Rob Harrop
 * @author Rick Evans
 */
public class DefaultNamespaceHandlerResolverTests {

  @Test
  public void testResolvedMappedHandler() {
    DefaultNamespaceHandlerResolver resolver = new DefaultNamespaceHandlerResolver(getClass().getClassLoader());
    NamespaceHandler handler = resolver.resolve("http://www.springframework.org/schema/util");
    assertThat(handler).as("Handler should not be null.").isNotNull();
    assertThat(handler.getClass()).as("Incorrect handler loaded").isEqualTo(UtilNamespaceHandler.class);
  }

  @Test
  public void testResolvedMappedHandlerWithNoArgCtor() {
    DefaultNamespaceHandlerResolver resolver = new DefaultNamespaceHandlerResolver();
    NamespaceHandler handler = resolver.resolve("http://www.springframework.org/schema/util");
    assertThat(handler).as("Handler should not be null.").isNotNull();
    assertThat(handler.getClass()).as("Incorrect handler loaded").isEqualTo(UtilNamespaceHandler.class);
  }

  @Test
  public void testNonExistentHandlerClass() {
    String mappingPath = "infra/beans/factory/xml/support/nonExistent.properties";
    new DefaultNamespaceHandlerResolver(getClass().getClassLoader(), mappingPath);
  }

  @Test
  public void testCtorWithNullClassLoaderArgument() {
    // simply must not bail...
    new DefaultNamespaceHandlerResolver(null);
  }

  @Test
  public void testCtorWithNullClassLoaderArgumentAndNullMappingLocationArgument() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new DefaultNamespaceHandlerResolver(null, null));
  }

  @Test
  public void testCtorWithNonExistentMappingLocationArgument() {
    // simply must not bail; we don't want non-existent resources to result in an Exception
    new DefaultNamespaceHandlerResolver(null, "738trbc bobabloobop871");
  }

}
