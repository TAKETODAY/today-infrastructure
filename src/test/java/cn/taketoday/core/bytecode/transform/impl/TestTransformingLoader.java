/*
 * Copyright 2003,2004 The Apache Software Foundation
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
package cn.taketoday.core.bytecode.transform.impl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.transform.ClassFilter;
import cn.taketoday.core.bytecode.transform.ClassTransformer;
import cn.taketoday.core.bytecode.transform.ClassTransformerChain;
import cn.taketoday.core.bytecode.transform.ClassTransformerFactory;
import cn.taketoday.core.bytecode.transform.TransformingClassLoader;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.context.el.ELProcessorTests.printlnError;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @version $Id: TestTransformingLoader.java,v 1.6 2006/03/05 02:43:17
 * herbyderby Exp $
 */
public class TestTransformingLoader {

  private static final ClassFilter TEST_FILTER = new ClassFilter() {
    public boolean accept(String name) {
      printlnError("Loading " + name);
      return name.startsWith("cn.taketoday.core.bytecode.");
    }
  };

  private ClassTransformer getExampleTransformer(String name, Type type) {
    return new AddPropertyTransformer(new String[] { name }, new Type[] { type });
  }
  @Test
  public void testExample() throws Exception {
    ClassTransformer t1 = getExampleTransformer("herby", Type.TYPE_STRING);
    ClassTransformer t2 = getExampleTransformer("derby", Type.DOUBLE_TYPE);
    ClassTransformer chain = new ClassTransformerChain(new ClassTransformer[] { t1, t2 });
    Class loaded = loadHelper(chain, Example.class);
    Object obj = loaded.newInstance();
    String value = "HELLO";
    loaded.getMethod("setHerby", new Class[] { String.class }).invoke(obj, value);
    assertEquals(value, loaded.getMethod("getHerby", (Class[]) null).invoke(obj, (Object[]) null));

    loaded.getMethod("setDerby", new Class[] { Double.TYPE }).invoke(obj, new Double(1.23456789d));
  }

  private static Class inited;

  public static void initStatic(Class foo) {
    printlnError("INITING: " + foo);
  }

  @Test
  public void testAddStatic() throws Exception {
    Method m = ReflectionUtils.findMethod(TestTransformingLoader.class, "initStatic", Class.class);
    ;
//        CglibReflectUtils.findMethod("cn.taketoday.core.bytecode.transform.impl.TestTransformingLoader.initStatic(Class)");
    ClassTransformer t = new AddStaticInitTransformer(m);
    // t = new ClassTransformerChain(new ClassTransformer[]{ t, new
    // ClassTransformerTee(new cn.taketoday.core.bytecode.util.TraceClassVisitor(null, new
    // java.io.PrintWriter(System.out))) });
    Class loaded = loadHelper(t, Example.class);
    Object obj = loaded.newInstance();
    // TODO
  }
  @Test
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

  @Test
  public void testFieldProvider() throws Exception {
    ClassTransformer t = new FieldProviderTransformer();
    Class loaded = loadHelper(t, Example.class);
    // TODO
//         FieldProvider fp = (FieldProvider)loaded.newInstance();
//         assertTrue(((Integer)fp.getField("example")).intValue() == 42);
//         fp.setField("example", new Integer(6));
//         assertTrue(((Integer)fp.getField("example")).intValue() == 6);
//         assertTrue(fp.getField("example") == null);
//         try {
//             fp.getField("dsfjkl");
//             fail("expected exception");
//         } catch (IllegalArgumentException ignore) { }
  }

  private static Class loadHelper(final ClassTransformer t, Class target) throws ClassNotFoundException {
    ClassLoader parent = TestTransformingLoader.class.getClassLoader();
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

}
