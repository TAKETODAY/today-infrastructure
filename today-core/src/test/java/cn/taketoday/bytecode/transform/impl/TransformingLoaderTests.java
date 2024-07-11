/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.bytecode.transform.impl;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.lang.reflect.Method;

import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.transform.ClassFilter;
import cn.taketoday.bytecode.transform.ClassTransformer;
import cn.taketoday.bytecode.transform.ClassTransformerChain;
import cn.taketoday.bytecode.transform.ClassTransformerFactory;
import cn.taketoday.bytecode.transform.CodeGenTestCase;
import cn.taketoday.bytecode.transform.TransformingClassLoader;
import cn.taketoday.util.ReflectionUtils;

/**
 * @version $Id: TestTransformingLoader.java,v 1.6 2006/03/05 02:43:17
 * herbyderby Exp $
 */
public class TransformingLoaderTests extends CodeGenTestCase {

  private static final ClassFilter TEST_FILTER = new ClassFilter() {
    public boolean accept(String name) {
      printlnError("Loading " + name);
      return name.startsWith("cn.taketoday.bytecode.");
    }
  };

  private ClassTransformer getExampleTransformer(String name, Type type) {
    return new AddPropertyTransformer(new String[] { name }, new Type[] { type });
  }

  public void testExample() throws Exception {
    ClassTransformer t1 = getExampleTransformer("herby", Type.TYPE_STRING);
    ClassTransformer t2 = getExampleTransformer("derby", Type.DOUBLE_TYPE);
    ClassTransformer chain = new ClassTransformerChain(new ClassTransformer[] { t1, t2 });
    Class loaded = loadHelper(chain, Example.class);
    Object obj = ReflectionUtils.newInstance(loaded);
    String value = "HELLO";
    loaded.getMethod("setHerby", new Class[] { String.class }).invoke(obj, value);
    assertEquals(value, loaded.getMethod("getHerby", (Class[]) null).invoke(obj, (Object[]) null));

    loaded.getMethod("setDerby", new Class[] { Double.TYPE }).invoke(obj, 1.23456789d);
  }

  private static Class inited;

  public static void initStatic(Class foo) {
    printlnError("INITING: " + foo);
  }

  public void testAddStatic() throws Exception {
    Method m = ReflectionUtils.findMethod(TransformingLoaderTests.class, "initStatic", Class.class);
    ;
//        CglibReflectUtils.findMethod("cn.taketoday.bytecode.transform.impl.TestTransformingLoader.initStatic(Class)");
    ClassTransformer t = new AddStaticInitTransformer(m);
    // t = new ClassTransformerChain(new ClassTransformer[]{ t, new
    // ClassTransformerTee(new cn.taketoday.bytecode.util.TraceClassVisitor(null, new
    // java.io.PrintWriter(System.out))) });
    Class loaded = loadHelper(t, Example.class);
    Object obj = ReflectionUtils.newInstance(loaded);
    // TODO
  }

  public void testInterceptField() throws Exception {
    ClassTransformer t = new InterceptFieldTransformer(new InterceptFieldFilter() {
      public boolean acceptRead(Type owner, String name) {
        return true;
      }

      public boolean acceptWrite(Type owner, String name) {
        return true;
      }
    });
    Class loaded = loadHelper(t, Example.class);
    // TODO
  }

  private static Class loadHelper(final ClassTransformer t, Class target) throws ClassNotFoundException {
    ClassLoader parent = TransformingLoaderTests.class.getClassLoader();
    TransformingClassLoader loader = new TransformingClassLoader(
            parent,
            TEST_FILTER,

            new ClassTransformerFactory() {
              public ClassTransformer newTransformer() {
                return t;
              }
            }

    );
    return loader.loadClass(target.getName());
  }

  public TransformingLoaderTests(String testName) {
    super(testName);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }

  public static Test suite() {
    return new TestSuite(TransformingLoaderTests.class);
  }

  public void perform(ClassLoader loader) throws Throwable { }

  public void testFailOnMemoryLeak() throws Throwable { }

}
