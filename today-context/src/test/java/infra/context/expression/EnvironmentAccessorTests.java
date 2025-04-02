/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.expression;

import org.junit.jupiter.api.Test;

import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.TestBean;
import infra.context.support.GenericApplicationContext;
import infra.core.env.Environment;
import infra.core.testfixture.env.MockPropertySource;
import infra.expression.AccessException;
import infra.expression.TypedValue;

import static infra.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 21:34
 */
class EnvironmentAccessorTests {

  @Test
  public void braceAccess() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "#{environment['my.name']}")
                    .getBeanDefinition());

    GenericApplicationContext ctx = new GenericApplicationContext(bf);
    ctx.getEnvironment().getPropertySources().addFirst(new MockPropertySource().withProperty("my.name", "myBean"));
    ctx.refresh();

    assertThat(ctx.getBean(TestBean.class).getName()).isEqualTo("myBean");
    ctx.close();
  }

  @Test
  void canReadAlwaysReturnsTrue() throws AccessException {
    EnvironmentAccessor accessor = new EnvironmentAccessor();
    Environment environment = mock(Environment.class);
    assertThat(accessor.canRead(null, environment, "any.property")).isTrue();
  }

  @Test
  void readReturnsPropertyValue() throws AccessException {
    EnvironmentAccessor accessor = new EnvironmentAccessor();
    Environment environment = mock(Environment.class);
    when(environment.getProperty("test.property")).thenReturn("test-value");

    TypedValue result = accessor.read(null, environment, "test.property");
    assertThat(result.getValue()).isEqualTo("test-value");
  }

  @Test
  void readWithNonEnvironmentTargetThrowsException() {
    EnvironmentAccessor accessor = new EnvironmentAccessor();

    assertThatIllegalStateException()
            .isThrownBy(() -> accessor.read(null, new Object(), "property"))
            .withMessage("Target must be of type Environment");
  }

  @Test
  void cannotWriteProperties() throws AccessException {
    EnvironmentAccessor accessor = new EnvironmentAccessor();
    Environment environment = mock(Environment.class);

    assertThat(accessor.canWrite(null, environment, "property")).isFalse();
  }

  @Test
  void writeIsNoOp() throws AccessException {
    EnvironmentAccessor accessor = new EnvironmentAccessor();
    Environment environment = mock(Environment.class);

    accessor.write(null, environment, "property", "value");
    verifyNoInteractions(environment);
  }

  @Test
  void specificTargetClassesReturnsEnvironment() {
    EnvironmentAccessor accessor = new EnvironmentAccessor();
    assertThat(accessor.getSpecificTargetClasses()).contains(Environment.class);
  }

  @Test
  void readNullPropertyReturnsNull() throws AccessException {
    EnvironmentAccessor accessor = new EnvironmentAccessor();
    Environment environment = mock(Environment.class);
    when(environment.getProperty("null.property")).thenReturn(null);

    TypedValue result = accessor.read(null, environment, "null.property");
    assertThat(result.getValue()).isNull();
  }

  @Test
  void dotNotationAccess() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "#{environment.myProperty}")
                    .getBeanDefinition());

    GenericApplicationContext ctx = new GenericApplicationContext(bf);
    ctx.getEnvironment().getPropertySources().addFirst(
            new MockPropertySource().withProperty("myProperty", "dotValue"));
    ctx.refresh();

    assertThat(ctx.getBean(TestBean.class).getName()).isEqualTo("dotValue");
    ctx.close();
  }

}
