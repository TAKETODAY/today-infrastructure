/*
 * Copyright 2012-present the original author or authors.
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

package infra.context.properties.bind;

import org.jspecify.annotations.Nullable;

import java.beans.PropertyEditor;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import infra.beans.BeanUtils;
import infra.beans.PropertyEditorRegistry;
import infra.beans.SimpleTypeConverter;
import infra.beans.propertyeditors.CustomBooleanEditor;
import infra.beans.propertyeditors.CustomNumberEditor;
import infra.beans.propertyeditors.FileEditor;
import infra.core.ResolvableType;
import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionException;
import infra.core.conversion.ConversionFailedException;
import infra.core.conversion.ConversionService;
import infra.core.conversion.ConverterNotFoundException;
import infra.core.conversion.support.GenericConversionService;
import infra.core.io.Resource;
import infra.format.support.ApplicationConversionService;
import infra.util.CollectionUtils;

/**
 * Utility to handle any conversion needed during binding. This class is not thread-safe
 * and so a new instance is created for each top-level bind.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class BindConverter {

  @Nullable
  private static BindConverter sharedInstance;

  private final ArrayList<ConversionService> delegates;

  private BindConverter(@Nullable List<ConversionService> conversionServices,
          @Nullable Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
    ArrayList<ConversionService> delegates = new ArrayList<>();
    delegates.add(new TypeConverterConversionService(propertyEditorInitializer));
    boolean hasApplication = false;
    if (CollectionUtils.isNotEmpty(conversionServices)) {
      for (ConversionService conversionService : conversionServices) {
        delegates.add(conversionService);
        hasApplication = hasApplication || conversionService instanceof ApplicationConversionService;
      }
    }
    if (!hasApplication) {
      delegates.add(ApplicationConversionService.getSharedInstance());
    }
    this.delegates = delegates;
  }

  public boolean canConvert(@Nullable Object source, ResolvableType type, Annotation @Nullable ... targetAnnotations) {
    TypeDescriptor sourceType = TypeDescriptor.forObject(source);
    TypeDescriptor targetType = new TypeDescriptor(type, null, targetAnnotations);
    for (ConversionService service : this.delegates) {
      if (service.canConvert(sourceType, targetType)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public <T> T convert(@Nullable Object source, Bindable<T> target) {
    return convert(source, target.getType(), target.getAnnotations());
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public <T> T convert(@Nullable Object source, ResolvableType type, Annotation... targetAnnotations) {
    if (source == null) {
      return null;
    }
    TypeDescriptor sourceType = TypeDescriptor.forObject(source);
    TypeDescriptor targetType = new TypeDescriptor(type, null, targetAnnotations);

    ConversionException failure = null;
    for (ConversionService delegate : this.delegates) {
      try {
        if (delegate.canConvert(sourceType, targetType)) {
          return (T) delegate.convert(source, sourceType, targetType);
        }
      }
      catch (ConversionException ex) {
        if (failure == null && ex instanceof ConversionFailedException) {
          failure = ex;
        }
      }
    }
    throw failure != null ? failure : new ConverterNotFoundException(sourceType, targetType);
  }

  static BindConverter get(@Nullable List<ConversionService> conversionServices,
          @Nullable Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
    boolean sharedApplicationConversionService = (conversionServices == null) || (conversionServices.size() == 1
            && conversionServices.get(0) == ApplicationConversionService.getSharedInstance());
    if (propertyEditorInitializer == null && sharedApplicationConversionService) {
      return getSharedInstance();
    }
    return new BindConverter(conversionServices, propertyEditorInitializer);
  }

  private static BindConverter getSharedInstance() {
    if (sharedInstance == null) {
      sharedInstance = new BindConverter(null, null);
    }
    return sharedInstance;
  }

  /**
   * A {@link ConversionService} implementation that delegates to a
   * {@link SimpleTypeConverter}. Allows {@link PropertyEditor} based conversion for
   * simple types, arrays and collections.
   */
  private static class TypeConverterConversionService extends GenericConversionService {

    TypeConverterConversionService(@Nullable Consumer<PropertyEditorRegistry> initializer) {
      ApplicationConversionService.addDelimitedStringConverters(this);
      addConverter(new TypeConverterConverter(initializer));
    }

    @Override
    public boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
      // Prefer conversion service to handle things like String to char[].
      if (targetType.isArray()) {
        TypeDescriptor descriptor = targetType.getElementDescriptor();
        if (descriptor != null && descriptor.isPrimitive()) {
          return false;
        }
      }
      return super.canConvert(sourceType, targetType);
    }

  }

  /**
   * {@link ConditionalGenericConverter} that delegates to {@link SimpleTypeConverter}.
   */
  private static class TypeConverterConverter implements ConditionalGenericConverter {

    private static final Set<Class<?>> EXCLUDED_EDITORS = Set.of(
            CustomNumberEditor.class, CustomBooleanEditor.class, FileEditor.class
    );

    @Nullable
    private final Consumer<PropertyEditorRegistry> initializer;

    // SimpleTypeConverter is not thread-safe to use for conversion but we can use it
    // in a thread-safe way to check if conversion is possible.
    private final SimpleTypeConverter matchesOnlyTypeConverter;

    TypeConverterConverter(@Nullable Consumer<PropertyEditorRegistry> initializer) {
      this.initializer = initializer;
      this.matchesOnlyTypeConverter = createTypeConverter();
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
      return Set.of(
              new ConvertiblePair(String.class, Object.class),
              new ConvertiblePair(String.class, Resource[].class),
              new ConvertiblePair(String.class, Collection.class)
      );
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      Class<?> type = targetType.getType();
      if (type == null || type == Object.class || Map.class.isAssignableFrom(type)) {
        return false;
      }
      if (Collection.class.isAssignableFrom(type)) {
        TypeDescriptor elementType = targetType.getElementDescriptor();
        if (elementType == null || (!Resource.class.isAssignableFrom(elementType.getType()))) {
          return false;
        }
      }
      PropertyEditor editor = this.matchesOnlyTypeConverter.getDefaultEditor(type);
      if (editor == null) {
        editor = this.matchesOnlyTypeConverter.findCustomEditor(type, null);
      }
      if (editor == null && String.class != type) {
        editor = BeanUtils.findEditorByConvention(type);
      }
      return (editor != null && !EXCLUDED_EDITORS.contains(editor.getClass()));
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      return createTypeConverter().convertIfNecessary(source, targetType.getType(), targetType);
    }

    private SimpleTypeConverter createTypeConverter() {
      SimpleTypeConverter typeConverter = new SimpleTypeConverter();
      if (this.initializer != null) {
        this.initializer.accept(typeConverter);
      }
      return typeConverter;
    }

  }

}
