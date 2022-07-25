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
package cn.taketoday.bytecode.transform;

import junit.framework.TestCase;

/**
 * @author baliuka $Id: AbstractTransformTest.java,v 1.8 2004/06/24 21:15:16
 * herbyderby Exp $
 */
abstract public class AbstractTransformTest extends TestCase {

  /** Creates a new instance of AbstractTransformTest */
  public AbstractTransformTest() {
    super(null);
  }

  /** Creates a new instance of AbstractTransformTest */
  public AbstractTransformTest(String s) {
    super(s);
  }

  protected abstract ClassTransformerFactory getTransformer() throws Exception;

  public Class transform() throws Exception {
    ClassLoader loader = new TransformingClassLoader(AbstractTransformTest.class.getClassLoader(), new ClassFilter() {
      public boolean accept(String name) {
        return !(name.startsWith("java") || name.startsWith("junit") || name.endsWith("Exclude"));
      }
    }, getTransformer());
    try {
      return loader.loadClass(getClass().getName());
    }
    catch (Exception e) {
      e.printStackTrace(System.err);
      throw e;
    }
  }

  protected void postProcess(Class c) { }
}
