package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.jdbc.JdbcOperations;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2021/1/7 22:49
 */
public class ObjectPropertySetter {

  @Nullable
  private final PropertyPath propertyPath;
  private final BeanProperty beanProperty; // cache

  private final TypeHandler<?> typeHandler;

  @Nullable
  private final PrimitiveTypeNullHandler primitiveTypeNullHandler;

  public ObjectPropertySetter(
          @Nullable PropertyPath propertyPath, BeanProperty beanProperty, JdbcOperations operations) {
    this(propertyPath, beanProperty,
            operations.getTypeHandler(beanProperty.getType()),
            operations.getPrimitiveTypeNullHandler());
  }

  public ObjectPropertySetter(
          @Nullable PropertyPath propertyPath, BeanProperty beanProperty,
          TypeHandler<?> typeHandler, @Nullable PrimitiveTypeNullHandler primitiveTypeNullHandler) {
    Assert.notNull(typeHandler, "TypeHandler is required");
    Assert.notNull(beanProperty, "BeanProperty is required");
    this.typeHandler = typeHandler;
    this.propertyPath = propertyPath;
    this.beanProperty = beanProperty;
    this.primitiveTypeNullHandler = primitiveTypeNullHandler;
  }

  /**
   * Set the data to {@code obj} from given {@code columnIndex} and {@link ResultSet}
   *
   * @param obj object to set
   * @param resultSet jdbc resultSet
   * @param columnIndex current column index
   * @throws SQLException when data fetch failed
   */
  public void setTo(Object obj, ResultSet resultSet, int columnIndex) throws SQLException {
    Object result = getResult(resultSet, columnIndex);
    if (result == null && beanProperty.isPrimitive()) {
      if (primitiveTypeNullHandler != null) {
        if (propertyPath != null) {
          obj = propertyPath.getNestedObject(obj);
        }
        primitiveTypeNullHandler.handleNull(beanProperty, obj);
      }
    }
    else {
      if (propertyPath != null) {
        propertyPath.set(obj, result);
      }
      else {
        beanProperty.setValue(obj, result);
      }
    }
  }

  /**
   * Get result from {@link ResultSet}.
   * <p>
   * Obtain from {@link TypeHandler}, if it fails, use the default acquisition method
   * </p>
   *
   * @param resultSet Target result set
   * @return data object
   * @throws SQLException If {@link ResultSet#getObject(int)} failed
   */
  @Nullable
  private Object getResult(ResultSet resultSet, int columnIndex) throws SQLException {
    try {
      return typeHandler.getResult(resultSet, columnIndex);
    }
    catch (SQLException e) {
      // maybe data conversion error
      return resultSet.getObject(columnIndex);
    }
  }

}
