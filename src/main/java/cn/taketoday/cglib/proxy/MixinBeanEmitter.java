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
package cn.taketoday.cglib.proxy;

import java.lang.reflect.Method;

import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.cglib.core.CglibReflectUtils;

/**
 * @author Chris Nokleberg
 * @version $Id: MixinBeanEmitter.java,v 1.2 2004/06/24 21:15:20 herbyderby Exp
 * $
 */
@SuppressWarnings("all")
class MixinBeanEmitter extends MixinEmitter {
  public MixinBeanEmitter(ClassVisitor v, String className, Class[] classes) {
    super(v, className, classes, null);
  }

  protected Class[] getInterfaces(Class[] classes) {
    return null;
  }

  protected Method[] getMethods(Class type) {
    return CglibReflectUtils.getPropertyMethods(CglibReflectUtils.getBeanProperties(type), true, true);
  }
}
