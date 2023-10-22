/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.bind;

import java.beans.PropertyEditor;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.PropertyEditorRegistry;
import cn.taketoday.beans.SimpleTypeConverter;
import cn.taketoday.beans.propertyeditors.CustomBooleanEditor;
import cn.taketoday.beans.propertyeditors.CustomNumberEditor;
import cn.taketoday.beans.propertyeditors.FileEditor;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConditionalGenericConverter;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.ConverterNotFoundException;
import cn.taketoday.core.conversion.support.GenericConversionService;
import cn.taketoday.core.io.Resource;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

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

  public boolean canConvert(Object source, ResolvableType type, @Nullable Annotation... targetAnnotations) {
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
  public <T> T convert(Object source, Bindable<T> target) {
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

    TypeConverterConversionService(Consumer<PropertyEditorRegistry> initializer) {
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
