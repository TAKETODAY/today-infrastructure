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
package cn.taketoday.bytecode.proxy;

import java.lang.reflect.Method;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.core.CglibReflectUtils;

/**
 * @author Chris Nokleberg
 * @version $Id: MixinBeanEmitter.java,v 1.2 2004/06/24 21:15:20 herbyderby Exp
 * $
 */
@SuppressWarnings({ "rawtypes" })
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
