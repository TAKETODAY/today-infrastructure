/*
 * Copyright 2002,2003,2004 The Apache Software Foundation
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
package cn.taketoday.context.cglib;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import cn.taketoday.context.cglib.beans.TestBeanCopier;
import cn.taketoday.context.cglib.beans.TestBeanGenerator;
import cn.taketoday.context.cglib.beans.TestBeanMap;
import cn.taketoday.context.cglib.beans.TestBulkBean;
import cn.taketoday.context.cglib.beans.TestImmutableBean;
import cn.taketoday.context.cglib.core.DebuggingClassWriter;
import cn.taketoday.context.cglib.core.TestKeyFactory;
import cn.taketoday.context.cglib.proxy.TestDispatcher;
import cn.taketoday.context.cglib.proxy.TestEnhancer;
import cn.taketoday.context.cglib.proxy.TestInterfaceMaker;
import cn.taketoday.context.cglib.proxy.TestLazyLoader;
import cn.taketoday.context.cglib.proxy.TestMixin;
import cn.taketoday.context.cglib.proxy.TestNoOp;
import cn.taketoday.context.cglib.proxy.TestProxy;
import cn.taketoday.context.cglib.proxy.TestProxyRefDispatcher;
import cn.taketoday.context.cglib.reflect.TestDelegates;
import cn.taketoday.context.cglib.reflect.TestFastClass;
import cn.taketoday.context.cglib.transform.impl.TestAddClassInit;
import cn.taketoday.context.cglib.transform.impl.TestAddDelegate;
import cn.taketoday.context.cglib.transform.impl.TestDemo;
import cn.taketoday.context.cglib.transform.impl.TestInterceptFields;
import cn.taketoday.context.cglib.transform.impl.TestProvideFields;
import cn.taketoday.context.cglib.transform.impl.TestTransformingLoader;
import cn.taketoday.context.cglib.util.TestParallelSorter;

/**
 * @author Gerhard Froehlich <a href="mailto:g-froehlich@gmx.de">
 * g-froehlich@gmx.de</a>
 * @version $Id: TestAll.java,v 1.66 2004/12/23 03:46:25 herbyderby Exp $
 */
public class TestAll extends TestCase {

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
