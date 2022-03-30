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

package cn.taketoday.framework.test.mock.mockito;

import org.mockito.Answers;
import org.mockito.MockSettings;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

import static org.mockito.Mockito.mock;

/**
 * A complete definition that can be used to create a Mockito mock.
 *
 * @author Phillip Webb
 */
class MockDefinition extends Definition {

  private static final int MULTIPLIER = 31;

  private final ResolvableType typeToMock;

  private final Set<Class<?>> extraInterfaces;

  private final Answers answer;

  private final boolean serializable;

  MockDefinition(String name, ResolvableType typeToMock, Class<?>[] extraInterfaces, Answers answer,
          boolean serializable, MockReset reset, QualifierDefinition qualifier) {
    super(name, reset, false, qualifier);
    Assert.notNull(typeToMock, "TypeToMock must not be null");
    this.typeToMock = typeToMock;
    this.extraInterfaces = asClassSet(extraInterfaces);
    this.answer = (answer != null) ? answer : Answers.RETURNS_DEFAULTS;
    this.serializable = serializable;
  }

  private Set<Class<?>> asClassSet(Class<?>[] classes) {
    Set<Class<?>> classSet = new LinkedHashSet<>();
    if (classes != null) {
      classSet.addAll(Arrays.asList(classes));
    }
    return Collections.unmodifiableSet(classSet);
  }

  /**
   * Return the type that should be mocked.
   *
   * @return the type to mock; never {@code null}
   */
  ResolvableType getTypeToMock() {
    return this.typeToMock;
  }

  /**
   * Return the extra interfaces.
   *
   * @return the extra interfaces or an empty set
   */
  Set<Class<?>> getExtraInterfaces() {
    return this.extraInterfaces;
  }

  /**
   * Return the answers mode.
   *
   * @return the answers mode; never {@code null}
   */
  Answers getAnswer() {
    return this.answer;
  }

  /**
   * Return if the mock is serializable.
   *
   * @return if the mock is serializable
   */
  boolean isSerializable() {
    return this.serializable;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }
    MockDefinition other = (MockDefinition) obj;
    boolean result = super.equals(obj);
    result = result && ObjectUtils.nullSafeEquals(this.typeToMock, other.typeToMock);
    result = result && ObjectUtils.nullSafeEquals(this.extraInterfaces, other.extraInterfaces);
    result = result && ObjectUtils.nullSafeEquals(this.answer, other.answer);
    result = result && this.serializable == other.serializable;
    return result;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.typeToMock);
    result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.extraInterfaces);
    result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.answer);
    result = MULTIPLIER * result + Boolean.hashCode(this.serializable);
    return result;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("name", getName()).append("typeToMock", this.typeToMock)
            .append("extraInterfaces", this.extraInterfaces).append("answer", this.answer)
            .append("serializable", this.serializable).append("reset", getReset()).toString();
  }

  <T> T createMock() {
    return createMock(getName());
  }

  @SuppressWarnings("unchecked")
  <T> T createMock(String name) {
    MockSettings settings = MockReset.withSettings(getReset());
    if (StringUtils.isNotEmpty(name)) {
      settings.name(name);
    }
    if (!this.extraInterfaces.isEmpty()) {
      settings.extraInterfaces(ClassUtils.toClassArray(this.extraInterfaces));
    }
    settings.defaultAnswer(this.answer);
    if (this.serializable) {
      settings.serializable();
    }
    return (T) mock(this.typeToMock.resolve(), settings);
  }

}
