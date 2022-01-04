/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.PropertyReadOnlyException;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.beans.support.BeanMetadata;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.beans.support.BeanPropertyAccessor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.dao.DataRetrievalFailureException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link RowMapper} implementation that converts a row into a new instance
 * of the specified mapped target class. The mapped target class must be a
 * top-level class and it must have a default or no-arg constructor.
 *
 * <p>Column values are mapped based on matching the column name as obtained from result set
 * meta-data to public setters for the corresponding properties. The names are matched either
 * directly or by transforming a name separating the parts with underscores to the same name
 * using "camel" case.
 *
 * <p>Mapping is provided for fields in the target class for many common types, e.g.:
 * String, boolean, Boolean, byte, Byte, short, Short, int, Integer, long, Long,
 * float, Float, double, Double, BigDecimal, {@code java.util.Date}, etc.
 *
 * <p>To facilitate mapping between columns and fields that don't have matching names,
 * try using column aliases in the SQL statement like "select fname as first_name from customer".
 *
 * <p>For 'null' values read from the database, we will attempt to call the setter, but in the case of
 * Java primitives, this causes a TypeMismatchException. This class can be configured (using the
 * primitivesDefaultedForNullValue property) to trap this exception and use the primitives default value.
 * Be aware that if you use the values from the generated bean to update the database the primitive value
 * will have been set to the primitive's default value instead of null.
 *
 * <p>Please note that this class is designed to provide convenience rather than high performance.
 * For best performance, consider using a custom {@link RowMapper} implementation.
 *
 * @param <T> the result type
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see DataClassRowMapper
 * @since 4.0
 */
public class BeanPropertyRowMapper<T> implements RowMapper<T> {
  private static final Logger log = LoggerFactory.getLogger(BeanPropertyRowMapper.class);

  /** The class we are mapping to. */
  @Nullable
  private Class<T> mappedClass;
  // @since 4.0
  private final boolean collectPropertiesFromMethods;

  /** Whether we're strictly validating. */
  private boolean checkFullyPopulated = false;

  /** Whether we're defaulting primitives when mapping a null value. */
  private boolean primitivesDefaultedForNullValue = false;

  /** ConversionService for binding JDBC values to bean properties. */
  @Nullable
  private ConversionService conversionService = DefaultConversionService.getSharedInstance();

  /** Map of the fields we provide mapping for. */
  @Nullable
  private Map<String, BeanProperty> mappedFields;

  /** Set of bean properties we provide mapping for. */
  @Nullable
  private Set<String> mappedProperties;

  @Nullable
  private BeanMetadata metadata;

  @Nullable
  protected BeanPropertyAccessor accessor;

  /**
   * Create a new {@code BeanPropertyRowMapper} for bean-style configuration.
   *
   * @see #setMappedClass
   * @see #setCheckFullyPopulated
   */
  public BeanPropertyRowMapper() {
    this.collectPropertiesFromMethods = false;
  }

  /**
   * Create a new {@code BeanPropertyRowMapper}, accepting unpopulated
   * properties in the target bean.
   *
   * @param mappedClass the class that each row should be mapped to
   */
  public BeanPropertyRowMapper(Class<T> mappedClass) {
    this(mappedClass, false);
  }

  /**
   * Create a new {@code BeanPropertyRowMapper}, accepting unpopulated
   * properties in the target bean.
   *
   * @param mappedClass the class that each row should be mapped to
   */
  public BeanPropertyRowMapper(Class<T> mappedClass, boolean collectPropertiesFromMethods) {
    this.collectPropertiesFromMethods = collectPropertiesFromMethods;
    initialize(mappedClass);
  }

  /**
   * Create a new {@code BeanPropertyRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   * @param checkFullyPopulated whether we're strictly validating that
   * all bean properties have been mapped from corresponding database fields
   */
  public BeanPropertyRowMapper(
          Class<T> mappedClass, boolean checkFullyPopulated, boolean collectPropertiesFromMethods) {
    this.collectPropertiesFromMethods = collectPropertiesFromMethods;
    this.checkFullyPopulated = checkFullyPopulated;
    initialize(mappedClass);
  }

  /**
   * Set the class that each row should be mapped to.
   */
  public void setMappedClass(Class<T> mappedClass) {
    if (this.mappedClass == null) {
      initialize(mappedClass);
    }
    else {
      if (this.mappedClass != mappedClass) {
        throw new InvalidDataAccessApiUsageException("The mapped class can not be reassigned to map to " +
                mappedClass + " since it is already providing mapping for " + this.mappedClass);
      }
    }
  }

  /**
   * Get the class that we are mapping to.
   */
  @Nullable
  public final Class<T> getMappedClass() {
    return this.mappedClass;
  }

  @Nullable
  public BeanMetadata getMetadata() {
    return metadata;
  }

  /**
   * Set whether we're strictly validating that all bean properties have been mapped
   * from corresponding database fields.
   * <p>Default is {@code false}, accepting unpopulated properties in the target bean.
   */
  public void setCheckFullyPopulated(boolean checkFullyPopulated) {
    this.checkFullyPopulated = checkFullyPopulated;
  }

  /**
   * Return whether we're strictly validating that all bean properties have been
   * mapped from corresponding database fields.
   */
  public boolean isCheckFullyPopulated() {
    return this.checkFullyPopulated;
  }

  /**
   * Set whether we're defaulting Java primitives in the case of mapping a null value
   * from corresponding database fields.
   * <p>Default is {@code false}, throwing an exception when nulls are mapped to Java primitives.
   */
  public void setPrimitivesDefaultedForNullValue(boolean primitivesDefaultedForNullValue) {
    this.primitivesDefaultedForNullValue = primitivesDefaultedForNullValue;
  }

  /**
   * Return whether we're defaulting Java primitives in the case of mapping a null value
   * from corresponding database fields.
   */
  public boolean isPrimitivesDefaultedForNullValue() {
    return this.primitivesDefaultedForNullValue;
  }

  /**
   * Set a {@link ConversionService} for binding JDBC values to bean properties,
   * or {@code null} for none.
   * <p>Default is a {@link DefaultConversionService}, This
   * provides support for {@code java.time} conversion and other special types.
   *
   * @see #initBeanPropertyAccessor(BeanPropertyAccessor)
   */
  public void setConversionService(@Nullable ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  /**
   * Return a {@link ConversionService} for binding JDBC values to bean properties,
   * or {@code null} if none.
   */
  @Nullable
  public ConversionService getConversionService() {
    return this.conversionService;
  }

  /**
   * Initialize the mapping meta-data for the given class.
   *
   * @param mappedClass the mapped class
   */
  protected void initialize(Class<T> mappedClass) {
    this.mappedClass = mappedClass;
    this.mappedFields = new HashMap<>();
    this.mappedProperties = new HashSet<>();
    BeanMetadata metadata = BeanMetadata.from(mappedClass, collectPropertiesFromMethods);
    this.metadata = metadata;
    this.accessor = new BeanPropertyAccessor();

    initBeanPropertyAccessor(accessor);

    for (BeanProperty property : metadata) {
      if (!property.isReadOnly()) {
        String lowerCaseName = lowerCaseName(property.getPropertyName());
        this.mappedFields.put(lowerCaseName, property);
        String underscoreName = underscoreName(property.getPropertyName());
        if (!lowerCaseName.equals(underscoreName)) {
          this.mappedFields.put(underscoreName, property);
        }
        this.mappedProperties.add(property.getPropertyName());
      }
    }
  }

  /**
   * Remove the specified property from the mapped fields.
   *
   * @param propertyName the property name (as used by property descriptors)
   */
  protected void suppressProperty(String propertyName) {
    if (this.mappedFields != null) {
      this.mappedFields.remove(lowerCaseName(propertyName));
      this.mappedFields.remove(underscoreName(propertyName));
    }
  }

  /**
   * Convert the given name to lower case.
   * By default, conversions will happen within the US locale.
   *
   * @param name the original name
   * @return the converted name
   */
  protected String lowerCaseName(String name) {
    return name.toLowerCase(Locale.US);
  }

  /**
   * Convert a name in camelCase to an underscored name in lower case.
   * Any upper case letters are converted to lower case with a preceding underscore.
   *
   * @param name the original name
   * @return the converted name
   * @see #lowerCaseName
   */
  protected String underscoreName(String name) {
    return StringUtils.camelCaseToUnderscore(name);
  }

  /**
   * Extract the values for all columns in the current row.
   * <p>Utilizes public setters and result set meta-data.
   *
   * @see ResultSetMetaData
   */
  @Override
  public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
    T mappedObject = constructMappedInstance(rs);

    ResultSetMetaData rsmd = rs.getMetaData();
    int columnCount = rsmd.getColumnCount();
    Set<String> populatedProperties = isCheckFullyPopulated() ? new HashSet<>() : null;

    for (int index = 1; index <= columnCount; index++) {
      String column = JdbcUtils.lookupColumnName(rsmd, index);
      String field = lowerCaseName(StringUtils.delete(column, " "));
      BeanProperty property = this.mappedFields != null ? this.mappedFields.get(field) : null;
      if (property != null) {
        try {
          Object value = getColumnValue(rs, index, property);
          if (rowNumber == 0 && log.isDebugEnabled()) {
            log.debug("Mapping column '{}' to property '{}' of type '{}'",
                    column, property.getName(), ClassUtils.getQualifiedName(property.getType()));
          }
          try {
            accessor.setProperty(mappedObject, metadata, property.getName(), value);
          }
          catch (TypeMismatchException ex) {
            if (value == null && this.primitivesDefaultedForNullValue) {
              if (log.isDebugEnabled()) {
                log.debug("Intercepted TypeMismatchException for row {} and column '{}'" +
                                " with null value when setting property '{}' of type '{}' on object: {}",
                        rowNumber, column, property.getName(), ClassUtils.getQualifiedName(property.getType()), mappedObject, ex);
              }
            }
            else {
              throw ex;
            }
          }
          if (populatedProperties != null) {
            populatedProperties.add(property.getName());
          }
        }
        catch (PropertyReadOnlyException ex) {
          throw new DataRetrievalFailureException(
                  "Unable to map column '" + column + "' to property '" + property.getName() + "'", ex);
        }
      }
      else {
        // No BeanProperty found
        if (rowNumber == 0 && log.isDebugEnabled()) {
          log.debug("No property found for column '{}' mapped to field '{}'", column, field);
        }
      }
    }

    if (populatedProperties != null && !populatedProperties.equals(this.mappedProperties)) {
      throw new InvalidDataAccessApiUsageException("Given ResultSet does not contain all fields " +
              "necessary to populate object of " + this.mappedClass + ": " + this.mappedProperties);
    }

    return mappedObject;
  }

  /**
   * Construct an instance of the mapped class for the current row.
   *
   * @param rs the ResultSet to map (pre-initialized for the current row)
   * @return a corresponding instance of the mapped class
   * @throws SQLException if an SQLException is encountered
   */
  @SuppressWarnings("unchecked")
  protected T constructMappedInstance(ResultSet rs) throws SQLException {
    Assert.state(this.mappedClass != null, "Mapped class was not specified");
    return (T) metadata.newInstance();
  }

  /**
   * Initialize the given BeanWrapper to be used for row mapping.
   * To be called for each row.
   * <p>The default implementation applies the configured {@link ConversionService},
   * if any. Can be overridden in subclasses.
   *
   * @param accessor the BeanPropertyAccessor to initialize
   * @see #getConversionService()
   * @see BeanPropertyAccessor#setConversionService
   */
  protected void initBeanPropertyAccessor(BeanPropertyAccessor accessor) {
    ConversionService cs = getConversionService();
    if (cs != null) {
      accessor.setConversionService(cs);
    }
    accessor.setMetadata(metadata);
  }

  /**
   * Retrieve a JDBC object value for the specified column.
   * <p>The default implementation delegates to
   * {@link #getColumnValue(ResultSet, int, Class)}.
   *
   * @param rs is the ResultSet holding the data
   * @param index is the column index
   * @param pd the bean property that each result object is expected to match
   * @return the Object value
   * @throws SQLException in case of extraction failure
   * @see #getColumnValue(ResultSet, int, Class)
   */
  @Nullable
  protected Object getColumnValue(ResultSet rs, int index, BeanProperty pd) throws SQLException {
    return JdbcUtils.getResultSetValue(rs, index, pd.getType());
  }

  /**
   * Retrieve a JDBC object value for the specified column.
   * <p>The default implementation calls
   * {@link JdbcUtils#getResultSetValue(ResultSet, int, Class)}.
   * Subclasses may override this to check specific value types upfront,
   * or to post-process values return from {@code getResultSetValue}.
   *
   * @param rs is the ResultSet holding the data
   * @param index is the column index
   * @param paramType the target parameter type
   * @return the Object value
   * @throws SQLException in case of extraction failure
   * @see cn.taketoday.jdbc.support.JdbcUtils#getResultSetValue(ResultSet, int, Class)
   */
  @Nullable
  protected Object getColumnValue(ResultSet rs, int index, Class<?> paramType) throws SQLException {
    return JdbcUtils.getResultSetValue(rs, index, paramType);
  }

  /**
   * Static factory method to create a new {@code BeanPropertyRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   * @see #from(Class, ConversionService)
   */
  public static <T> BeanPropertyRowMapper<T> from(Class<T> mappedClass) {
    return new BeanPropertyRowMapper<>(mappedClass);
  }

  /**
   * Static factory method to create a new {@code BeanPropertyRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   * @param conversionService the {@link ConversionService} for binding
   * JDBC values to bean properties, or {@code null} for none
   * @see #from(Class)
   * @see #setConversionService
   */
  public static <T> BeanPropertyRowMapper<T> from(
          Class<T> mappedClass, @Nullable ConversionService conversionService) {

    BeanPropertyRowMapper<T> rowMapper = from(mappedClass);
    rowMapper.setConversionService(conversionService);
    return rowMapper;
  }

}
