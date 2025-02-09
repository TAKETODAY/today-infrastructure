/*
 * Copyright 2017 - 2025 the original author or authors.
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

import infra.beans.propertyeditors.ByteArrayPropertyEditor;
import infra.beans.propertyeditors.CharArrayPropertyEditor;
import infra.beans.propertyeditors.CharacterEditor;
import infra.beans.propertyeditors.CharsetEditor;
import infra.beans.propertyeditors.ClassArrayEditor;
import infra.beans.propertyeditors.ClassEditor;
import infra.beans.propertyeditors.CurrencyEditor;
import infra.beans.propertyeditors.CustomBooleanEditor;
import infra.beans.propertyeditors.CustomCollectionEditor;
import infra.beans.propertyeditors.CustomMapEditor;
import infra.beans.propertyeditors.CustomNumberEditor;
import infra.beans.propertyeditors.FileEditor;
import infra.beans.propertyeditors.InputSourceEditor;
import infra.beans.propertyeditors.InputStreamEditor;
import infra.beans.propertyeditors.LocaleEditor;
import infra.beans.propertyeditors.PathEditor;
import infra.beans.propertyeditors.PatternEditor;
import infra.beans.propertyeditors.PropertiesEditor;
import infra.beans.propertyeditors.ReaderEditor;
import infra.beans.propertyeditors.StringArrayPropertyEditor;
import infra.beans.propertyeditors.TimeZoneEditor;
import infra.beans.propertyeditors.URIEditor;
import infra.beans.propertyeditors.URLEditor;
import infra.beans.propertyeditors.UUIDEditor;
import infra.beans.propertyeditors.ZoneIdEditor;
import infra.core.conversion.ConversionService;
import infra.core.io.Resource;
import infra.core.io.ResourceArrayPropertyEditor;
import infra.lang.Nullable;
import infra.util.ClassUtils;

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
  private PropertyEditorRegistrar defaultEditorRegistrar;

  @Nullable
  private HashMap<Class<?>, PropertyEditor> defaultEditors;

  @Nullable
  private HashMap<Class<?>, PropertyEditor> overriddenDefaultEditors;

  @Nullable
  private LinkedHashMap<Class<?>, PropertyEditor> customEditors;

  @Nullable
  private LinkedHashMap<String, CustomEditorHolder> customEditorsForPath;

  @Nullable
  private HashMap<Class<?>, PropertyEditor> customEditorCache;

  /**
   * Specify a ConversionService to use for converting
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
   * such as {@link StringArrayPropertyEditor}.
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
    HashMap<Class<?>, PropertyEditor> editors = overriddenDefaultEditors;
    if (editors == null) {
      editors = new HashMap<>();
      this.overriddenDefaultEditors = editors;
    }
    editors.put(requiredType, propertyEditor);
  }

  /**
   * Set a registrar for default editors, as a lazy way of overriding default editors.
   * <p>This is expected to be a collaborator with {@link PropertyEditorRegistrySupport},
   * downcasting the given {@link PropertyEditorRegistry} accordingly and calling
   * {@link #overrideDefaultEditor} for registering additional default editors on it.
   *
   * @param registrar the registrar to call when default editors are actually needed
   * @see #overrideDefaultEditor
   * @since 5.0
   */
  public void setDefaultEditorRegistrar(PropertyEditorRegistrar registrar) {
    this.defaultEditorRegistrar = registrar;
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
    if (defaultEditorsActive) {
      HashMap<Class<?>, PropertyEditor> overriddenDefaultEditors = this.overriddenDefaultEditors;
      if (overriddenDefaultEditors == null && defaultEditorRegistrar != null) {
        defaultEditorRegistrar.registerCustomEditors(this);
      }

      if (overriddenDefaultEditors != null) {
        PropertyEditor editor = overriddenDefaultEditors.get(requiredType);
        if (editor != null) {
          return editor;
        }
      }
      HashMap<Class<?>, PropertyEditor> defaultEditors = this.defaultEditors;
      if (defaultEditors == null) {
        defaultEditors = createDefaultEditors();
        this.defaultEditors = defaultEditors;
      }
      return defaultEditors.get(requiredType);
    }
    return null;
  }

  /**
   * Actually register the default editors for this registry instance.
   */
  private HashMap<Class<?>, PropertyEditor> createDefaultEditors() {
    HashMap<Class<?>, PropertyEditor> defaultEditors = new HashMap<>(64);

    // Simple editors, without parameterization capabilities.
    // The JDK does not contain a default editor for any of these target types.
    defaultEditors.put(Charset.class, new CharsetEditor());
    defaultEditors.put(Class.class, new ClassEditor());
    defaultEditors.put(Class[].class, new ClassArrayEditor());
    defaultEditors.put(Currency.class, new CurrencyEditor());
    defaultEditors.put(File.class, new FileEditor());
    defaultEditors.put(InputStream.class, new InputStreamEditor());
    defaultEditors.put(InputSource.class, new InputSourceEditor());
    defaultEditors.put(Locale.class, new LocaleEditor());
    defaultEditors.put(Path.class, new PathEditor());
    defaultEditors.put(Pattern.class, new PatternEditor());
    defaultEditors.put(Properties.class, new PropertiesEditor());
    defaultEditors.put(Reader.class, new ReaderEditor());
    defaultEditors.put(Resource[].class, new ResourceArrayPropertyEditor());
    defaultEditors.put(TimeZone.class, new TimeZoneEditor());
    defaultEditors.put(URI.class, new URIEditor());
    defaultEditors.put(URL.class, new URLEditor());
    defaultEditors.put(UUID.class, new UUIDEditor());
    defaultEditors.put(ZoneId.class, new ZoneIdEditor());

    // Default instances of collection editors.
    // Can be overridden by registering custom instances of those as custom editors.
    defaultEditors.put(Collection.class, new CustomCollectionEditor(Collection.class));
    defaultEditors.put(Set.class, new CustomCollectionEditor(Set.class));
    defaultEditors.put(SortedSet.class, new CustomCollectionEditor(SortedSet.class));
    defaultEditors.put(List.class, new CustomCollectionEditor(List.class));
    defaultEditors.put(SortedMap.class, new CustomMapEditor(SortedMap.class));

    // Default editors for primitive arrays.
    defaultEditors.put(byte[].class, new ByteArrayPropertyEditor());
    defaultEditors.put(char[].class, new CharArrayPropertyEditor());

    // The JDK does not contain a default editor for char!
    defaultEditors.put(char.class, new CharacterEditor(false));
    defaultEditors.put(Character.class, new CharacterEditor(true));

    // Framework's CustomBooleanEditor accepts more flag values than the JDK's default editor.
    defaultEditors.put(boolean.class, new CustomBooleanEditor(false));
    defaultEditors.put(Boolean.class, new CustomBooleanEditor(true));

    // The JDK does not contain default editors for number wrapper types!
    // Override JDK primitive number editors with our own CustomNumberEditor.
    defaultEditors.put(byte.class, new CustomNumberEditor(Byte.class, false));
    defaultEditors.put(Byte.class, new CustomNumberEditor(Byte.class, true));
    defaultEditors.put(short.class, new CustomNumberEditor(Short.class, false));
    defaultEditors.put(Short.class, new CustomNumberEditor(Short.class, true));
    defaultEditors.put(int.class, new CustomNumberEditor(Integer.class, false));
    defaultEditors.put(Integer.class, new CustomNumberEditor(Integer.class, true));
    defaultEditors.put(long.class, new CustomNumberEditor(Long.class, false));
    defaultEditors.put(Long.class, new CustomNumberEditor(Long.class, true));
    defaultEditors.put(float.class, new CustomNumberEditor(Float.class, false));
    defaultEditors.put(Float.class, new CustomNumberEditor(Float.class, true));
    defaultEditors.put(double.class, new CustomNumberEditor(Double.class, false));
    defaultEditors.put(Double.class, new CustomNumberEditor(Double.class, true));
    defaultEditors.put(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, true));
    defaultEditors.put(BigInteger.class, new CustomNumberEditor(BigInteger.class, true));

    // Only register config value editors if explicitly requested.
    if (this.configValueEditorsActive) {
      StringArrayPropertyEditor sae = new StringArrayPropertyEditor();
      defaultEditors.put(String[].class, sae);
      defaultEditors.put(short[].class, sae);
      defaultEditors.put(int[].class, sae);
      defaultEditors.put(long[].class, sae);
    }

    return defaultEditors;
  }

  /**
   * Copy the default editors registered in this instance to the given target registry.
   *
   * @param target the target registry to copy to
   */
  protected void copyDefaultEditorsTo(PropertyEditorRegistrySupport target) {
    target.defaultEditors = this.defaultEditors;
    target.defaultEditorsActive = this.defaultEditorsActive;
    target.configValueEditorsActive = this.configValueEditorsActive;
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
      LinkedHashMap<String, CustomEditorHolder> editors = this.customEditorsForPath;
      if (editors == null) {
        editors = new LinkedHashMap<>(16);
        this.customEditorsForPath = editors;
      }
      editors.put(propertyPath, new CustomEditorHolder(propertyEditor, requiredType));
    }
    else {
      LinkedHashMap<Class<?>, PropertyEditor> editors = this.customEditors;
      if (editors == null) {
        editors = new LinkedHashMap<>(16);
        this.customEditors = editors;
      }
      editors.put(requiredType, propertyEditor);
      this.customEditorCache = null;
    }
  }

  @Override
  @Nullable
  public PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath) {
    Class<?> requiredTypeToUse = requiredType;
    if (propertyPath != null) {
      LinkedHashMap<String, CustomEditorHolder> editors = this.customEditorsForPath;
      if (editors != null) {
        // Check property-specific editor first.
        PropertyEditor editor = getCustomEditor(propertyPath, requiredType, editors);
        if (editor == null) {
          ArrayList<String> strippedPaths = new ArrayList<>();
          addStrippedPropertyPaths(strippedPaths, "", propertyPath);
          for (Iterator<String> it = strippedPaths.iterator(); it.hasNext() && editor == null; ) {
            String strippedPath = it.next();
            editor = getCustomEditor(strippedPath, requiredType, editors);
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
    if (propertyPath != null && customEditorsForPath != null) {
      for (Map.Entry<String, CustomEditorHolder> entry : customEditorsForPath.entrySet()) {
        if (PropertyAccessorUtils.matchesProperty(entry.getKey(), propertyPath)
                && entry.getValue().getPropertyEditor(elementType) != null) {
          return true;
        }
      }
    }
    // No property-specific editor -> check type-specific editor.
    return elementType != null
            && this.customEditors != null
            && this.customEditors.containsKey(elementType);
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
  private static PropertyEditor getCustomEditor(String propertyName,
          @Nullable Class<?> requiredType, LinkedHashMap<String, CustomEditorHolder> customEditorsForPath) {
    CustomEditorHolder holder = customEditorsForPath.get(propertyName);
    return holder != null ? holder.getPropertyEditor(requiredType) : null;
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
    if (requiredType == null) {
      return null;
    }
    LinkedHashMap<Class<?>, PropertyEditor> customEditors = this.customEditors;
    if (customEditors == null) {
      return null;
    }
    // Check directly registered editor for type.
    PropertyEditor editor = customEditors.get(requiredType);
    if (editor == null) {
      // Check cached editor for type, registered for superclass or interface.
      HashMap<Class<?>, PropertyEditor> customEditorCache = this.customEditorCache;
      if (customEditorCache != null) {
        editor = customEditorCache.get(requiredType);
      }
      if (editor == null) {
        // Find editor for superclass or interface.
        for (Map.Entry<Class<?>, PropertyEditor> entry : customEditors.entrySet()) {
          Class<?> key = entry.getKey();
          if (key.isAssignableFrom(requiredType)) {
            editor = entry.getValue();
            // Cache editor for search type, to avoid the overhead
            // of repeated assignable-from checks.
            if (customEditorCache == null) {
              customEditorCache = new HashMap<>();
              this.customEditorCache = customEditorCache;
            }
            customEditorCache.put(requiredType, editor);
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
    LinkedHashMap<String, CustomEditorHolder> editors = this.customEditorsForPath;
    if (editors != null) {
      CustomEditorHolder editorHolder = editors.get(propertyName);
      if (editorHolder == null) {
        ArrayList<String> strippedPaths = new ArrayList<>();
        addStrippedPropertyPaths(strippedPaths, "", propertyName);
        Iterator<String> it = strippedPaths.iterator();
        while (editorHolder == null && it.hasNext()) {
          editorHolder = editors.get(/*String strippedName = */it.next());
        }
      }
      if (editorHolder != null) {
        return editorHolder.registeredType;
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
            nestedProperty != null ? PropertyAccessorUtils.getPropertyName(nestedProperty) : null;
    if (customEditors != null) {
      for (Map.Entry<Class<?>, PropertyEditor> entry : customEditors.entrySet()) {
        target.registerCustomEditor(entry.getKey(), entry.getValue());
      }
    }
    if (customEditorsForPath != null) {
      for (Map.Entry<String, CustomEditorHolder> entry : customEditorsForPath.entrySet()) {
        String editorPath = entry.getKey();
        CustomEditorHolder editorHolder = entry.getValue();
        if (nestedProperty != null) {
          int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(editorPath);
          if (pos != -1) {
            String editorNestedProperty = editorPath.substring(0, pos);
            String editorNestedPath = editorPath.substring(pos + 1);
            if (editorNestedProperty.equals(nestedProperty) || editorNestedProperty.equals(actualPropertyName)) {
              target.registerCustomEditor(editorHolder.registeredType, editorNestedPath, editorHolder.propertyEditor);
            }
          }
        }
        else {
          target.registerCustomEditor(editorHolder.registeredType, editorPath, editorHolder.propertyEditor);
        }
      }
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
  private static final class CustomEditorHolder {

    @Nullable
    public final Class<?> registeredType;

    public final PropertyEditor propertyEditor;

    private CustomEditorHolder(PropertyEditor propertyEditor, @Nullable Class<?> registeredType) {
      this.propertyEditor = propertyEditor;
      this.registeredType = registeredType;
    }

    @Nullable
    private PropertyEditor getPropertyEditor(@Nullable Class<?> requiredType) {
      // Special case: If no required type specified, which usually only happens for
      // Collection elements, or required type is not assignable to registered type,
      // which usually only happens for generic properties of type Object -
      // then return PropertyEditor if not registered for Collection or array type.
      // (If not registered for Collection or array, it is assumed to be intended
      // for elements.)
      Class<?> registeredType = this.registeredType;
      if (registeredType == null
              || (requiredType != null && (ClassUtils.isAssignable(registeredType, requiredType) || ClassUtils.isAssignable(requiredType, registeredType)))
              || (requiredType == null && (!Collection.class.isAssignableFrom(registeredType) && !registeredType.isArray()))) {
        return this.propertyEditor;
      }
      else {
        return null;
      }
    }
  }

}

