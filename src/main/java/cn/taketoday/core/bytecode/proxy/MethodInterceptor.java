/*
 * Copyright 2002,2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.core.bytecode.proxy;

import java.lang.reflect.Method;

/**
 * General-purpose {@link Enhancer} callback which provides for "around advice".
 *
 * @author Juozas Baliuka <a href="mailto:baliuka@mwm.lt">baliuka@mwm.lt</a>
 * @author TODAY
 */
@FunctionalInterface
public interface MethodInterceptor extends Callback {

  /**
   * All generated proxied methods call this method instead of the original
   * method. The original method may either be invoked by normal reflection using
   * the Method object, or by using the MethodProxy (faster).
   *
   * @param obj "this", the enhanced object
   * @param method intercepted Method
   * @param args argument array; primitive types are wrapped
   * @param proxy used to invoke super (non-intercepted method); may be called as
   * many times as needed
   * @return any value compatible with the signature of the proxied method. Method
   * returning void will ignore this value.
   * @throws Throwable any exception may be thrown; if so, super method will not be
   * invoked
   * @see MethodProxy
   */
  Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable;

}
