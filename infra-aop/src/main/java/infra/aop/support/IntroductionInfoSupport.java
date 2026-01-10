/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aop.support;

import org.aopalliance.intercept.MethodInvocation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;

import infra.aop.IntroductionInfo;
import infra.util.ClassUtils;

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
  @Serial
  private static final long serialVersionUID = 1L;

  final LinkedHashSet<Class<?>> publishedInterfaces = new LinkedHashSet<>();

  private transient ConcurrentHashMap<Method, Boolean> rememberedMethods = new ConcurrentHashMap<>(32);

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
  @Serial
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    // Rely on default serialization; just initialize state after deserialization.
    ois.defaultReadObject();
    // Initialize transient fields.
    this.rememberedMethods = new ConcurrentHashMap<>(32);
  }

}
