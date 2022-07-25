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
package cn.taketoday.bytecode;

/**
 * @author Gerhard Froehlich <a href="mailto:g-froehlich@gmx.de">
 * g-froehlich@gmx.de</a>
 * @version $Id: TestAll.java,v 1.66 2004/12/23 03:46:25 herbyderby Exp $
 */
/*
public class TestAll {

//  static {
//    final PrintStream printStream = new PrintStream(new NopFilterOutputStream(), true) {
//      @Override
//      public void write(int b) {}
//
//      @Override
//      public void write(byte[] buf, int off, int len) { }
//
//      @Override
//      public void write(byte[] b) {}
//    };
//    System.setOut(printStream);
//    System.setErr(printStream);
//  }

  public static String DEFAULT_DEBUG_LOACATION = System.getProperty("user.home") + System.getProperty("file.separator") + "cglib-debug";

  public TestAll(String testName) {
    super(testName);
  }

  public static Test suite() throws Exception {

    System.getProperties().list(System.out);
    TestSuite suite = new TestSuite();

    // security

    // proxy
    suite.addTest(TestEnhancer.suite());
    suite.addTest(TestProxy.suite());
    suite.addTest(TestDispatcher.suite());
    suite.addTest(TestProxyRefDispatcher.suite());
    suite.addTest(TestLazyLoader.suite());
    suite.addTest(TestNoOp.suite());
    suite.addTest(TestMixin.suite());
    suite.addTest(TestInterfaceMaker.suite());

    // beans
    suite.addTest(TestBulkBean.suite());
    suite.addTest(TestBeanMap.suite());
    suite.addTest(TestImmutableBean.suite());
    suite.addTest(TestBeanCopier.suite());
    suite.addTest(TestBeanGenerator.suite());

    // reflect
    suite.addTest(TestDelegates.suite());
    suite.addTest(TestFastClass.suite());

    // core
    suite.addTest(TestKeyFactory.suite());

    // util
    suite.addTest(TestParallelSorter.suite());

    // transform
    suite.addTest(TestTransformingLoader.suite());
    suite.addTest(TestAddClassInit.suite());
    suite.addTest(TestProvideFields.suite());
    suite.addTest(TestAddDelegate.suite());
    suite.addTest(TestInterceptFields.suite());
    suite.addTest(TestDemo.suite());

    // performance
    // suite.addTest(TestReflectPerf.suite());
    // suite.addTest(TestXmlParsing.suite());
    return suite;
  }

  public static void main(String args[]) throws Exception {

    if (System.getProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY) == null) {
      System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, DEFAULT_DEBUG_LOACATION);
    }
    String[] testCaseName = { TestAll.class.getName() };
    junit.textui.TestRunner.main(testCaseName);

  }
}
*/
