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

package cn.taketoday.beans.factory;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.FatalBeanException;
import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.lang.Nullable;

/**
 * Exception thrown when a BeanFactory encounters an error when
 * attempting to create a bean from a bean definition.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/11/4 23:21
 */
public class BeanCreationException extends FatalBeanException {

  @Nullable
  private final String beanName;

  @Nullable
  private final String resourceDescription;

  @Nullable
  private List<Throwable> relatedCauses;

  /**
   * Create a new BeanCreationException.
   *
   * @param msg the detail message
   */
  public BeanCreationException(String msg) {
    super(msg);
    this.beanName = null;
    this.resourceDescription = null;
  }

  /**
   * Create a new BeanCreationException.
   *
   * @param msg the detail message
   * @param cause the root cause
   */
  public BeanCreationException(String msg, Throwable cause) {
    super(msg, cause);
    this.beanName = null;
    this.resourceDescription = null;
  }

  /**
   * Create a new BeanCreationException.
   *
   * @param beanName the name of the bean requested
   * @param msg the detail message
   */
  public BeanCreationException(String beanName, String msg) {
    super("Error creating bean with name '" + beanName + "': " + msg);
    this.beanName = beanName;
    this.resourceDescription = null;
  }

  /**
   * Create a new BeanCreationException.
   *
   * @param beanName the name of the bean requested
   * @param msg the detail message
   * @param cause the root cause
   */
  public BeanCreationException(String beanName, String msg, Throwable cause) {
    this(beanName, msg);
    initCause(cause);
  }

  /**
   * Create a new BeanCreationException.
   *
   * @param resourceDescription description of the resource
   * that the bean definition came from
   * @param beanName the name of the bean requested
   * @param msg the detail message
   */
  public BeanCreationException(@Nullable String resourceDescription, @Nullable String beanName, String msg) {
    super("Error creating bean with name '" + beanName + "'" +
            (resourceDescription != null ? " defined in " + resourceDescription : "") + ": " + msg);
    this.resourceDescription = resourceDescription;
    this.beanName = beanName;
    this.relatedCauses = null;
  }

  /**
   * Create a new BeanCreationException.
   *
   * @param resourceDescription description of the resource
   * that the bean definition came from
   * @param beanName the name of the bean requested
   * @param msg the detail message
   * @param cause the root cause
   */
  public BeanCreationException(
          @Nullable String resourceDescription, String beanName, String msg, Throwable cause) {
    this(resourceDescription, beanName, msg);
    initCause(cause);
  }

  /**
   * Return the description of the resource that the bean
   * definition came from, if any.
   */
  @Nullable
  public String getResourceDescription() {
    return this.resourceDescription;
  }

  /**
   * Return the name of the bean requested, if any.
   */
  @Nullable
  public String getBeanName() {
    return this.beanName;
  }

  /**
   * Add a related cause to this bean creation exception,
   * not being a direct cause of the failure but having occurred
   * earlier in the creation of the same bean instance.
   *
   * @param ex the related cause to add
   */
  public void addRelatedCause(Throwable ex) {
    if (this.relatedCauses == null) {
      this.relatedCauses = new ArrayList<>();
    }
    this.relatedCauses.add(ex);
  }

  /**
   * Return the related causes, if any.
   *
   * @return the array of related causes, or {@code null} if none
   */
  @Nullable
  public Throwable[] getRelatedCauses() {
    if (this.relatedCauses == null) {
      return null;
    }
    return this.relatedCauses.toArray(new Throwable[0]);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    if (this.relatedCauses != null) {
      for (Throwable relatedCause : this.relatedCauses) {
        sb.append("\nRelated cause: ");
        sb.append(relatedCause);
      }
    }
    return sb.toString();
  }

  @Override
  public void printStackTrace(PrintStream ps) {
    synchronized(ps) {
      super.printStackTrace(ps);
      if (this.relatedCauses != null) {
        for (Throwable relatedCause : this.relatedCauses) {
          ps.println("Related cause:");
          relatedCause.printStackTrace(ps);
        }
      }
    }
  }

  @Override
  public void printStackTrace(PrintWriter pw) {
    synchronized(pw) {
      super.printStackTrace(pw);
      if (this.relatedCauses != null) {
        for (Throwable relatedCause : this.relatedCauses) {
          pw.println("Related cause:");
          relatedCause.printStackTrace(pw);
        }
      }
    }
  }

  @Override
  public boolean contains(@Nullable Class<?> exClass) {
    if (super.contains(exClass)) {
      return true;
    }
    if (this.relatedCauses != null) {
      for (Throwable relatedCause : this.relatedCauses) {
        if (relatedCause instanceof NestedRuntimeException &&
                ((NestedRuntimeException) relatedCause).contains(exClass)) {
          return true;
        }
      }
    }
    return false;
  }

}
