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

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ObjectUtils;

/**
 * Decorator for a standard {@link BeanInfo} object, e.g. as created by
 * {@link Introspector#getBeanInfo(Class)}, designed to discover and register
 * static and/or non-void returning setter methods. For example:
 *
 * <pre class="code">
 * public class Bean {
 *
 *     private Foo foo;
 *
 *     public Foo getFoo() {
 *         return this.foo;
 *     }
 *
 *     public Bean setFoo(Foo foo) {
 *         this.foo = foo;
 *         return this;
 *     }
 * }</pre>
 *
 * The standard JavaBeans {@code Introspector} will discover the {@code getFoo} read
 * method, but will bypass the {@code #setFoo(Foo)} write method, because its non-void
 * returning signature does not comply with the JavaBeans specification.
 * {@code ExtendedBeanInfo}, on the other hand, will recognize and include it. This is
 * designed to allow APIs with "builder" or method-chaining style setter signatures to be
 * used within Framework {@code <beans>} XML. {@link #getPropertyDescriptors()} returns all
 * existing property descriptors from the wrapped {@code BeanInfo} as well any added for
 * non-void returning setters. Both standard ("non-indexed") and
 * <a href="https://docs.oracle.com/javase/tutorial/javabeans/writing/properties.html">
 * indexed properties</a> are fully supported.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #ExtendedBeanInfo(BeanInfo)
 * @see ExtendedBeanInfoFactory
 * @see CachedIntrospectionResults
 * @since 4.0 2022/2/23 11:27
 */
class ExtendedBeanInfo implements BeanInfo {

  private static final Logger logger = LoggerFactory.getLogger(ExtendedBeanInfo.class);

  private final BeanInfo delegate;

  private final TreeSet<PropertyDescriptor> propertyDescriptors = new TreeSet<>(new PropertyDescriptorComparator());

  /**
   * Wrap the given {@link BeanInfo} instance; copy all its existing property descriptors
   * locally, wrapping each in a custom {@link SimpleIndexedPropertyDescriptor indexed}
   * or {@link SimplePropertyDescriptor non-indexed} {@code PropertyDescriptor}
   * variant that bypasses default JDK weak/soft reference management; then search
   * through its method descriptors to find any non-void returning write methods and
   * update or create the corresponding {@link PropertyDescriptor} for each one found.
   *
   * @param delegate the wrapped {@code BeanInfo}, which is never modified
   * @see #getPropertyDescriptors()
   */
  public ExtendedBeanInfo(BeanInfo delegate) {
    this.delegate = delegate;
    for (PropertyDescriptor pd : delegate.getPropertyDescriptors()) {
      try {
        propertyDescriptors.add(
                pd instanceof IndexedPropertyDescriptor ?
                new SimpleIndexedPropertyDescriptor((IndexedPropertyDescriptor) pd) :
                new SimplePropertyDescriptor(pd));
      }
      catch (IntrospectionException ex) {
        // Probably simply a method that wasn't meant to follow the JavaBeans pattern...
        logger.debug("Ignoring invalid bean property '{}': {}", pd.getName(), ex.getMessage());
      }
    }
    MethodDescriptor[] methodDescriptors = delegate.getMethodDescriptors();
    if (methodDescriptors != null) {
      for (Method method : findCandidateWriteMethods(methodDescriptors)) {
        try {
          handleCandidateWriteMethod(method);
        }
        catch (IntrospectionException ex) {
          // We're only trying to find candidates, can easily ignore extra ones here...
          logger.debug("Ignoring candidate write method [{}]: {}", method, ex.getMessage());
        }
      }
    }
  }

  private List<Method> findCandidateWriteMethods(MethodDescriptor[] methodDescriptors) {
    ArrayList<Method> matches = new ArrayList<>();
    for (MethodDescriptor methodDescriptor : methodDescriptors) {
      Method method = methodDescriptor.getMethod();
      if (isCandidateWriteMethod(method)) {
        matches.add(method);
      }
    }
    // Sort non-void returning write methods to guard against the ill effects of
    // non-deterministic sorting of methods returned from Class#getDeclaredMethods
    // under JDK 7. See https://bugs.java.com/view_bug.do?bug_id=7023180
    matches.sort(Comparator.comparing(Method::toString).reversed());
    return matches;
  }

  public static boolean isCandidateWriteMethod(Method method) {
    String methodName = method.getName();
    int nParams = method.getParameterCount();
    return (methodName.length() > 3
            && methodName.startsWith("set")
            && Modifier.isPublic(method.getModifiers())
            && (!void.class.isAssignableFrom(method.getReturnType()) || Modifier.isStatic(method.getModifiers()))
            && (nParams == 1 || (nParams == 2 && int.class == method.getParameterTypes()[0])));
  }

  private void handleCandidateWriteMethod(Method method) throws IntrospectionException {
    int nParams = method.getParameterCount();
    String propertyName = propertyNameFor(method);
    Class<?> propertyType = method.getParameterTypes()[nParams - 1];
    PropertyDescriptor existingPd = findExistingPropertyDescriptor(propertyName, propertyType);
    if (nParams == 1) {
      if (existingPd == null) {
        this.propertyDescriptors.add(new SimplePropertyDescriptor(propertyName, null, method));
      }
      else {
        existingPd.setWriteMethod(method);
      }
    }
    else if (nParams == 2) {
      if (existingPd == null) {
        this.propertyDescriptors.add(
                new SimpleIndexedPropertyDescriptor(propertyName, null, null, null, method));
      }
      else if (existingPd instanceof IndexedPropertyDescriptor) {
        ((IndexedPropertyDescriptor) existingPd).setIndexedWriteMethod(method);
      }
      else {
        this.propertyDescriptors.remove(existingPd);
        this.propertyDescriptors.add(new SimpleIndexedPropertyDescriptor(
                propertyName, existingPd.getReadMethod(), existingPd.getWriteMethod(), null, method));
      }
    }
    else {
      throw new IllegalArgumentException("Write method must have exactly 1 or 2 parameters: " + method);
    }
  }

  @Nullable
  private PropertyDescriptor findExistingPropertyDescriptor(String propertyName, Class<?> propertyType) {
    for (PropertyDescriptor pd : this.propertyDescriptors) {
      final Class<?> candidateType;
      final String candidateName = pd.getName();
      if (pd instanceof IndexedPropertyDescriptor ipd) {
        candidateType = ipd.getIndexedPropertyType();
        if (candidateName.equals(propertyName) &&
                (candidateType.equals(propertyType) || candidateType.equals(propertyType.getComponentType()))) {
          return pd;
        }
      }
      else {
        candidateType = pd.getPropertyType();
        if (candidateName.equals(propertyName) &&
                (candidateType.equals(propertyType) || propertyType.equals(candidateType.getComponentType()))) {
          return pd;
        }
      }
    }
    return null;
  }

  private String propertyNameFor(Method method) {
    return Introspector.decapitalize(method.getName().substring(3));
  }

  /**
   * Return the set of {@link PropertyDescriptor PropertyDescriptors} from the wrapped
   * {@link BeanInfo} object as well as {@code PropertyDescriptors} for each non-void
   * returning setter method found during construction.
   *
   * @see #ExtendedBeanInfo(BeanInfo)
   */
  @Override
  public PropertyDescriptor[] getPropertyDescriptors() {
    return this.propertyDescriptors.toArray(new PropertyDescriptor[0]);
  }

  @Override
  public BeanInfo[] getAdditionalBeanInfo() {
    return this.delegate.getAdditionalBeanInfo();
  }

  @Override
  public BeanDescriptor getBeanDescriptor() {
    return this.delegate.getBeanDescriptor();
  }

  @Override
  public int getDefaultEventIndex() {
    return this.delegate.getDefaultEventIndex();
  }

  @Override
  public int getDefaultPropertyIndex() {
    return this.delegate.getDefaultPropertyIndex();
  }

  @Override
  public EventSetDescriptor[] getEventSetDescriptors() {
    return this.delegate.getEventSetDescriptors();
  }

  @Override
  public Image getIcon(int iconKind) {
    return this.delegate.getIcon(iconKind);
  }

  @Override
  public MethodDescriptor[] getMethodDescriptors() {
    return this.delegate.getMethodDescriptors();
  }

  /**
   * A simple {@link PropertyDescriptor}.
   */
  static class SimplePropertyDescriptor extends PropertyDescriptor {

    @Nullable
    private Method readMethod;

    @Nullable
    private Method writeMethod;

    @Nullable
    private Class<?> propertyType;

    @Nullable
    private Class<?> propertyEditorClass;

    public SimplePropertyDescriptor(PropertyDescriptor original) throws IntrospectionException {
      this(original.getName(), original.getReadMethod(), original.getWriteMethod());
      PropertyDescriptorUtils.copyNonMethodProperties(original, this);
    }

    public SimplePropertyDescriptor(String propertyName, @Nullable Method readMethod, @Nullable Method writeMethod)
            throws IntrospectionException {

      super(propertyName, null, null);
      this.readMethod = readMethod;
      this.writeMethod = writeMethod;
      this.propertyType = PropertyDescriptorUtils.findPropertyType(readMethod, writeMethod);
    }

    @Override
    @Nullable
    public Method getReadMethod() {
      return this.readMethod;
    }

    @Override
    public void setReadMethod(@Nullable Method readMethod) {
      this.readMethod = readMethod;
    }

    @Override
    @Nullable
    public Method getWriteMethod() {
      return this.writeMethod;
    }

    @Override
    public void setWriteMethod(@Nullable Method writeMethod) {
      this.writeMethod = writeMethod;
    }

    @Override
    @Nullable
    public Class<?> getPropertyType() {
      if (this.propertyType == null) {
        try {
          this.propertyType = PropertyDescriptorUtils.findPropertyType(this.readMethod, this.writeMethod);
        }
        catch (IntrospectionException ex) {
          // Ignore, as does PropertyDescriptor#getPropertyType
        }
      }
      return this.propertyType;
    }

    @Override
    @Nullable
    public Class<?> getPropertyEditorClass() {
      return this.propertyEditorClass;
    }

    @Override
    public void setPropertyEditorClass(@Nullable Class<?> propertyEditorClass) {
      this.propertyEditorClass = propertyEditorClass;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return (this == other || (other instanceof PropertyDescriptor &&
              PropertyDescriptorUtils.equals(this, (PropertyDescriptor) other)));
    }

    @Override
    public int hashCode() {
      return (ObjectUtils.nullSafeHashCode(getReadMethod()) * 29 + ObjectUtils.nullSafeHashCode(getWriteMethod()));
    }

    @Override
    public String toString() {
      return String.format("%s[name=%s, propertyType=%s, readMethod=%s, writeMethod=%s]",
              getClass().getSimpleName(), getName(), getPropertyType(), this.readMethod, this.writeMethod);
    }
  }

  /**
   * A simple {@link IndexedPropertyDescriptor}.
   */
  static class SimpleIndexedPropertyDescriptor extends IndexedPropertyDescriptor {

    @Nullable
    private Method readMethod;

    @Nullable
    private Method writeMethod;

    @Nullable
    private Class<?> propertyType;

    @Nullable
    private Method indexedReadMethod;

    @Nullable
    private Method indexedWriteMethod;

    @Nullable
    private Class<?> indexedPropertyType;

    @Nullable
    private Class<?> propertyEditorClass;

    public SimpleIndexedPropertyDescriptor(IndexedPropertyDescriptor original) throws IntrospectionException {
      this(original.getName(), original.getReadMethod(), original.getWriteMethod(),
              original.getIndexedReadMethod(), original.getIndexedWriteMethod());
      PropertyDescriptorUtils.copyNonMethodProperties(original, this);
    }

    public SimpleIndexedPropertyDescriptor(
            String propertyName, @Nullable Method readMethod,
            @Nullable Method writeMethod, @Nullable Method indexedReadMethod, @Nullable Method indexedWriteMethod) throws IntrospectionException {

      super(propertyName, null, null, null, null);
      this.readMethod = readMethod;
      this.writeMethod = writeMethod;
      this.propertyType = PropertyDescriptorUtils.findPropertyType(readMethod, writeMethod);
      this.indexedReadMethod = indexedReadMethod;
      this.indexedWriteMethod = indexedWriteMethod;
      this.indexedPropertyType = PropertyDescriptorUtils.findIndexedPropertyType(
              propertyName, this.propertyType, indexedReadMethod, indexedWriteMethod);
    }

    @Override
    @Nullable
    public Method getReadMethod() {
      return this.readMethod;
    }

    @Override
    public void setReadMethod(@Nullable Method readMethod) {
      this.readMethod = readMethod;
    }

    @Override
    @Nullable
    public Method getWriteMethod() {
      return this.writeMethod;
    }

    @Override
    public void setWriteMethod(@Nullable Method writeMethod) {
      this.writeMethod = writeMethod;
    }

    @Override
    @Nullable
    public Class<?> getPropertyType() {
      if (this.propertyType == null) {
        try {
          this.propertyType = PropertyDescriptorUtils.findPropertyType(this.readMethod, this.writeMethod);
        }
        catch (IntrospectionException ex) {
          // Ignore, as does IndexedPropertyDescriptor#getPropertyType
        }
      }
      return this.propertyType;
    }

    @Override
    @Nullable
    public Method getIndexedReadMethod() {
      return this.indexedReadMethod;
    }

    @Override
    public void setIndexedReadMethod(@Nullable Method indexedReadMethod) throws IntrospectionException {
      this.indexedReadMethod = indexedReadMethod;
    }

    @Override
    @Nullable
    public Method getIndexedWriteMethod() {
      return this.indexedWriteMethod;
    }

    @Override
    public void setIndexedWriteMethod(@Nullable Method indexedWriteMethod) {
      this.indexedWriteMethod = indexedWriteMethod;
    }

    @Override
    @Nullable
    public Class<?> getIndexedPropertyType() {
      if (this.indexedPropertyType == null) {
        try {
          this.indexedPropertyType = PropertyDescriptorUtils.findIndexedPropertyType(
                  getName(), getPropertyType(), this.indexedReadMethod, this.indexedWriteMethod);
        }
        catch (IntrospectionException ex) {
          // Ignore, as does IndexedPropertyDescriptor#getIndexedPropertyType
        }
      }
      return this.indexedPropertyType;
    }

    @Override
    @Nullable
    public Class<?> getPropertyEditorClass() {
      return this.propertyEditorClass;
    }

    @Override
    public void setPropertyEditorClass(@Nullable Class<?> propertyEditorClass) {
      this.propertyEditorClass = propertyEditorClass;
    }

    /*
     * See java.beans.IndexedPropertyDescriptor#equals
     */
    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof IndexedPropertyDescriptor otherPd)) {
        return false;
      }
      return Objects.equals(getIndexedReadMethod(), otherPd.getIndexedReadMethod())
              && Objects.equals(getIndexedWriteMethod(), otherPd.getIndexedWriteMethod())
              && Objects.equals(getIndexedPropertyType(), otherPd.getIndexedPropertyType())
              && PropertyDescriptorUtils.equals(this, otherPd);
    }

    @Override
    public int hashCode() {
      int hashCode = Objects.hashCode(getReadMethod());
      hashCode = 29 * hashCode + Objects.hashCode(getWriteMethod());
      hashCode = 29 * hashCode + Objects.hashCode(getIndexedReadMethod());
      hashCode = 29 * hashCode + Objects.hashCode(getIndexedWriteMethod());
      return hashCode;
    }

    @Override
    public String toString() {
      return String.format("%s[name=%s, propertyType=%s, indexedPropertyType=%s, " +
                      "readMethod=%s, writeMethod=%s, indexedReadMethod=%s, indexedWriteMethod=%s]",
              getClass().getSimpleName(), getName(), getPropertyType(), getIndexedPropertyType(),
              this.readMethod, this.writeMethod, this.indexedReadMethod, this.indexedWriteMethod);
    }
  }

  /**
   * Sorts PropertyDescriptor instances alpha-numerically to emulate the behavior of
   * {@link java.beans.BeanInfo#getPropertyDescriptors()}.
   *
   * @see ExtendedBeanInfo#propertyDescriptors
   */
  static class PropertyDescriptorComparator implements Comparator<PropertyDescriptor> {

    @Override
    public int compare(PropertyDescriptor desc1, PropertyDescriptor desc2) {
      String left = desc1.getName();
      String right = desc2.getName();
      byte[] leftBytes = left.getBytes();
      byte[] rightBytes = right.getBytes();
      for (int i = 0; i < left.length(); i++) {
        if (right.length() == i) {
          return 1;
        }
        int result = leftBytes[i] - rightBytes[i];
        if (result != 0) {
          return result;
        }
      }
      return left.length() - right.length();
    }
  }

}
