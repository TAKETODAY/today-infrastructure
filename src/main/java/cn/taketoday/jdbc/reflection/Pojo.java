package cn.taketoday.jdbc.reflection;

import cn.taketoday.context.factory.BeanProperty;

/**
 * Used internally to represent a plain old java object.
 */
public class Pojo {

  private final Object object;
  private final JdbcBeanMetadata metadata;
  private final boolean caseSensitive;

  public Pojo(JdbcBeanMetadata metadata, Object object) {
    this.object = object;
    this.metadata = metadata;
    this.caseSensitive = metadata.isCaseSensitive();
  }

  public Object getProperty(String propertyPath) {
    // String.split uses RegularExpression
    // this is overkill for every column for every row
    int index = propertyPath.indexOf('.');
    final JdbcBeanMetadata metadata = this.metadata;

    final BeanProperty beanProperty;
    if (index > 0) {
      final String property = propertyPath.substring(0, index);

      beanProperty = metadata.getBeanProperty(property);
      String newPath = propertyPath.substring(index + 1);
      Object propertyValue = beanProperty.getValue(object);

      if (propertyValue == null) {
        // 上一级为空,下一级自然为空
        return null;
      }

      JdbcBeanMetadata subMetadata = metadata.createProperty(beanProperty.getType(), this.caseSensitive);
      Pojo subPojo = new Pojo(subMetadata, propertyValue);
      return subPojo.getProperty(newPath);
    }
    else {
      beanProperty = metadata.getBeanProperty(propertyPath);
    }

    return beanProperty == null ? null : beanProperty.getValue(this.object);
  }

  public void setProperty(String propertyPath, Object value) {
    // String.split uses RegularExpression
    // this is overkill for every column for every row
    int index = propertyPath.indexOf('.');
    final JdbcBeanMetadata metadata = this.metadata;
    final BeanProperty beanProperty;
    if (index > 0) {
      final String property = propertyPath.substring(0, index);
      beanProperty = metadata.getBeanProperty(property);

      Object subValue = beanProperty.getValue(object);
      if (subValue == null) {
        subValue = beanProperty.newInstance();
        beanProperty.setValue(object, subValue);
      }

      JdbcBeanMetadata subMetadata = metadata.createProperty(beanProperty.getType(), this.caseSensitive);

      Pojo subPojo = new Pojo(subMetadata, subValue);
      String newPath = propertyPath.substring(index + 1);
      subPojo.setProperty(newPath, value);
    }
    else {
      beanProperty = metadata.getBeanProperty(propertyPath);
      // TODO 转换
      beanProperty.setValue(object, value);
    }
  }

  public Object getObject() {
    return this.object;
  }

}
