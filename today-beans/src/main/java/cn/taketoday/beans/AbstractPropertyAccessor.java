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

package cn.taketoday.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * Abstract implementation of the {@link PropertyAccessor} interface.
 * Provides base implementations of all convenience methods, with the
 * implementation of actual property access left to subclasses.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getPropertyValue
 * @see #setPropertyValue
 * @since 4.0 2022/2/17 17:42
 */
public abstract class AbstractPropertyAccessor extends TypeConverterSupport implements ConfigurablePropertyAccessor {

  private boolean extractOldValueForEditor = false;

  private boolean autoGrowNestedPaths = false;

  boolean suppressNotWritablePropertyException = false;

  @Override
  public void setExtractOldValueForEditor(boolean extractOldValueForEditor) {
    this.extractOldValueForEditor = extractOldValueForEditor;
  }

  @Override
  public boolean isExtractOldValueForEditor() {
    return this.extractOldValueForEditor;
  }

  @Override
  public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
    this.autoGrowNestedPaths = autoGrowNestedPaths;
  }

  @Override
  public boolean isAutoGrowNestedPaths() {
    return this.autoGrowNestedPaths;
  }

  @Override
  public void setPropertyValue(PropertyValue pv) throws BeansException {
    setPropertyValue(pv.getName(), pv.getValue());
  }

  @Override
  public void setPropertyValues(Map<?, ?> map) throws BeansException {
    setPropertyValues(map, false, false);
  }

  @Override
  public void setPropertyValues(Map<?, ?> map, boolean ignoreUnknown) throws BeansException {
    setPropertyValues(map, ignoreUnknown, false);
  }

  @Override
  public void setPropertyValues(PropertyValues pvs) throws BeansException {
    setPropertyValues(pvs, false, false);
  }

  @Override
  public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown) throws BeansException {
    setPropertyValues(pvs, ignoreUnknown, false);
  }

  @Override
  public void setPropertyValues(Map<?, ?> map, boolean ignoreUnknown, boolean ignoreInvalid)
          throws BeansException {
    if (CollectionUtils.isEmpty(map)) {
      return;
    }

    ArrayList<PropertyAccessException> propertyAccessExceptions = null;
    if (ignoreUnknown) {
      this.suppressNotWritablePropertyException = true;
    }

    try {
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        // setPropertyValue may throw any BeansException, which won't be caught
        // here, if there is a critical failure such as no matching field.
        // We can attempt to deal only with less serious exceptions.
        try {
          setPropertyValue(new PropertyValue(String.valueOf(entry.getKey()), entry.getValue()));
        }
        catch (NotWritablePropertyException ex) {
          if (!ignoreUnknown) {
            throw ex;
          }
          // Otherwise, just ignore it and continue...
        }
        catch (NullValueInNestedPathException ex) {
          if (!ignoreInvalid) {
            throw ex;
          }
          // Otherwise, just ignore it and continue...
        }
        catch (PropertyAccessException ex) {
          if (propertyAccessExceptions == null) {
            propertyAccessExceptions = new ArrayList<>();
          }
          propertyAccessExceptions.add(ex);
        }
      }
    }
    finally {
      if (ignoreUnknown) {
        this.suppressNotWritablePropertyException = false;
      }
    }

    // If we encountered individual exceptions, throw the composite exception.
    if (propertyAccessExceptions != null) {
      var paeArray = propertyAccessExceptions.toArray(new PropertyAccessException[0]);
      throw new PropertyBatchUpdateException(paeArray);
    }
  }

  @Override
  public void setPropertyValues(
          PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid) throws BeansException {
    List<PropertyAccessException> propertyAccessExceptions = null;
    if (ignoreUnknown) {
      this.suppressNotWritablePropertyException = true;
    }
    try {
      for (PropertyValue pv : pvs) {
        // setPropertyValue may throw any BeansException, which won't be caught
        // here, if there is a critical failure such as no matching field.
        // We can attempt to deal ony with less serious exceptions.
        try {
          setPropertyValue(pv);
        }
        catch (NotWritablePropertyException ex) {
          if (!ignoreUnknown) {
            throw ex;
          }
          // Otherwise, just ignore it and continue...
        }
        catch (NullValueInNestedPathException ex) {
          if (!ignoreInvalid) {
            throw ex;
          }
          // Otherwise, just ignore it and continue...
        }
        catch (PropertyAccessException ex) {
          if (propertyAccessExceptions == null) {
            propertyAccessExceptions = new ArrayList<>();
          }
          propertyAccessExceptions.add(ex);
        }
      }
    }
    finally {
      if (ignoreUnknown) {
        this.suppressNotWritablePropertyException = false;
      }
    }

    // If we encountered individual exceptions, throw the composite exception.
    if (propertyAccessExceptions != null) {
      var paeArray = propertyAccessExceptions.toArray(new PropertyAccessException[0]);
      throw new PropertyBatchUpdateException(paeArray);
    }
  }

  // Redefined with public visibility.
  @Override
  @Nullable
  public Class<?> getPropertyType(String propertyPath) {
    return null;
  }

  /**
   * Actually get the value of a property.
   *
   * @param propertyName name of the property to get the value of
   * @return the value of the property
   * @throws InvalidPropertyException if there is no such property or
   * if the property isn't readable
   * @throws PropertyAccessException if the property was valid but the
   * accessor method failed
   */
  @Override
  @Nullable
  public abstract Object getPropertyValue(String propertyName) throws BeansException;

  /**
   * Actually set a property value.
   *
   * @param propertyName name of the property to set value of
   * @param value the new value
   * @throws InvalidPropertyException if there is no such property or
   * if the property isn't writable
   * @throws PropertyAccessException if the property was valid but the
   * accessor method failed or a type mismatch occurred
   */
  @Override
  public abstract void setPropertyValue(String propertyName, @Nullable Object value) throws BeansException;

}

