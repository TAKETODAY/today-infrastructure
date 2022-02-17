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

import org.xml.sax.InputSource;

import java.beans.PropertyEditor;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import cn.taketoday.beans.propertyeditors.ByteArrayPropertyEditor;
import cn.taketoday.beans.propertyeditors.CharArrayPropertyEditor;
import cn.taketoday.beans.propertyeditors.CharacterEditor;
import cn.taketoday.beans.propertyeditors.CharsetEditor;
import cn.taketoday.beans.propertyeditors.ClassArrayEditor;
import cn.taketoday.beans.propertyeditors.ClassEditor;
import cn.taketoday.beans.propertyeditors.CurrencyEditor;
import cn.taketoday.beans.propertyeditors.CustomBooleanEditor;
import cn.taketoday.beans.propertyeditors.CustomCollectionEditor;
import cn.taketoday.beans.propertyeditors.CustomMapEditor;
import cn.taketoday.beans.propertyeditors.CustomNumberEditor;
import cn.taketoday.beans.propertyeditors.FileEditor;
import cn.taketoday.beans.propertyeditors.InputSourceEditor;
import cn.taketoday.beans.propertyeditors.InputStreamEditor;
import cn.taketoday.beans.propertyeditors.LocaleEditor;
import cn.taketoday.beans.propertyeditors.PathEditor;
import cn.taketoday.beans.propertyeditors.PatternEditor;
import cn.taketoday.beans.propertyeditors.PropertiesEditor;
import cn.taketoday.beans.propertyeditors.ReaderEditor;
import cn.taketoday.beans.propertyeditors.StringArrayPropertyEditor;
import cn.taketoday.beans.propertyeditors.TimeZoneEditor;
import cn.taketoday.beans.propertyeditors.URIEditor;
import cn.taketoday.beans.propertyeditors.URLEditor;
import cn.taketoday.beans.propertyeditors.UUIDEditor;
import cn.taketoday.beans.propertyeditors.ZoneIdEditor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceArrayPropertyEditor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Base implementation of the {@link PropertyEditorRegistry} interface.
 * Provides management of default editors and custom editors.
 * Mainly serves as base class for {@link BeanWrapperImpl}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.beans.PropertyEditorManager
 * @see java.beans.PropertyEditorSupport#setAsText
 * @see java.beans.PropertyEditorSupport#setValue
 * @since 4.0 2022/2/17 17:40
 */
public class PropertyEditorRegistrySupport implements PropertyEditorRegistry {

  @Nullable
  private ConversionService conversionService;

  private boolean defaultEditorsActive = false;

  private boolean configValueEditorsActive = false;

  @Nullable
  private Map<Class<?>, PropertyEditor> defaultEditors;

  @Nullable
  private Map<Class<?>, PropertyEditor> overriddenDefaultEditors;

  @Nullable
  private Map<Class<?>, PropertyEditor> customEditors;

  @Nullable
  private Map<String, CustomEditorHolder> customEditorsForPath;

  @Nullable
  private Map<Class<?>, PropertyEditor> customEditorCache;

  /**
   * Specify a Spring 3.0 ConversionService to use for converting
   * property values, as an alternative to JavaBeans PropertyEditors.
   */
  public void setConversionService(@Nullable ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  /**
   * Return the associated ConversionService, if any.
   */
  @Nullable
  public ConversionService getConversionService() {
    return this.conversionService;
  }

  //---------------------------------------------------------------------
  // Management of default editors
  //---------------------------------------------------------------------

  /**
   * Activate the default editors for this registry instance,
   * allowing for lazily registering default editors when needed.
   */
  protected void registerDefaultEditors() {
    this.defaultEditorsActive = true;
  }

  /**
   * Activate config value editors which are only intended for configuration purposes,
   * such as {@link cn.taketoday.beans.propertyeditors.StringArrayPropertyEditor}.
   * <p>Those editors are not registered by default simply because they are in
   * general inappropriate for data binding purposes. Of course, you may register
   * them individually in any case, through {@link #registerCustomEditor}.
   */
  public void useConfigValueEditors() {
    this.configValueEditorsActive = true;
  }

  /**
   * Override the default editor for the specified type with the given property editor.
   * <p>Note that this is different from registering a custom editor in that the editor
   * semantically still is a default editor. A ConversionService will override such a
   * default editor, whereas custom editors usually override the ConversionService.
   *
   * @param requiredType the type of the property
   * @param propertyEditor the editor to register
   * @see #registerCustomEditor(Class, PropertyEditor)
   */
  public void overrideDefaultEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
    if (this.overriddenDefaultEditors == null) {
      this.overriddenDefaultEditors = new HashMap<>();
    }
    this.overriddenDefaultEditors.put(requiredType, propertyEditor);
  }

  /**
   * Retrieve the default editor for the given property type, if any.
   * <p>Lazily registers the default editors, if they are active.
   *
   * @param requiredType type of the property
   * @return the default editor, or {@code null} if none found
   * @see #registerDefaultEditors
   */
  @Nullable
  public PropertyEditor getDefaultEditor(Class<?> requiredType) {
    if (!this.defaultEditorsActive) {
      return null;
    }
    if (this.overriddenDefaultEditors != null) {
      PropertyEditor editor = this.overriddenDefaultEditors.get(requiredType);
      if (editor != null) {
        return editor;
      }
    }
    if (this.defaultEditors == null) {
      createDefaultEditors();
    }
    return this.defaultEditors.get(requiredType);
  }

  /**
   * Actually register the default editors for this registry instance.
   */
  private void createDefaultEditors() {
    this.defaultEditors = new HashMap<>(64);

    // Simple editors, without parameterization capabilities.
    // The JDK does not contain a default editor for any of these target types.
    this.defaultEditors.put(Charset.class, new CharsetEditor());
    this.defaultEditors.put(Class.class, new ClassEditor());
    this.defaultEditors.put(Class[].class, new ClassArrayEditor());
    this.defaultEditors.put(Currency.class, new CurrencyEditor());
    this.defaultEditors.put(File.class, new FileEditor());
    this.defaultEditors.put(InputStream.class, new InputStreamEditor());
    this.defaultEditors.put(InputSource.class, new InputSourceEditor());
    this.defaultEditors.put(Locale.class, new LocaleEditor());
    this.defaultEditors.put(Path.class, new PathEditor());
    this.defaultEditors.put(Pattern.class, new PatternEditor());
    this.defaultEditors.put(Properties.class, new PropertiesEditor());
    this.defaultEditors.put(Reader.class, new ReaderEditor());
    this.defaultEditors.put(Resource[].class, new ResourceArrayPropertyEditor());
    this.defaultEditors.put(TimeZone.class, new TimeZoneEditor());
    this.defaultEditors.put(URI.class, new URIEditor());
    this.defaultEditors.put(URL.class, new URLEditor());
    this.defaultEditors.put(UUID.class, new UUIDEditor());
    this.defaultEditors.put(ZoneId.class, new ZoneIdEditor());

    // Default instances of collection editors.
    // Can be overridden by registering custom instances of those as custom editors.
    this.defaultEditors.put(Collection.class, new CustomCollectionEditor(Collection.class));
    this.defaultEditors.put(Set.class, new CustomCollectionEditor(Set.class));
    this.defaultEditors.put(SortedSet.class, new CustomCollectionEditor(SortedSet.class));
    this.defaultEditors.put(List.class, new CustomCollectionEditor(List.class));
    this.defaultEditors.put(SortedMap.class, new CustomMapEditor(SortedMap.class));

    // Default editors for primitive arrays.
    this.defaultEditors.put(byte[].class, new ByteArrayPropertyEditor());
    this.defaultEditors.put(char[].class, new CharArrayPropertyEditor());

    // The JDK does not contain a default editor for char!
    this.defaultEditors.put(char.class, new CharacterEditor(false));
    this.defaultEditors.put(Character.class, new CharacterEditor(true));

    // Spring's CustomBooleanEditor accepts more flag values than the JDK's default editor.
    this.defaultEditors.put(boolean.class, new CustomBooleanEditor(false));
    this.defaultEditors.put(Boolean.class, new CustomBooleanEditor(true));

    // The JDK does not contain default editors for number wrapper types!
    // Override JDK primitive number editors with our own CustomNumberEditor.
    this.defaultEditors.put(byte.class, new CustomNumberEditor(Byte.class, false));
    this.defaultEditors.put(Byte.class, new CustomNumberEditor(Byte.class, true));
    this.defaultEditors.put(short.class, new CustomNumberEditor(Short.class, false));
    this.defaultEditors.put(Short.class, new CustomNumberEditor(Short.class, true));
    this.defaultEditors.put(int.class, new CustomNumberEditor(Integer.class, false));
    this.defaultEditors.put(Integer.class, new CustomNumberEditor(Integer.class, true));
    this.defaultEditors.put(long.class, new CustomNumberEditor(Long.class, false));
    this.defaultEditors.put(Long.class, new CustomNumberEditor(Long.class, true));
    this.defaultEditors.put(float.class, new CustomNumberEditor(Float.class, false));
    this.defaultEditors.put(Float.class, new CustomNumberEditor(Float.class, true));
    this.defaultEditors.put(double.class, new CustomNumberEditor(Double.class, false));
    this.defaultEditors.put(Double.class, new CustomNumberEditor(Double.class, true));
    this.defaultEditors.put(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, true));
    this.defaultEditors.put(BigInteger.class, new CustomNumberEditor(BigInteger.class, true));

    // Only register config value editors if explicitly requested.
    if (this.configValueEditorsActive) {
      StringArrayPropertyEditor sae = new StringArrayPropertyEditor();
      this.defaultEditors.put(String[].class, sae);
      this.defaultEditors.put(short[].class, sae);
      this.defaultEditors.put(int[].class, sae);
      this.defaultEditors.put(long[].class, sae);
    }
  }

  /**
   * Copy the default editors registered in this instance to the given target registry.
   *
   * @param target the target registry to copy to
   */
  protected void copyDefaultEditorsTo(PropertyEditorRegistrySupport target) {
    target.defaultEditorsActive = this.defaultEditorsActive;
    target.configValueEditorsActive = this.configValueEditorsActive;
    target.defaultEditors = this.defaultEditors;
    target.overriddenDefaultEditors = this.overriddenDefaultEditors;
  }

  //---------------------------------------------------------------------
  // Management of custom editors
  //---------------------------------------------------------------------

  @Override
  public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
    registerCustomEditor(requiredType, null, propertyEditor);
  }

  @Override
  public void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath, PropertyEditor propertyEditor) {
    if (requiredType == null && propertyPath == null) {
      throw new IllegalArgumentException("Either requiredType or propertyPath is required");
    }
    if (propertyPath != null) {
      if (this.customEditorsForPath == null) {
        this.customEditorsForPath = new LinkedHashMap<>(16);
      }
      this.customEditorsForPath.put(propertyPath, new CustomEditorHolder(propertyEditor, requiredType));
    }
    else {
      if (this.customEditors == null) {
        this.customEditors = new LinkedHashMap<>(16);
      }
      this.customEditors.put(requiredType, propertyEditor);
      this.customEditorCache = null;
    }
  }

  @Override
  @Nullable
  public PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath) {
    Class<?> requiredTypeToUse = requiredType;
    if (propertyPath != null) {
      if (this.customEditorsForPath != null) {
        // Check property-specific editor first.
        PropertyEditor editor = getCustomEditor(propertyPath, requiredType);
        if (editor == null) {
          List<String> strippedPaths = new ArrayList<>();
          addStrippedPropertyPaths(strippedPaths, "", propertyPath);
          for (Iterator<String> it = strippedPaths.iterator(); it.hasNext() && editor == null; ) {
            String strippedPath = it.next();
            editor = getCustomEditor(strippedPath, requiredType);
          }
        }
        if (editor != null) {
          return editor;
        }
      }
      if (requiredType == null) {
        requiredTypeToUse = getPropertyType(propertyPath);
      }
    }
    // No property-specific editor -> check type-specific editor.
    return getCustomEditor(requiredTypeToUse);
  }

  /**
   * Determine whether this registry contains a custom editor
   * for the specified array/collection element.
   *
   * @param elementType the target type of the element
   * (can be {@code null} if not known)
   * @param propertyPath the property path (typically of the array/collection;
   * can be {@code null} if not known)
   * @return whether a matching custom editor has been found
   */
  public boolean hasCustomEditorForElement(@Nullable Class<?> elementType, @Nullable String propertyPath) {
    if (propertyPath != null && this.customEditorsForPath != null) {
      for (Map.Entry<String, CustomEditorHolder> entry : this.customEditorsForPath.entrySet()) {
        if (PropertyAccessorUtils.matchesProperty(entry.getKey(), propertyPath) &&
                entry.getValue().getPropertyEditor(elementType) != null) {
          return true;
        }
      }
    }
    // No property-specific editor -> check type-specific editor.
    return (elementType != null && this.customEditors != null && this.customEditors.containsKey(elementType));
  }

  /**
   * Determine the property type for the given property path.
   * <p>Called by {@link #findCustomEditor} if no required type has been specified,
   * to be able to find a type-specific editor even if just given a property path.
   * <p>The default implementation always returns {@code null}.
   * BeanWrapperImpl overrides this with the standard {@code getPropertyType}
   * method as defined by the BeanWrapper interface.
   *
   * @param propertyPath the property path to determine the type for
   * @return the type of the property, or {@code null} if not determinable
   * @see BeanWrapper#getPropertyType(String)
   */
  @Nullable
  protected Class<?> getPropertyType(String propertyPath) {
    return null;
  }

  /**
   * Get custom editor that has been registered for the given property.
   *
   * @param propertyName the property path to look for
   * @param requiredType the type to look for
   * @return the custom editor, or {@code null} if none specific for this property
   */
  @Nullable
  private PropertyEditor getCustomEditor(String propertyName, @Nullable Class<?> requiredType) {
    CustomEditorHolder holder =
            (this.customEditorsForPath != null ? this.customEditorsForPath.get(propertyName) : null);
    return (holder != null ? holder.getPropertyEditor(requiredType) : null);
  }

  /**
   * Get custom editor for the given type. If no direct match found,
   * try custom editor for superclass (which will in any case be able
   * to render a value as String via {@code getAsText}).
   *
   * @param requiredType the type to look for
   * @return the custom editor, or {@code null} if none found for this type
   * @see java.beans.PropertyEditor#getAsText()
   */
  @Nullable
  private PropertyEditor getCustomEditor(@Nullable Class<?> requiredType) {
    if (requiredType == null || this.customEditors == null) {
      return null;
    }
    // Check directly registered editor for type.
    PropertyEditor editor = this.customEditors.get(requiredType);
    if (editor == null) {
      // Check cached editor for type, registered for superclass or interface.
      if (this.customEditorCache != null) {
        editor = this.customEditorCache.get(requiredType);
      }
      if (editor == null) {
        // Find editor for superclass or interface.
        for (Map.Entry<Class<?>, PropertyEditor> entry : this.customEditors.entrySet()) {
          Class<?> key = entry.getKey();
          if (key.isAssignableFrom(requiredType)) {
            editor = entry.getValue();
            // Cache editor for search type, to avoid the overhead
            // of repeated assignable-from checks.
            if (this.customEditorCache == null) {
              this.customEditorCache = new HashMap<>();
            }
            this.customEditorCache.put(requiredType, editor);
            if (editor != null) {
              break;
            }
          }
        }
      }
    }
    return editor;
  }

  /**
   * Guess the property type of the specified property from the registered
   * custom editors (provided that they were registered for a specific type).
   *
   * @param propertyName the name of the property
   * @return the property type, or {@code null} if not determinable
   */
  @Nullable
  protected Class<?> guessPropertyTypeFromEditors(String propertyName) {
    if (this.customEditorsForPath != null) {
      CustomEditorHolder editorHolder = this.customEditorsForPath.get(propertyName);
      if (editorHolder == null) {
        List<String> strippedPaths = new ArrayList<>();
        addStrippedPropertyPaths(strippedPaths, "", propertyName);
        for (Iterator<String> it = strippedPaths.iterator(); it.hasNext() && editorHolder == null; ) {
          String strippedName = it.next();
          editorHolder = this.customEditorsForPath.get(strippedName);
        }
      }
      if (editorHolder != null) {
        return editorHolder.getRegisteredType();
      }
    }
    return null;
  }

  /**
   * Copy the custom editors registered in this instance to the given target registry.
   *
   * @param target the target registry to copy to
   * @param nestedProperty the nested property path of the target registry, if any.
   * If this is non-null, only editors registered for a path below this nested property
   * will be copied. If this is null, all editors will be copied.
   */
  protected void copyCustomEditorsTo(PropertyEditorRegistry target, @Nullable String nestedProperty) {
    String actualPropertyName =
            (nestedProperty != null ? PropertyAccessorUtils.getPropertyName(nestedProperty) : null);
    if (this.customEditors != null) {
      this.customEditors.forEach(target::registerCustomEditor);
    }
    if (this.customEditorsForPath != null) {
      this.customEditorsForPath.forEach((editorPath, editorHolder) -> {
        if (nestedProperty != null) {
          int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(editorPath);
          if (pos != -1) {
            String editorNestedProperty = editorPath.substring(0, pos);
            String editorNestedPath = editorPath.substring(pos + 1);
            if (editorNestedProperty.equals(nestedProperty) || editorNestedProperty.equals(actualPropertyName)) {
              target.registerCustomEditor(
                      editorHolder.getRegisteredType(), editorNestedPath, editorHolder.getPropertyEditor());
            }
          }
        }
        else {
          target.registerCustomEditor(
                  editorHolder.getRegisteredType(), editorPath, editorHolder.getPropertyEditor());
        }
      });
    }
  }

  /**
   * Add property paths with all variations of stripped keys and/or indexes.
   * Invokes itself recursively with nested paths.
   *
   * @param strippedPaths the result list to add to
   * @param nestedPath the current nested path
   * @param propertyPath the property path to check for keys/indexes to strip
   */
  private void addStrippedPropertyPaths(List<String> strippedPaths, String nestedPath, String propertyPath) {
    int startIndex = propertyPath.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR);
    if (startIndex != -1) {
      int endIndex = propertyPath.indexOf(PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR);
      if (endIndex != -1) {
        String prefix = propertyPath.substring(0, startIndex);
        String key = propertyPath.substring(startIndex, endIndex + 1);
        String suffix = propertyPath.substring(endIndex + 1);
        // Strip the first key.
        strippedPaths.add(nestedPath + prefix + suffix);
        // Search for further keys to strip, with the first key stripped.
        addStrippedPropertyPaths(strippedPaths, nestedPath + prefix, suffix);
        // Search for further keys to strip, with the first key not stripped.
        addStrippedPropertyPaths(strippedPaths, nestedPath + prefix + key, suffix);
      }
    }
  }

  /**
   * Holder for a registered custom editor with property name.
   * Keeps the PropertyEditor itself plus the type it was registered for.
   */
  private record CustomEditorHolder(PropertyEditor propertyEditor, @Nullable Class<?> registeredType) {

    private CustomEditorHolder(PropertyEditor propertyEditor, @Nullable Class<?> registeredType) {
      this.propertyEditor = propertyEditor;
      this.registeredType = registeredType;
    }

    private PropertyEditor getPropertyEditor() {
      return this.propertyEditor;
    }

    @Nullable
    private Class<?> getRegisteredType() {
      return this.registeredType;
    }

    @Nullable
    private PropertyEditor getPropertyEditor(@Nullable Class<?> requiredType) {
      // Special case: If no required type specified, which usually only happens for
      // Collection elements, or required type is not assignable to registered type,
      // which usually only happens for generic properties of type Object -
      // then return PropertyEditor if not registered for Collection or array type.
      // (If not registered for Collection or array, it is assumed to be intended
      // for elements.)
      if (this.registeredType == null ||
              (requiredType != null &&
                      (ClassUtils.isAssignable(this.registeredType, requiredType)
                              || ClassUtils.isAssignable(requiredType, this.registeredType))) ||
              (requiredType == null &&
                      (!Collection.class.isAssignableFrom(this.registeredType) && !this.registeredType.isArray()))) {
        return this.propertyEditor;
      }
      else {
        return null;
      }
    }
  }

}

