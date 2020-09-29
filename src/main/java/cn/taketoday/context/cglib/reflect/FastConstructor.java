/*
 * Copyright 2003 The Apache Software Foundation
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
package cn.taketoday.context.cglib.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author TODAY <br>
 * 2018-11-08 15:08
 */
@SuppressWarnings("all")
public class FastConstructor<T> extends FastMember {

  FastConstructor(FastClass fc, Constructor<T> constructor) {
    super(fc, constructor, fc.getIndex(constructor.getParameterTypes()));
  }

  public Class[] getParameterTypes() {
    return ((Constructor<T>) member).getParameterTypes();
  }

  public Class[] getExceptionTypes() {
    return ((Constructor<T>) member).getExceptionTypes();
  }

  public T newInstance() throws InvocationTargetException {
    return (T) fc.newInstance(index, null);
  }

  public T newInstance(Object[] args) throws InvocationTargetException {
    return (T) fc.newInstance(index, args);
  }

  public Constructor<T> getJavaConstructor() {
    return (Constructor<T>) member;
  }
}
