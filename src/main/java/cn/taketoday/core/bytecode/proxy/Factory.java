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
package cn.taketoday.core.bytecode.proxy;

/**
 * All enhanced instances returned by the {@link Enhancer} class implement this
 * interface. Using this interface for new instances is faster than going
 * through the <code>Enhancer</code> interface or using reflection. In addition,
 * to intercept methods called during object construction you <b>must</b> use
 * these methods instead of reflection.
 *
 * @author Juozas Baliuka <a href="mailto:baliuka@mwm.lt">baliuka@mwm.lt</a>
 * @version $Id: Factory.java,v 1.13 2004/06/24 21:15:20 herbyderby Exp $
 */
public interface Factory {
  /**
   * Creates new instance of the same type, using the no-arg constructor. The
   * class of this object must have been created using a single Callback type. If
   * multiple callbacks are required an exception will be thrown.
   *
   * @param callback the new interceptor to use
   * @return new instance of the same type
   */
  Object newInstance(Callback callback);

  /**
   * Creates new instance of the same type, using the no-arg constructor.
   *
   * @param callbacks the new callbacks(s) to use
   * @return new instance of the same type
   */
  Object newInstance(Callback[] callbacks);

  /**
   * Creates a new instance of the same type, using the constructor matching the
   * given signature.
   *
   * @param types the constructor argument types
   * @param args the constructor arguments
   * @param callbacks the new interceptor(s) to use
   * @return new instance of the same type
   */
  Object newInstance(Class<?>[] types, Object[] args, Callback[] callbacks);

  /**
   * Return the <code>Callback</code> implementation at the specified index.
   *
   * @param index the callback index
   * @return the callback implementation
   */
  Callback getCallback(int index);

  /**
   * Set the callback for this object for the given type.
   *
   * @param index the callback index to replace
   * @param callback the new callback
   */
  void setCallback(int index, Callback callback);

  /**
   * Replace all of the callbacks for this object at once.
   *
   * @param callbacks the new callbacks(s) to use
   */
  void setCallbacks(Callback[] callbacks);

  /**
   * Get the current set of callbacks for ths object.
   *
   * @return a new array instance
   */
  Callback[] getCallbacks();
}
