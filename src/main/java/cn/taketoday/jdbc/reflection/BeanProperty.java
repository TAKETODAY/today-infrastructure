package cn.taketoday.jdbc.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import cn.taketoday.context.reflect.ConstructorAccessor;
import cn.taketoday.context.reflect.PropertyAccessor;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ReflectionUtils;

/**
 * @author TODAY
 * @date 2020/12/30 23:26
 */
public class BeanProperty {
  private final Field field;
  private ConstructorAccessor constructor;
  private final PropertyAccessor propertyAccessor;

  public BeanProperty(Field field) {
    Assert.notNull(field, "field must not be null");
    this.field = field;
    this.propertyAccessor = ReflectionUtils.newPropertyAccessor(field);
  }

  public Object getValue(Object object) {
    return propertyAccessor.get(object);
  }

  public Object newInstance() {
    if (constructor == null) {
      final Class<?> fieldType = field.getType();
      if (fieldType.isArray()) {
        return Array.newInstance(fieldType.getComponentType(), 1);
      }
      constructor = fieldType.isPrimitive() ? null : ReflectionUtils.newConstructorAccessor(fieldType);
    }
    return constructor.newInstance();
  }

  public void setValue(Object obj, Object value) {
    propertyAccessor.set(obj, value);
  }

  public Class<?> getType() {
    return field.getType();
  }

  //

  public ConstructorAccessor getConstructor() {
    return constructor;
  }

  public PropertyAccessor getPropertyAccessor() {
    return propertyAccessor;
  }

  public Field getField() {
    return field;
  }
}
