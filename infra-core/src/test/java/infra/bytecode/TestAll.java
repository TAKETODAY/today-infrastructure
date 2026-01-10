// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.

// Modifications Copyright 2017 - 2026 the TODAY authors.
package infra.bytecode;

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
