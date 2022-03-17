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

package cn.taketoday.aop.support;

import org.aopalliance.intercept.MethodInvocation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aop.IntroductionInfo;
import cn.taketoday.util.ClassUtils;

/**
 * Support for implementations of {@link IntroductionInfo}.
 *
 * <p>Allows subclasses to conveniently add all interfaces from a given object,
 * and to suppress interfaces that should not be added. Also allows for querying
 * all introduced interfaces.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/3/8 22:19
 * @since 3.0
 */
public class IntroductionInfoSupport implements IntroductionInfo, Serializable {
  private static final long serialVersionUID = 1L;

  final LinkedHashSet<Class<?>> publishedInterfaces = new LinkedHashSet<>();

  private transient Map<Method, Boolean> rememberedMethods = new ConcurrentHashMap<>(32);

  /**
   * Suppress the specified interface, which may have been autodetected
   * due to the delegate implementing it. Call this method to exclude
   * internal interfaces from being visible at the proxy level.
   * <p>Does nothing if the interface is not implemented by the delegate.
   *
   * @param ifc the interface to suppress
   */
  public void suppressInterface(Class<?> ifc) {
    this.publishedInterfaces.remove(ifc);
  }

  @Override
  public Class<?>[] getInterfaces() {
    return ClassUtils.toClassArray(this.publishedInterfaces);
  }

  /**
   * Check whether the specified interfaces is a published introduction interface.
   *
   * @param ifc the interface to check
   * @return whether the interface is part of this introduction
   */
  public boolean implementsInterface(Class<?> ifc) {
    for (Class<?> pubIfc : this.publishedInterfaces) {
      if (ifc.isInterface() && ifc.isAssignableFrom(pubIfc)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Publish all interfaces that the given delegate implements at the proxy level.
   *
   * @param delegate the delegate object
   */
  protected void implementInterfacesOnObject(Object delegate) {
    this.publishedInterfaces.addAll(ClassUtils.getAllInterfacesAsSet(delegate));
  }

  /**
   * Is this method on an introduced interface?
   *
   * @param mi the method invocation
   * @return whether the invoked method is on an introduced interface
   */
  protected final boolean isMethodOnIntroducedInterface(MethodInvocation mi) {
    Boolean rememberedResult = this.rememberedMethods.get(mi.getMethod());
    if (rememberedResult != null) {
      return rememberedResult;
    }
    else {
      // Work it out and cache it.
      boolean result = implementsInterface(mi.getMethod().getDeclaringClass());
      this.rememberedMethods.put(mi.getMethod(), result);
      return result;
    }
  }

  //---------------------------------------------------------------------
  // Serialization support
  //---------------------------------------------------------------------

  /**
   * This method is implemented only to restore the logger.
   * We don't make the logger static as that would mean that subclasses
   * would use this class's log category.
   */
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    // Rely on default serialization; just initialize state after deserialization.
    ois.defaultReadObject();
    // Initialize transient fields.
    this.rememberedMethods = new ConcurrentHashMap<>(32);
  }

}
