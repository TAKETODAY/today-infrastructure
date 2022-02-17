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

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 18:04
 */

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link ConfigurablePropertyAccessor} implementation that directly accesses
 * instance fields. Allows for direct binding to fields instead of going through
 * JavaBean setters.
 *
 * <p>As of Spring 4.2, the vast majority of the {@link BeanWrapper} features have
 * been merged to {@link AbstractPropertyAccessor}, which means that property
 * traversal as well as collections and map access is now supported here as well.
 *
 * <p>A DirectFieldAccessor's default for the "extractOldValueForEditor" setting
 * is "true", since a field can always be read without side effects.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see #setExtractOldValueForEditor
 * @see BeanWrapper
 * @see cn.taketoday.validation.DirectFieldBindingResult
 * @see cn.taketoday.validation.DataBinder#initDirectFieldAccess()
 * @since 2.0
 */
public class DirectFieldAccessor extends AbstractNestablePropertyAccessor {

  private final Map<String, FieldPropertyHandler> fieldMap = new HashMap<>();

  /**
   * Create a new DirectFieldAccessor for the given object.
   *
   * @param object the object wrapped by this DirectFieldAccessor
   */
  public DirectFieldAccessor(Object object) {
    super(object);
  }

  /**
   * Create a new DirectFieldAccessor for the given object,
   * registering a nested path that the object is in.
   *
   * @param object the object wrapped by this DirectFieldAccessor
   * @param nestedPath the nested path of the object
   * @param parent the containing DirectFieldAccessor (must not be {@code null})
   */
  protected DirectFieldAccessor(Object object, String nestedPath, DirectFieldAccessor parent) {
    super(object, nestedPath, parent);
  }

  @Override
  @Nullable
  protected FieldPropertyHandler getLocalPropertyHandler(String propertyName) {
    FieldPropertyHandler propertyHandler = this.fieldMap.get(propertyName);
    if (propertyHandler == null) {
      Field field = ReflectionUtils.findField(getWrappedClass(), propertyName);
      if (field != null) {
        propertyHandler = new FieldPropertyHandler(field);
        this.fieldMap.put(propertyName, propertyHandler);
      }
    }
    return propertyHandler;
  }

  @Override
  protected DirectFieldAccessor newNestedPropertyAccessor(Object object, String nestedPath) {
    return new DirectFieldAccessor(object, nestedPath, this);
  }

  @Override
  protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
    PropertyMatches matches = PropertyMatches.forField(propertyName, getRootClass());
    throw new NotWritablePropertyException(getRootClass(), getNestedPath() + propertyName,
            matches.buildErrorMessage(), matches.getPossibleMatches());
  }

  private class FieldPropertyHandler extends PropertyHandler {

    private final Field field;

    public FieldPropertyHandler(Field field) {
      super(field.getType(), true, true);
      this.field = field;
    }

    @Override
    public TypeDescriptor toTypeDescriptor() {
      return new TypeDescriptor(this.field);
    }

    @Override
    public ResolvableType getResolvableType() {
      return ResolvableType.fromField(this.field);
    }

    @Override
    @Nullable
    public TypeDescriptor nested(int level) {
      return TypeDescriptor.nested(this.field, level);
    }

    @Override
    @Nullable
    public Object getValue() throws Exception {
      try {
        ReflectionUtils.makeAccessible(this.field);
        return this.field.get(getWrappedInstance());
      }

      catch (IllegalAccessException ex) {
        throw new InvalidPropertyException(getWrappedClass(),
                this.field.getName(), "Field is not accessible", ex);
      }
    }

    @Override
    public void setValue(@Nullable Object value) throws Exception {
      try {
        ReflectionUtils.makeAccessible(this.field);
        this.field.set(getWrappedInstance(), value);
      }
      catch (IllegalAccessException ex) {
        throw new InvalidPropertyException(getWrappedClass(), this.field.getName(),
                "Field is not accessible", ex);
      }
    }
  }

}
