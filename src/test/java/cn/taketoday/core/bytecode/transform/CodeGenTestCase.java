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
package cn.taketoday.core.bytecode.transform;

import junit.framework.TestCase;

/**
 * @author Chris Nokleberg
 * <a href="mailto:chris@nokleberg.com">chris@nokleberg.com</a>
 * @version $Id: CodeGenTestCase.java,v 1.10 2012/07/27 16:02:49 baliuka Exp $
 */
public abstract class CodeGenTestCase extends TestCase {

  public CodeGenTestCase(String testName) {
    super(testName);
  }

  public abstract void perform(ClassLoader loader) throws Throwable;

  public static void printlnError(String x) {
//    System.err.println(x);
  }

  public static void println(String x) {
    //    System.out.println(x);
  }
}
