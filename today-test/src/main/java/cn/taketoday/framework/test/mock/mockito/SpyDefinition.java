/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import org.mockito.AdditionalAnswers;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.listeners.VerificationStartedEvent;
import org.mockito.listeners.VerificationStartedListener;

import java.lang.reflect.Proxy;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.test.util.AopTestUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

import static org.mockito.Mockito.mock;

/**
 * A complete definition that can be used to create a Mockito spy.
 *
 * @author Phillip Webb
 */
class SpyDefinition extends Definition {

  private static final int MULTIPLIER = 31;

  private final ResolvableType typeToSpy;

  SpyDefinition(String name, ResolvableType typeToSpy, MockReset reset, boolean proxyTargetAware,
          QualifierDefinition qualifier) {
    super(name, reset, proxyTargetAware, qualifier);
    Assert.notNull(typeToSpy, "TypeToSpy must not be null");
    this.typeToSpy = typeToSpy;

  }

  ResolvableType getTypeToSpy() {
    return this.typeToSpy;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }
    SpyDefinition other = (SpyDefinition) obj;
    boolean result = super.equals(obj);
    result = result && ObjectUtils.nullSafeEquals(this.typeToSpy, other.typeToSpy);
    return result;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.typeToSpy);
    return result;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("name", getName()).append("typeToSpy", this.typeToSpy)
            .append("reset", getReset()).toString();
  }

  <T> T createSpy(Object instance) {
    return createSpy(getName(), instance);
  }

  @SuppressWarnings("unchecked")
  <T> T createSpy(String name, Object instance) {
    Assert.notNull(instance, "Instance must not be null");
    Assert.isInstanceOf(this.typeToSpy.resolve(), instance);
    if (Mockito.mockingDetails(instance).isSpy()) {
      return (T) instance;
    }
    MockSettings settings = MockReset.withSettings(getReset());
    if (StringUtils.isNotEmpty(name)) {
      settings.name(name);
    }
    if (isProxyTargetAware()) {
      settings.verificationStartedListeners(new AopBypassingVerificationStartedListener());
    }
    Class<?> toSpy;
    if (Proxy.isProxyClass(instance.getClass())) {
      settings.defaultAnswer(AdditionalAnswers.delegatesTo(instance));
      toSpy = this.typeToSpy.toClass();
    }
    else {
      settings.defaultAnswer(Mockito.CALLS_REAL_METHODS);
      settings.spiedInstance(instance);
      toSpy = instance.getClass();
    }
    return (T) mock(toSpy, settings);
  }

  /**
   * A {@link VerificationStartedListener} that bypasses any proxy created by Infra AOP
   * when the verification of a spy starts.
   */
  private static final class AopBypassingVerificationStartedListener implements VerificationStartedListener {

    @Override
    public void onVerificationStarted(VerificationStartedEvent event) {
      event.setMock(AopTestUtils.getUltimateTargetObject(event.getMock()));
    }

  }

}
