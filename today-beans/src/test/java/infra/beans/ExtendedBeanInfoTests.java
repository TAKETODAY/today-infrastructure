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

package infra.beans;

import org.junit.jupiter.api.Test;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.math.BigDecimal;

import infra.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
public class ExtendedBeanInfoTests {

  @Test
  public void standardReadMethodOnly() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public String getFoo() { return null; }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    assertThat(hasReadMethodForProperty(ebi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isFalse();
  }

  @Test
  public void standardWriteMethodOnly() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public void setFoo(String f) { }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isFalse();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isTrue();

    assertThat(hasReadMethodForProperty(ebi, "foo")).isFalse();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isTrue();
  }

  @Test
  public void standardReadAndWriteMethods() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public void setFoo(String f) { }

      public String getFoo() { return null; }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isTrue();

    assertThat(hasReadMethodForProperty(ebi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isTrue();
  }

  @Test
  public void nonStandardWriteMethodOnly() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public C setFoo(String foo) { return this; }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isFalse();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    assertThat(hasReadMethodForProperty(ebi, "foo")).isFalse();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isTrue();
  }

  @Test
  public void standardReadAndNonStandardWriteMethods() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public String getFoo() { return null; }

      public C setFoo(String foo) { return this; }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    assertThat(hasReadMethodForProperty(ebi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isTrue();
  }

  @Test
  public void standardReadAndNonStandardIndexedWriteMethod() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public String[] getFoo() { return null; }

      public C setFoo(int i, String foo) { return this; }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();
    assertThat(hasIndexedWriteMethodForProperty(bi, "foo")).isFalse();

    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(ebi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isFalse();
    assertThat(hasIndexedWriteMethodForProperty(ebi, "foo")).isTrue();
  }

  @Test
  public void standardReadMethodsAndOverloadedNonStandardWriteMethods() throws Exception {
    @SuppressWarnings("unused")
    class C {
      public String getFoo() { return null; }

      public C setFoo(String foo) { return this; }

      public C setFoo(Number foo) { return this; }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    assertThat(hasReadMethodForProperty(ebi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isTrue();

    for (PropertyDescriptor pd : ebi.getPropertyDescriptors()) {
      if (pd.getName().equals("foo")) {
        assertThat(pd.getWriteMethod()).isEqualTo(C.class.getMethod("setFoo", String.class));
        return;
      }
    }
    throw new AssertionError("never matched write method");
  }

  @Test
  public void cornerSpr9414() throws IntrospectionException {
    @SuppressWarnings("unused")
    class Parent {
      public Number getProperty1() {
        return 1;
      }
    }
    class Child extends Parent {
      @Override
      public Integer getProperty1() {
        return 2;
      }
    }
    { // always passes
      ExtendedBeanInfo bi = new ExtendedBeanInfo(Introspector.getBeanInfo(Parent.class));
      assertThat(hasReadMethodForProperty(bi, "property1")).isTrue();
    }
    {
      ExtendedBeanInfo bi = new ExtendedBeanInfo(Introspector.getBeanInfo(Child.class));
      assertThat(hasReadMethodForProperty(bi, "property1")).isTrue();
    }
  }

  @Test
  public void cornerSpr9453() throws IntrospectionException {
    final class Bean implements Spr9453<Class<?>> {
      @Override
      public Class<?> getProp() {
        return null;
      }
    }
    { // always passes
      BeanInfo info = Introspector.getBeanInfo(Bean.class);
      assertThat(info.getPropertyDescriptors().length).isEqualTo(2);
    }
    {
      BeanInfo info = new ExtendedBeanInfo(Introspector.getBeanInfo(Bean.class));
      assertThat(info.getPropertyDescriptors().length).isEqualTo(2);
    }
  }

  @Test
  public void standardReadMethodInSuperclassAndNonStandardWriteMethodInSubclass() throws Exception {
    @SuppressWarnings("unused")
    class B {
      public String getFoo() { return null; }
    }
    @SuppressWarnings("unused")
    class C extends B {
      public C setFoo(String foo) { return this; }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    assertThat(hasReadMethodForProperty(ebi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isTrue();
  }

  @Test
  public void standardReadMethodInSuperAndSubclassesAndGenericBuilderStyleNonStandardWriteMethodInSuperAndSubclasses() throws Exception {
    abstract class B<This extends B<This>> {
      @SuppressWarnings("unchecked")
      protected final This instance = (This) this;
      private String foo;

      public String getFoo() { return foo; }

      public This setFoo(String foo) {
        this.foo = foo;
        return this.instance;
      }
    }

    class C extends B<C> {
      private int bar = -1;

      public int getBar() { return bar; }

      public C setBar(int bar) {
        this.bar = bar;
        return this.instance;
      }
    }

    C c = new C()
            .setFoo("blue")
            .setBar(42);

    assertThat(c.getFoo()).isEqualTo("blue");
    assertThat(c.getBar()).isEqualTo(42);

    BeanInfo bi = Introspector.getBeanInfo(C.class);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    assertThat(hasReadMethodForProperty(bi, "bar")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "bar")).isFalse();

    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    assertThat(hasReadMethodForProperty(bi, "bar")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "bar")).isFalse();

    assertThat(hasReadMethodForProperty(ebi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isTrue();

    assertThat(hasReadMethodForProperty(ebi, "bar")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "bar")).isTrue();
  }

  @Test
  public void nonPublicStandardReadAndWriteMethods() throws Exception {
    @SuppressWarnings("unused")
    class C {
      String getFoo() { return null; }

      C setFoo(String foo) { return this; }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isFalse();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    assertThat(hasReadMethodForProperty(ebi, "foo")).isFalse();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isFalse();
  }

  /**
   * {@link ExtendedBeanInfo} should behave exactly like {@link BeanInfo}
   * in strange edge cases.
   */
  @Test
  public void readMethodReturnsSupertypeOfWriteMethodParameter() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public Number getFoo() { return null; }

      public void setFoo(Integer foo) { }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasReadMethodForProperty(ebi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isEqualTo(hasWriteMethodForProperty(bi, "foo"));
  }

  @Test
  public void indexedReadMethodReturnsSupertypeOfIndexedWriteMethodParameter() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public Number getFoos(int index) { return null; }

      public void setFoos(int index, Integer foo) { }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasIndexedReadMethodForProperty(bi, "foos")).isTrue();
    assertThat(hasIndexedReadMethodForProperty(ebi, "foos")).isTrue();
    assertThat(hasIndexedWriteMethodForProperty(ebi, "foos")).isEqualTo(hasIndexedWriteMethodForProperty(bi, "foos"));
  }

  /**
   * {@link ExtendedBeanInfo} should behave exactly like {@link BeanInfo}
   * in strange edge cases.
   */
  @Test
  public void readMethodReturnsSubtypeOfWriteMethodParameter() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public Integer getFoo() { return null; }

      public void setFoo(Number foo) { }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    assertThat(hasReadMethodForProperty(ebi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isFalse();
  }

  @Test
  public void indexedReadMethodReturnsSubtypeOfIndexedWriteMethodParameter() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public Integer getFoos(int index) { return null; }

      public void setFoo(int index, Number foo) { }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasIndexedReadMethodForProperty(bi, "foos")).isTrue();
    assertThat(hasIndexedWriteMethodForProperty(bi, "foos")).isFalse();

    assertThat(hasIndexedReadMethodForProperty(ebi, "foos")).isTrue();
    assertThat(hasIndexedWriteMethodForProperty(ebi, "foos")).isFalse();
  }

  @Test
  public void indexedReadMethodOnly() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      // indexed read method
      public String getFoos(int i) { return null; }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    BeanInfo ebi = new ExtendedBeanInfo(Introspector.getBeanInfo(C.class));

    assertThat(hasReadMethodForProperty(bi, "foos")).isFalse();
    assertThat(hasIndexedReadMethodForProperty(bi, "foos")).isTrue();

    assertThat(hasReadMethodForProperty(ebi, "foos")).isFalse();
    assertThat(hasIndexedReadMethodForProperty(ebi, "foos")).isTrue();
  }

  @Test
  public void indexedWriteMethodOnly() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      // indexed write method
      public void setFoos(int i, String foo) { }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    BeanInfo ebi = new ExtendedBeanInfo(Introspector.getBeanInfo(C.class));

    assertThat(hasWriteMethodForProperty(bi, "foos")).isFalse();
    assertThat(hasIndexedWriteMethodForProperty(bi, "foos")).isTrue();

    assertThat(hasWriteMethodForProperty(ebi, "foos")).isFalse();
    assertThat(hasIndexedWriteMethodForProperty(ebi, "foos")).isTrue();
  }

  @Test
  public void indexedReadAndIndexedWriteMethods() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      // indexed read method
      public String getFoos(int i) { return null; }

      // indexed write method
      public void setFoos(int i, String foo) { }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    BeanInfo ebi = new ExtendedBeanInfo(Introspector.getBeanInfo(C.class));

    assertThat(hasReadMethodForProperty(bi, "foos")).isFalse();
    assertThat(hasIndexedReadMethodForProperty(bi, "foos")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foos")).isFalse();
    assertThat(hasIndexedWriteMethodForProperty(bi, "foos")).isTrue();

    assertThat(hasReadMethodForProperty(ebi, "foos")).isFalse();
    assertThat(hasIndexedReadMethodForProperty(ebi, "foos")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "foos")).isFalse();
    assertThat(hasIndexedWriteMethodForProperty(ebi, "foos")).isTrue();
  }

  @Test
  public void readAndWriteAndIndexedReadAndIndexedWriteMethods() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      // read method
      public String[] getFoos() { return null; }

      // indexed read method
      public String getFoos(int i) { return null; }

      // write method
      public void setFoos(String[] foos) { }

      // indexed write method
      public void setFoos(int i, String foo) { }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    BeanInfo ebi = new ExtendedBeanInfo(Introspector.getBeanInfo(C.class));

    assertThat(hasReadMethodForProperty(bi, "foos")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foos")).isTrue();
    assertThat(hasIndexedReadMethodForProperty(bi, "foos")).isTrue();
    assertThat(hasIndexedWriteMethodForProperty(bi, "foos")).isTrue();

    assertThat(hasReadMethodForProperty(ebi, "foos")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "foos")).isTrue();
    assertThat(hasIndexedReadMethodForProperty(ebi, "foos")).isTrue();
    assertThat(hasIndexedWriteMethodForProperty(ebi, "foos")).isTrue();
  }

  @Test
  public void indexedReadAndNonStandardIndexedWrite() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      // indexed read method
      public String getFoos(int i) { return null; }

      // non-standard indexed write method
      public C setFoos(int i, String foo) { return this; }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);

    assertThat(hasIndexedReadMethodForProperty(bi, "foos")).isTrue();
    // interesting! standard Inspector picks up non-void return types on indexed write methods by default
    assertThat(hasIndexedWriteMethodForProperty(bi, "foos")).isFalse();

    BeanInfo ebi = new ExtendedBeanInfo(Introspector.getBeanInfo(C.class));

    assertThat(hasIndexedReadMethodForProperty(ebi, "foos")).isTrue();
    assertThat(hasIndexedWriteMethodForProperty(ebi, "foos")).isTrue();
  }

  @Test
  public void indexedReadAndNonStandardWriteAndNonStandardIndexedWrite() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      // non-standard write method
      public C setFoos(String[] foos) { return this; }

      // indexed read method
      public String getFoos(int i) { return null; }

      // non-standard indexed write method
      public C setFoos(int i, String foo) { return this; }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);

    assertThat(hasIndexedReadMethodForProperty(bi, "foos")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foos")).isFalse();
    // again as above, standard Inspector picks up non-void return types on indexed write methods by default
    assertThat(hasIndexedWriteMethodForProperty(bi, "foos")).isFalse();

    BeanInfo ebi = new ExtendedBeanInfo(Introspector.getBeanInfo(C.class));

    assertThat(hasIndexedReadMethodForProperty(bi, "foos")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foos")).isFalse();
    assertThat(hasIndexedWriteMethodForProperty(bi, "foos")).isFalse();

    assertThat(hasIndexedReadMethodForProperty(ebi, "foos")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "foos")).isTrue();
    assertThat(hasIndexedWriteMethodForProperty(ebi, "foos")).isTrue();
  }

  @Test
  public void cornerSpr9702() throws IntrospectionException {
    { // baseline with standard write method
      @SuppressWarnings("unused")
      class C {
        // VOID-RETURNING, NON-INDEXED write method
        public void setFoos(String[] foos) { }

        // indexed read method
        public String getFoos(int i) { return null; }
      }

      BeanInfo bi = Introspector.getBeanInfo(C.class);
      assertThat(hasReadMethodForProperty(bi, "foos")).isFalse();
      assertThat(hasIndexedReadMethodForProperty(bi, "foos")).isTrue();
      assertThat(hasWriteMethodForProperty(bi, "foos")).isTrue();
      assertThat(hasIndexedWriteMethodForProperty(bi, "foos")).isFalse();

      BeanInfo ebi = Introspector.getBeanInfo(C.class);
      assertThat(hasReadMethodForProperty(ebi, "foos")).isFalse();
      assertThat(hasIndexedReadMethodForProperty(ebi, "foos")).isTrue();
      assertThat(hasWriteMethodForProperty(ebi, "foos")).isTrue();
      assertThat(hasIndexedWriteMethodForProperty(ebi, "foos")).isFalse();
    }
    { // variant with non-standard write method
      @SuppressWarnings("unused")
      class C {
        // NON-VOID-RETURNING, NON-INDEXED write method
        public C setFoos(String[] foos) { return this; }

        // indexed read method
        public String getFoos(int i) { return null; }
      }

      BeanInfo bi = Introspector.getBeanInfo(C.class);
      assertThat(hasReadMethodForProperty(bi, "foos")).isFalse();
      assertThat(hasIndexedReadMethodForProperty(bi, "foos")).isTrue();
      assertThat(hasWriteMethodForProperty(bi, "foos")).isFalse();
      assertThat(hasIndexedWriteMethodForProperty(bi, "foos")).isFalse();

      BeanInfo ebi = new ExtendedBeanInfo(Introspector.getBeanInfo(C.class));
      assertThat(hasReadMethodForProperty(ebi, "foos")).isFalse();
      assertThat(hasIndexedReadMethodForProperty(ebi, "foos")).isTrue();
      assertThat(hasWriteMethodForProperty(ebi, "foos")).isTrue();
      assertThat(hasIndexedWriteMethodForProperty(ebi, "foos")).isFalse();
    }
  }

  /**
   * this method would throw an
   * IntrospectionException regarding a "type mismatch between indexed and non-indexed
   * methods" intermittently (approximately one out of every four times) under JDK 7
   * due to non-deterministic results from {@link Class#getDeclaredMethods()}.
   * See https://bugs.java.com/view_bug.do?bug_id=7023180
   *
   * @see #cornerSpr9702()
   */
  @Test
  public void cornerSpr10111() throws Exception {
    new ExtendedBeanInfo(Introspector.getBeanInfo(BigDecimal.class));
  }

  @Test
  public void subclassWriteMethodWithCovariantReturnType() throws IntrospectionException {
    @SuppressWarnings("unused")
    class B {
      public String getFoo() { return null; }

      public Number setFoo(String foo) { return null; }
    }
    class C extends B {
      @Override
      public String getFoo() { return null; }

      @Override
      public Integer setFoo(String foo) { return null; }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    assertThat(hasReadMethodForProperty(ebi, "foo")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isTrue();

    assertThat(ebi.getPropertyDescriptors().length).isEqualTo(bi.getPropertyDescriptors().length);
  }

  @Test
  public void nonStandardReadMethodAndStandardWriteMethod() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public void getFoo() { }

      public void setFoo(String foo) { }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isFalse();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isTrue();

    assertThat(hasReadMethodForProperty(ebi, "foo")).isFalse();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isTrue();
  }

  /**
   * Ensures that an empty string is not passed into a PropertyDescriptor constructor. This
   * could occur when handling ArrayList.set(int,Object)
   */
  @Test
  public void emptyPropertiesIgnored() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public Object set(Object o) { return null; }

      public Object set(int i, Object o) { return null; }
    }

    BeanInfo bi = Introspector.getBeanInfo(C.class);
    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(ebi.getPropertyDescriptors()).isEqualTo(bi.getPropertyDescriptors());
  }

  @Test
  public void overloadedNonStandardWriteMethodsOnly_orderA() throws IntrospectionException, SecurityException, NoSuchMethodException {
    @SuppressWarnings("unused")
    class C {
      public Object setFoo(String p) { return new Object(); }

      public Object setFoo(int p) { return new Object(); }
    }
    BeanInfo bi = Introspector.getBeanInfo(C.class);

    assertThat(hasReadMethodForProperty(bi, "foo")).isFalse();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isFalse();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    assertThat(hasReadMethodForProperty(ebi, "foo")).isFalse();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isTrue();

    for (PropertyDescriptor pd : ebi.getPropertyDescriptors()) {
      if (pd.getName().equals("foo")) {
        assertThat(pd.getWriteMethod()).isEqualTo(C.class.getMethod("setFoo", String.class));
        return;
      }
    }
    throw new AssertionError("never matched write method");
  }

  @Test
  public void overloadedNonStandardWriteMethodsOnly_orderB() throws IntrospectionException, SecurityException, NoSuchMethodException {
    @SuppressWarnings("unused")
    class C {
      public Object setFoo(int p) { return new Object(); }

      public Object setFoo(String p) { return new Object(); }
    }
    BeanInfo bi = Introspector.getBeanInfo(C.class);

    assertThat(hasReadMethodForProperty(bi, "foo")).isFalse();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "foo")).isFalse();
    assertThat(hasWriteMethodForProperty(bi, "foo")).isFalse();

    assertThat(hasReadMethodForProperty(ebi, "foo")).isFalse();
    assertThat(hasWriteMethodForProperty(ebi, "foo")).isTrue();

    for (PropertyDescriptor pd : ebi.getPropertyDescriptors()) {
      if (pd.getName().equals("foo")) {
        assertThat(pd.getWriteMethod()).isEqualTo(C.class.getMethod("setFoo", String.class));
        return;
      }
    }
    throw new AssertionError("never matched write method");
  }

  /**
   * which an (apparently) indexed write method
   * without a corresponding indexed read method would fail to be processed correctly by
   * ExtendedBeanInfo. The local class C below represents the relevant methods from
   * Google's GsonBuilder class. Interestingly, the setDateFormat(int, int) method was
   * not actually intended to serve as an indexed write method; it just appears that way.
   */
  @Test
  public void reproSpr8522() throws IntrospectionException {
    @SuppressWarnings("unused")
    class C {
      public Object setDateFormat(String pattern) { return new Object(); }

      public Object setDateFormat(int style) { return new Object(); }

      public Object setDateFormat(int dateStyle, int timeStyle) { return new Object(); }
    }
    BeanInfo bi = Introspector.getBeanInfo(C.class);

    assertThat(hasReadMethodForProperty(bi, "dateFormat")).isFalse();
    assertThat(hasWriteMethodForProperty(bi, "dateFormat")).isFalse();
    assertThat(hasIndexedReadMethodForProperty(bi, "dateFormat")).isFalse();
    assertThat(hasIndexedWriteMethodForProperty(bi, "dateFormat")).isFalse();

    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "dateFormat")).isFalse();
    assertThat(hasWriteMethodForProperty(bi, "dateFormat")).isFalse();
    assertThat(hasIndexedReadMethodForProperty(bi, "dateFormat")).isFalse();
    assertThat(hasIndexedWriteMethodForProperty(bi, "dateFormat")).isFalse();

    assertThat(hasReadMethodForProperty(ebi, "dateFormat")).isFalse();
    assertThat(hasWriteMethodForProperty(ebi, "dateFormat")).isTrue();
    assertThat(hasIndexedReadMethodForProperty(ebi, "dateFormat")).isFalse();
    assertThat(hasIndexedWriteMethodForProperty(ebi, "dateFormat")).isFalse();
  }

  @Test
  public void propertyCountsMatch() throws IntrospectionException {
    BeanInfo bi = Introspector.getBeanInfo(TestBean.class);
    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(ebi.getPropertyDescriptors().length).isEqualTo(bi.getPropertyDescriptors().length);
  }

  @Test
  public void propertyCountsWithNonStandardWriteMethod() throws IntrospectionException {
    class ExtendedTestBean extends TestBean {
      @SuppressWarnings("unused")
      public ExtendedTestBean setFoo(String s) { return this; }
    }
    BeanInfo bi = Introspector.getBeanInfo(ExtendedTestBean.class);
    BeanInfo ebi = new ExtendedBeanInfo(bi);

    boolean found = false;
    for (PropertyDescriptor pd : ebi.getPropertyDescriptors()) {
      if (pd.getName().equals("foo")) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();
    assertThat(ebi.getPropertyDescriptors().length).isEqualTo(bi.getPropertyDescriptors().length + 1);
  }

  /**
   * {@link BeanInfo#getPropertyDescriptors()} returns alphanumerically sorted.
   * Test that {@link ExtendedBeanInfo#getPropertyDescriptors()} does the same.
   */
  @Test
  public void propertyDescriptorOrderIsEqual() throws IntrospectionException {
    BeanInfo bi = Introspector.getBeanInfo(TestBean.class);
    BeanInfo ebi = new ExtendedBeanInfo(bi);

    for (int i = 0; i < bi.getPropertyDescriptors().length; i++) {
      assertThat(ebi.getPropertyDescriptors()[i].getName()).isEqualTo(bi.getPropertyDescriptors()[i].getName());
    }
  }

  @Test
  public void propertyDescriptorComparator() throws IntrospectionException {
    ExtendedBeanInfo.PropertyDescriptorComparator c = new ExtendedBeanInfo.PropertyDescriptorComparator();

    assertThat(c.compare(new PropertyDescriptor("a", null, null), new PropertyDescriptor("a", null, null))).isEqualTo(0);
    assertThat(c.compare(new PropertyDescriptor("abc", null, null), new PropertyDescriptor("abc", null, null))).isEqualTo(0);
    assertThat(c.compare(new PropertyDescriptor("a", null, null), new PropertyDescriptor("b", null, null))).isLessThan(0);
    assertThat(c.compare(new PropertyDescriptor("b", null, null), new PropertyDescriptor("a", null, null))).isGreaterThan(0);
    assertThat(c.compare(new PropertyDescriptor("abc", null, null), new PropertyDescriptor("abd", null, null))).isLessThan(0);
    assertThat(c.compare(new PropertyDescriptor("xyz", null, null), new PropertyDescriptor("123", null, null))).isGreaterThan(0);
    assertThat(c.compare(new PropertyDescriptor("a", null, null), new PropertyDescriptor("abc", null, null))).isLessThan(0);
    assertThat(c.compare(new PropertyDescriptor("abc", null, null), new PropertyDescriptor("a", null, null))).isGreaterThan(0);
    assertThat(c.compare(new PropertyDescriptor("abc", null, null), new PropertyDescriptor("b", null, null))).isLessThan(0);

    assertThat(c.compare(new PropertyDescriptor(" ", null, null), new PropertyDescriptor("a", null, null))).isLessThan(0);
    assertThat(c.compare(new PropertyDescriptor("1", null, null), new PropertyDescriptor("a", null, null))).isLessThan(0);
    assertThat(c.compare(new PropertyDescriptor("a", null, null), new PropertyDescriptor("A", null, null))).isGreaterThan(0);
  }

  @Test
  public void reproSpr8806() throws IntrospectionException {
    // does not throw
    Introspector.getBeanInfo(LawLibrary.class);

    new ExtendedBeanInfo(Introspector.getBeanInfo(LawLibrary.class));
  }

  @Test
  public void cornerSpr8949() throws IntrospectionException {
    class A {
      @SuppressWarnings("unused")
      public boolean isTargetMethod() {
        return false;
      }
    }

    class B extends A {
      @Override
      public boolean isTargetMethod() {
        return false;
      }
    }

    BeanInfo bi = Introspector.getBeanInfo(B.class);

    // java.beans.Introspector returns the "wrong" declaring class for overridden read
    // methods, which in turn violates expectations in {@link ExtendedBeanInfo} regarding
    // method equality. Framework's {@link ClassUtils#getMostSpecificMethod(Method, Class)}
    // helps out here, and is now put into use in ExtendedBeanInfo as well.
    BeanInfo ebi = new ExtendedBeanInfo(bi);

    assertThat(hasReadMethodForProperty(bi, "targetMethod")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "targetMethod")).isFalse();

    assertThat(hasReadMethodForProperty(ebi, "targetMethod")).isTrue();
    assertThat(hasWriteMethodForProperty(ebi, "targetMethod")).isFalse();
  }

  @Test
  public void cornerSpr8937AndSpr12582() throws IntrospectionException {
    @SuppressWarnings("unused")
    class A {
      public void setAddress(String addr) { }

      public void setAddress(int index, String addr) { }

      public String getAddress(int index) { return null; }
    }

    // Baseline:
    BeanInfo bi = Introspector.getBeanInfo(A.class);
    boolean hasReadMethod = hasReadMethodForProperty(bi, "address");
    boolean hasWriteMethod = hasWriteMethodForProperty(bi, "address");
    boolean hasIndexedReadMethod = hasIndexedReadMethodForProperty(bi, "address");
    boolean hasIndexedWriteMethod = hasIndexedWriteMethodForProperty(bi, "address");

    // ExtendedBeanInfo needs to behave exactly like BeanInfo...
    BeanInfo ebi = new ExtendedBeanInfo(bi);
    assertThat(hasReadMethodForProperty(ebi, "address")).isEqualTo(hasReadMethod);
    assertThat(hasWriteMethodForProperty(ebi, "address")).isEqualTo(hasWriteMethod);
    assertThat(hasIndexedReadMethodForProperty(ebi, "address")).isEqualTo(hasIndexedReadMethod);
    assertThat(hasIndexedWriteMethodForProperty(ebi, "address")).isEqualTo(hasIndexedWriteMethod);
  }

  @Test
  public void shouldSupportStaticWriteMethod() throws IntrospectionException {
    {
      BeanInfo bi = Introspector.getBeanInfo(WithStaticWriteMethod.class);
      assertThat(hasReadMethodForProperty(bi, "prop1")).isFalse();
      assertThat(hasWriteMethodForProperty(bi, "prop1")).isFalse();
      assertThat(hasIndexedReadMethodForProperty(bi, "prop1")).isFalse();
      assertThat(hasIndexedWriteMethodForProperty(bi, "prop1")).isFalse();
    }
    {
      BeanInfo bi = new ExtendedBeanInfo(Introspector.getBeanInfo(WithStaticWriteMethod.class));
      assertThat(hasReadMethodForProperty(bi, "prop1")).isFalse();
      assertThat(hasWriteMethodForProperty(bi, "prop1")).isTrue();
      assertThat(hasIndexedReadMethodForProperty(bi, "prop1")).isFalse();
      assertThat(hasIndexedWriteMethodForProperty(bi, "prop1")).isFalse();
    }
  }

  @Test
  public void shouldDetectValidPropertiesAndIgnoreInvalidProperties() throws IntrospectionException {
    BeanInfo bi = new ExtendedBeanInfo(Introspector.getBeanInfo(java.awt.Window.class));
    assertThat(hasReadMethodForProperty(bi, "locationByPlatform")).isTrue();
    assertThat(hasWriteMethodForProperty(bi, "locationByPlatform")).isTrue();
    assertThat(hasIndexedReadMethodForProperty(bi, "locationByPlatform")).isFalse();
    assertThat(hasIndexedWriteMethodForProperty(bi, "locationByPlatform")).isFalse();
  }

  private boolean hasWriteMethodForProperty(BeanInfo beanInfo, String propertyName) {
    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
      if (pd.getName().equals(propertyName)) {
        return pd.getWriteMethod() != null;
      }
    }
    return false;
  }

  private boolean hasReadMethodForProperty(BeanInfo beanInfo, String propertyName) {
    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
      if (pd.getName().equals(propertyName)) {
        return pd.getReadMethod() != null;
      }
    }
    return false;
  }

  private boolean hasIndexedWriteMethodForProperty(BeanInfo beanInfo, String propertyName) {
    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
      if (pd.getName().equals(propertyName)) {
        if (!(pd instanceof IndexedPropertyDescriptor)) {
          return false;
        }
        return ((IndexedPropertyDescriptor) pd).getIndexedWriteMethod() != null;
      }
    }
    return false;
  }

  private boolean hasIndexedReadMethodForProperty(BeanInfo beanInfo, String propertyName) {
    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
      if (pd.getName().equals(propertyName)) {
        if (!(pd instanceof IndexedPropertyDescriptor)) {
          return false;
        }
        return ((IndexedPropertyDescriptor) pd).getIndexedReadMethod() != null;
      }
    }
    return false;
  }

  interface Spr9453<T> {

    T getProp();
  }

  interface Book {
  }

  interface TextBook extends Book {
  }

  interface LawBook extends TextBook {
  }

  interface BookOperations {

    Book getBook();

    void setBook(Book book);
  }

  interface TextBookOperations extends BookOperations {

    @Override
    TextBook getBook();
  }

  abstract class Library {

    public Book getBook() {
      return null;
    }

    public void setBook(Book book) {
    }
  }

  class LawLibrary extends Library implements TextBookOperations {

    @Override
    public LawBook getBook() {
      return null;
    }
  }

  static class WithStaticWriteMethod {

    public static void setProp1(String prop1) {
    }
  }

}
