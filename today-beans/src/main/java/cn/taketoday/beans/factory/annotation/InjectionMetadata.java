/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.factory.annotation;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * Internal class for managing injection metadata.
 * Not intended for direct use in applications.
 *
 * <p>Used by {@link AutowiredAnnotationBeanPostProcessor}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Juergen Hoeller
 * @since 4.0
 */
public class InjectionMetadata {

  /**
   * An empty {@code InjectionMetadata} instance with no-op callbacks.
   */
  public static final InjectionMetadata EMPTY = new InjectionMetadata(Object.class, Collections.emptyList()) {
    @Override
    protected boolean needsRefresh(Class<?> clazz) {
      return false;
    }

    @Override
    public void checkConfigMembers(RootBeanDefinition beanDefinition) { }

    @Override
    public void inject(Object target, @Nullable String beanName, @Nullable PropertyValues pvs) { }

    @Override
    public void clear(@Nullable PropertyValues pvs) { }
  };

  private final Class<?> targetClass;

  private final Collection<InjectedElement> injectedElements;

  @Nullable
  private volatile Set<InjectedElement> checkedElements;

  /**
   * Create a new {@code InjectionMetadata instance}.
   * <p>Preferably use {@link #forElements} for reusing the {@link #EMPTY}
   * instance in case of no elements.
   *
   * @param targetClass the target class
   * @param elements the associated elements to inject
   * @see #forElements
   */
  public InjectionMetadata(Class<?> targetClass, Collection<InjectedElement> elements) {
    this.targetClass = targetClass;
    this.injectedElements = elements;
  }

  /**
   * Return the {@link InjectedElement elements} to inject.
   *
   * @return the elements to inject
   */
  public Collection<InjectedElement> getInjectedElements() {
    return Collections.unmodifiableCollection(this.injectedElements);
  }

  /**
   * Return the {@link InjectedElement elements} to inject based on the
   * specified {@link PropertyValues}. If a property is already defined
   * for an {@link InjectedElement}, it is excluded.
   *
   * @param pvs the property values to consider
   * @return the elements to inject
   */
  public Collection<InjectedElement> getInjectedElements(@Nullable PropertyValues pvs) {
    return injectedElements.stream().filter(candidate -> candidate.shouldInject(pvs)).toList();
  }

  /**
   * Determine whether this metadata instance needs to be refreshed.
   *
   * @param clazz the current target class
   * @return {@code true} indicating a refresh, {@code false} otherwise
   */
  protected boolean needsRefresh(Class<?> clazz) {
    return targetClass != clazz;
  }

  public void checkConfigMembers(RootBeanDefinition beanDefinition) {
    if (this.injectedElements.isEmpty()) {
      this.checkedElements = Collections.emptySet();
    }
    else {
      var checkedElements = new LinkedHashSet<InjectedElement>((this.injectedElements.size() * 4 / 3) + 1);
      for (InjectedElement element : this.injectedElements) {
        Member member = element.getMember();
        if (!beanDefinition.isExternallyManagedConfigMember(member)) {
          beanDefinition.registerExternallyManagedConfigMember(member);
          checkedElements.add(element);
        }
      }
      this.checkedElements = checkedElements;
    }
  }

  public void inject(Object target, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
    Collection<InjectedElement> checkedElements = this.checkedElements;
    Collection<InjectedElement> elementsToIterate =
            checkedElements != null ? checkedElements : this.injectedElements;
    if (!elementsToIterate.isEmpty()) {
      for (InjectedElement element : elementsToIterate) {
        element.inject(target, beanName, pvs);
      }
    }
  }

  /**
   * Clear property skipping for the contained elements.
   */
  public void clear(@Nullable PropertyValues pvs) {
    Collection<InjectedElement> checkedElements = this.checkedElements;
    Collection<InjectedElement> elementsToIterate =
            (checkedElements != null ? checkedElements : this.injectedElements);
    if (!elementsToIterate.isEmpty()) {
      for (InjectedElement element : elementsToIterate) {
        element.clearPropertySkipping(pvs);
      }
    }
  }

  /**
   * Return an {@code InjectionMetadata} instance, possibly for empty elements.
   *
   * @param elements the elements to inject (possibly empty)
   * @param clazz the target class
   * @return a new {@link #InjectionMetadata(Class, Collection)} instance
   */
  public static InjectionMetadata forElements(Collection<InjectedElement> elements, Class<?> clazz) {
    return (elements.isEmpty() ? new InjectionMetadata(clazz, Collections.emptyList()) :
            new InjectionMetadata(clazz, elements));
  }

  /**
   * Check whether the given injection metadata needs to be refreshed.
   *
   * @param metadata the existing metadata instance
   * @param clazz the current target class
   * @return {@code true} indicating a refresh, {@code false} otherwise
   * @see #needsRefresh(Class)
   */
  public static boolean needsRefresh(@Nullable InjectionMetadata metadata, Class<?> clazz) {
    return (metadata == null || metadata.needsRefresh(clazz));
  }

  /**
   * A single injected element.
   */
  public abstract static class InjectedElement {
    protected final Member member;

    protected final boolean isField;

    @Nullable
    protected final PropertyDescriptor pd;

    @Nullable
    protected volatile Boolean skip;

    protected InjectedElement(Member member, @Nullable PropertyDescriptor pd) {
      this.member = member;
      this.isField = (member instanceof Field);
      this.pd = pd;
    }

    public final Member getMember() {
      return this.member;
    }

    protected final Class<?> getResourceType() {
      if (this.isField) {
        return ((Field) this.member).getType();
      }
      else if (this.pd != null) {
        return this.pd.getPropertyType();
      }
      else {
        return ((Method) this.member).getParameterTypes()[0];
      }
    }

    protected final void checkResourceType(Class<?> resourceType) {
      if (this.isField) {
        Class<?> fieldType = ((Field) this.member).getType();
        if (!(resourceType.isAssignableFrom(fieldType) || fieldType.isAssignableFrom(resourceType))) {
          throw new IllegalStateException("Specified field type [" + fieldType +
                  "] is incompatible with resource type [" + resourceType.getName() + "]");
        }
      }
      else {
        Class<?> paramType =
                (this.pd != null ? this.pd.getPropertyType() : ((Method) this.member).getParameterTypes()[0]);
        if (!(resourceType.isAssignableFrom(paramType) || paramType.isAssignableFrom(resourceType))) {
          throw new IllegalStateException("Specified parameter type [" + paramType +
                  "] is incompatible with resource type [" + resourceType.getName() + "]");
        }
      }
    }

    /**
     * Whether the property values should be injected.
     *
     * @param pvs property values to check
     * @return whether the property values should be injected
     */
    protected boolean shouldInject(@Nullable PropertyValues pvs) {
      if (this.isField) {
        return true;
      }
      return !checkPropertySkipping(pvs);
    }

    /**
     * Either this or {@link #getResourceToInject} needs to be overridden.
     */
    protected void inject(Object target, @Nullable String requestingBeanName, @Nullable PropertyValues pvs)
            throws Throwable //
    {
      if (shouldInject(pvs)) {
        if (this.isField) {
          Field field = (Field) this.member;
          ReflectionUtils.makeAccessible(field);
          field.set(target, getResourceToInject(target, requestingBeanName));
        }
        else {
          try {
            Method method = (Method) this.member;
            ReflectionUtils.makeAccessible(method);
            method.invoke(target, getResourceToInject(target, requestingBeanName));
          }
          catch (InvocationTargetException ex) {
            throw ex.getTargetException();
          }
        }
      }
    }

    /**
     * Check whether this injector's property needs to be skipped due to
     * an explicit property value having been specified. Also marks the
     * affected property as processed for other processors to ignore it.
     */
    protected boolean checkPropertySkipping(@Nullable PropertyValues pvs) {
      Boolean skip = this.skip;
      if (skip != null) {
        return skip;
      }
      if (pvs == null) {
        this.skip = false;
        return false;
      }
      synchronized(pvs) {
        skip = this.skip;
        if (skip != null) {
          return skip;
        }
        if (this.pd != null) {
          if (pvs.contains(this.pd.getName())) {
            // Explicit value provided as part of the bean definition.
            this.skip = true;
            return true;
          }
          else {
            pvs.registerProcessedProperty(this.pd.getName());
          }
        }
        this.skip = false;
        return false;
      }
    }

    /**
     * Clear property skipping for this element.
     */
    protected void clearPropertySkipping(@Nullable PropertyValues pvs) {
      if (pvs == null) {
        return;
      }
      synchronized(pvs) {
        if (Boolean.FALSE.equals(this.skip) && this.pd != null) {
          pvs.clearProcessedProperty(this.pd.getName());
        }
      }
    }

    /**
     * Either this or {@link #inject} needs to be overridden.
     */
    @Nullable
    protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
      return null;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof InjectedElement otherElement)) {
        return false;
      }
      return this.member.equals(otherElement.member);
    }

    @Override
    public int hashCode() {
      return this.member.getClass().hashCode() * 29 + this.member.getName().hashCode();
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + " for " + this.member;
    }
  }

}
