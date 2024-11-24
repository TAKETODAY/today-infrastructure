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

package infra.validation;

import java.beans.PropertyEditor;

import infra.beans.ConfigurablePropertyAccessor;
import infra.beans.PropertyAccessor;
import infra.beans.PropertyAccessorUtils;
import infra.beans.PropertyEditorRegistry;
import infra.beans.BeanUtils;
import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionService;
import infra.core.conversion.support.ConvertingPropertyEditorAdapter;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Abstract base class for {@link BindingResult} implementations that work with
 * Framework's {@link PropertyAccessor} mechanism.
 * Pre-implements field access through delegation to the corresponding
 * PropertyAccessor methods.
 *
 * @author Juergen Hoeller
 * @see #getPropertyAccessor()
 * @see PropertyAccessor
 * @see ConfigurablePropertyAccessor
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class AbstractPropertyBindingResult extends AbstractBindingResult {

  @Nullable
  private transient ConversionService conversionService;

  /**
   * Create a new AbstractPropertyBindingResult instance.
   *
   * @param objectName the name of the target object
   * @see DefaultMessageCodesResolver
   */
  protected AbstractPropertyBindingResult(String objectName) {
    super(objectName);
  }

  public void initConversion(ConversionService conversionService) {
    Assert.notNull(conversionService, "ConversionService is required");
    this.conversionService = conversionService;
    if (getTarget() != null) {
      getPropertyAccessor().setConversionService(conversionService);
    }
  }

  /**
   * Returns the underlying PropertyAccessor.
   *
   * @see #getPropertyAccessor()
   */
  @Override
  public PropertyEditorRegistry getPropertyEditorRegistry() {
    return (getTarget() != null ? getPropertyAccessor() : null);
  }

  /**
   * Returns the canonical property name.
   *
   * @see PropertyAccessorUtils#canonicalPropertyName
   */
  @Override
  protected String canonicalFieldName(String field) {
    return PropertyAccessorUtils.canonicalPropertyName(field);
  }

  /**
   * Determines the field type from the property type.
   *
   * @see #getPropertyAccessor()
   */
  @Override
  @Nullable
  public Class<?> getFieldType(@Nullable String field) {
    return getTarget() != null
           ? getPropertyAccessor().getPropertyType(fixedField(field))
           : super.getFieldType(field);
  }

  /**
   * Fetches the field value from the PropertyAccessor.
   *
   * @see #getPropertyAccessor()
   */
  @Override
  @Nullable
  protected Object getActualFieldValue(String field) {
    return getPropertyAccessor().getPropertyValue(field);
  }

  /**
   * Formats the field value based on registered PropertyEditors.
   *
   * @see #getCustomEditor
   */
  @Override
  protected Object formatFieldValue(String field, @Nullable Object value) {
    String fixedField = fixedField(field);
    // Try custom editor...
    PropertyEditor customEditor = getCustomEditor(fixedField);
    if (customEditor != null) {
      customEditor.setValue(value);
      String textValue = customEditor.getAsText();
      // If the PropertyEditor returned null, there is no appropriate
      // text representation for this value: only use it if non-null.
      if (textValue != null) {
        return textValue;
      }
    }
    if (this.conversionService != null) {
      // Try custom converter...
      TypeDescriptor fieldDesc = getPropertyAccessor().getPropertyTypeDescriptor(fixedField);
      TypeDescriptor strDesc = TypeDescriptor.valueOf(String.class);
      if (fieldDesc != null && this.conversionService.canConvert(fieldDesc, strDesc)) {
        return this.conversionService.convert(value, fieldDesc, strDesc);
      }
    }
    return value;
  }

  /**
   * Retrieve the custom PropertyEditor for the given field, if any.
   *
   * @param fixedField the fully qualified field name
   * @return the custom PropertyEditor, or {@code null}
   */
  @Nullable
  protected PropertyEditor getCustomEditor(String fixedField) {
    ConfigurablePropertyAccessor propertyAccessor = getPropertyAccessor();
    Class<?> targetType = propertyAccessor.getPropertyType(fixedField);
    PropertyEditor editor = propertyAccessor.findCustomEditor(targetType, fixedField);
    if (editor == null) {
      editor = BeanUtils.findEditorByConvention(targetType);
    }
    return editor;
  }

  /**
   * This implementation exposes a PropertyEditor adapter for a Formatter,
   * if applicable.
   */
  @Override
  @Nullable
  public PropertyEditor findEditor(@Nullable String field, @Nullable Class<?> valueType) {
    Class<?> valueTypeForLookup = valueType;
    if (valueTypeForLookup == null) {
      valueTypeForLookup = getFieldType(field);
    }
    PropertyEditor editor = super.findEditor(field, valueTypeForLookup);
    if (editor == null && this.conversionService != null) {
      TypeDescriptor td = null;
      if (field != null && getTarget() != null) {
        TypeDescriptor ptd = getPropertyAccessor().getPropertyTypeDescriptor(fixedField(field));
        if (ptd != null && (valueType == null || valueType.isAssignableFrom(ptd.getType()))) {
          td = ptd;
        }
      }
      if (td == null) {
        td = TypeDescriptor.valueOf(valueTypeForLookup);
      }
      if (this.conversionService.canConvert(TypeDescriptor.valueOf(String.class), td)) {
        editor = new ConvertingPropertyEditorAdapter(this.conversionService, td);
      }
    }
    return editor;
  }

  /**
   * Provide the PropertyAccessor to work with, according to the
   * concrete strategy of access.
   * <p>Note that a PropertyAccessor used by a BindingResult should
   * always have its "extractOldValueForEditor" flag set to "true"
   * by default, since this is typically possible without side effects
   * for model objects that serve as data binding target.
   *
   * @see ConfigurablePropertyAccessor#setExtractOldValueForEditor
   */
  public abstract ConfigurablePropertyAccessor getPropertyAccessor();

}
