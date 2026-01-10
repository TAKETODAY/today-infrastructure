/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import infra.core.ResolvableType;
import infra.core.TypeDescriptor;
import infra.util.ReflectionUtils;

/**
 * {@link ConfigurablePropertyAccessor} implementation that directly accesses
 * instance fields. Allows for direct binding to fields instead of going through
 * JavaBean setters.
 *
 * <p>the vast majority of the {@link BeanWrapper} features have been merged to
 * {@link AbstractPropertyAccessor}, which means that property traversal as well
 * as collections and map access is now supported here as well.
 *
 * <p>A DirectFieldAccessor's default for the "extractOldValueForEditor" setting
 * is "true", since a field can always be read without side effects.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setExtractOldValueForEditor
 * @see BeanWrapper
 * @see infra.validation.DirectFieldBindingResult
 * @see infra.validation.DataBinder#initDirectFieldAccess()
 * @since 4.0 2022/2/17 18:04
 */
public class DirectFieldAccessor extends AbstractNestablePropertyAccessor {

  private final HashMap<String, FieldPropertyHandler> fieldMap = new HashMap<>();

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

  private final class FieldPropertyHandler extends PropertyHandler {

    private final Field field;

    @Nullable
    private TypeDescriptor typeDescriptor;

    @Nullable
    private ResolvableType resolvableType;

    public FieldPropertyHandler(Field field) {
      super(field.getType(), true, !Modifier.isFinal(field.getModifiers()));
      this.field = field;
    }

    /**
     * Returns {@link TypeDescriptor} for this property
     */
    @Override
    public TypeDescriptor toTypeDescriptor() {
      TypeDescriptor typeDescriptor = this.typeDescriptor;
      if (typeDescriptor == null) {
        typeDescriptor = new TypeDescriptor(getResolvableType(), field.getType(), field);
        this.typeDescriptor = typeDescriptor;
      }
      return typeDescriptor;
    }

    @Override
    public ResolvableType getResolvableType() {
      ResolvableType resolvableType = this.resolvableType;
      if (resolvableType == null) {
        resolvableType = ResolvableType.forField(field);
        this.resolvableType = resolvableType;
      }
      return resolvableType;
    }

    @Override
    public TypeDescriptor getMapValueType(int nestingLevel) {
      return new TypeDescriptor(getResolvableType().getNested(nestingLevel).asMap().getGeneric(1),
              null, field);
    }

    @Override
    public TypeDescriptor getCollectionType(int nestingLevel) {
      return new TypeDescriptor(getResolvableType().getNested(nestingLevel).asCollection().getGeneric(),
              null, field);
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
      catch (IllegalAccessException | InaccessibleObjectException ex) {
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
      catch (IllegalAccessException | InaccessibleObjectException ex) {
        throw new InvalidPropertyException(getWrappedClass(), this.field.getName(),
                "Field is not accessible", ex);
      }
    }
  }

}
