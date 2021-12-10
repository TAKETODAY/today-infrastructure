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

import org.apache.ibatis.reflection.wrapper.BeanWrapper;
import org.hibernate.TypeMismatchException;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.support.BeanUtils;
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
 * @since 2.5
 */
public class BeanPropertyRowMapper<T> implements RowMapper<T> {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /** The class we are mapping to. */
  @Nullable
  private Class<T> mappedClass;

  /** Whether we're strictly validating. */
  private boolean checkFullyPopulated = false;

  /** Whether we're defaulting primitives when mapping a null value. */
  private boolean primitivesDefaultedForNullValue = false;

  /** ConversionService for binding JDBC values to bean properties. */
  @Nullable
  private ConversionService conversionService = DefaultConversionService.getSharedInstance();

  /** Map of the fields we provide mapping for. */
  @Nullable
  private Map<String, PropertyDescriptor> mappedFields;

  /** Set of bean properties we provide mapping for. */
  @Nullable
  private Set<String> mappedProperties;

  /**
   * Create a new {@code BeanPropertyRowMapper} for bean-style configuration.
   *
   * @see #setMappedClass
   * @see #setCheckFullyPopulated
   */
  public BeanPropertyRowMapper() { }

  /**
   * Create a new {@code BeanPropertyRowMapper}, accepting unpopulated
   * properties in the target bean.
   *
   * @param mappedClass the class that each row should be mapped to
   */
  public BeanPropertyRowMapper(Class<T> mappedClass) {
    initialize(mappedClass);
  }

  /**
   * Create a new {@code BeanPropertyRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   * @param checkFullyPopulated whether we're strictly validating that
   * all bean properties have been mapped from corresponding database fields
   */
  public BeanPropertyRowMapper(Class<T> mappedClass, boolean checkFullyPopulated) {
    initialize(mappedClass);
    this.checkFullyPopulated = checkFullyPopulated;
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
   * <p>Default is a {@link DefaultConversionService}, as of Spring 4.3. This
   * provides support for {@code java.time} conversion and other special types.
   *
   * @see #initBeanWrapper(BeanWrapper)
   * @since 4.0
   */
  public void setConversionService(@Nullable ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  /**
   * Return a {@link ConversionService} for binding JDBC values to bean properties,
   * or {@code null} if none.
   *
   * @since 4.0
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

    for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(mappedClass)) {
      if (pd.getWriteMethod() != null) {
        String lowerCaseName = lowerCaseName(pd.getName());
        this.mappedFields.put(lowerCaseName, pd);
        String underscoreName = underscoreName(pd.getName());
        if (!lowerCaseName.equals(underscoreName)) {
          this.mappedFields.put(underscoreName, pd);
        }
        this.mappedProperties.add(pd.getName());
      }
    }
  }

  /**
   * Remove the specified property from the mapped fields.
   *
   * @param propertyName the property name (as used by property descriptors)
   * @since 4.0
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
   * @since 4.0
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
   * @since 4.0
   */
  protected String underscoreName(String name) {
    if (!StringUtils.isNotEmpty(name)) {
      return "";
    }

    StringBuilder result = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (Character.isUpperCase(c)) {
        result.append('_').append(Character.toLowerCase(c));
      }
      else {
        result.append(c);
      }
    }
    return result.toString();
  }

  /**
   * Extract the values for all columns in the current row.
   * <p>Utilizes public setters and result set meta-data.
   *
   * @see ResultSetMetaData
   */
  @Override
  public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
    BeanWrapperImpl bw = new BeanWrapperImpl();
    initBeanWrapper(bw);

    T mappedObject = constructMappedInstance(rs, bw);
    bw.setBeanInstance(mappedObject);

    ResultSetMetaData rsmd = rs.getMetaData();
    int columnCount = rsmd.getColumnCount();
    Set<String> populatedProperties = (isCheckFullyPopulated() ? new HashSet<>() : null);

    for (int index = 1; index <= columnCount; index++) {
      String column = JdbcUtils.lookupColumnName(rsmd, index);
      String field = lowerCaseName(StringUtils.delete(column, " "));
      PropertyDescriptor pd = (this.mappedFields != null ? this.mappedFields.get(field) : null);
      if (pd != null) {
        try {
          Object value = getColumnValue(rs, index, pd);
          if (rowNumber == 0 && logger.isDebugEnabled()) {
            logger.debug("Mapping column '" + column + "' to property '" + pd.getName() +
                    "' of type '" + ClassUtils.getQualifiedName(pd.getPropertyType()) + "'");
          }
          try {
            bw.setPropertyValue(pd.getName(), value);
          }
          catch (TypeMismatchException ex) {
            if (value == null && this.primitivesDefaultedForNullValue) {
              if (logger.isDebugEnabled()) {
                logger.debug("Intercepted TypeMismatchException for row " + rowNumber +
                        " and column '" + column + "' with null value when setting property '" +
                        pd.getName() + "' of type '" +
                        ClassUtils.getQualifiedName(pd.getPropertyType()) +
                        "' on object: " + mappedObject, ex);
              }
            }
            else {
              throw ex;
            }
          }
          if (populatedProperties != null) {
            populatedProperties.add(pd.getName());
          }
        }
        catch (NotWritablePropertyException ex) {
          throw new DataRetrievalFailureException(
                  "Unable to map column '" + column + "' to property '" + pd.getName() + "'", ex);
        }
      }
      else {
        // No PropertyDescriptor found
        if (rowNumber == 0 && logger.isDebugEnabled()) {
          logger.debug("No property found for column '" + column + "' mapped to field '" + field + "'");
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
   * @param tc a TypeConverter with this RowMapper's conversion service
   * @return a corresponding instance of the mapped class
   * @throws SQLException if an SQLException is encountered
   * @since 4.0
   */
  protected T constructMappedInstance(ResultSet rs, TypeConverter tc) throws SQLException {
    Assert.state(this.mappedClass != null, "Mapped class was not specified");
    return BeanUtils.instantiateClass(this.mappedClass);
  }

  /**
   * Initialize the given BeanWrapper to be used for row mapping.
   * To be called for each row.
   * <p>The default implementation applies the configured {@link ConversionService},
   * if any. Can be overridden in subclasses.
   *
   * @param bw the BeanWrapper to initialize
   * @see #getConversionService()
   * @see BeanWrapper#setConversionService
   */
  protected void initBeanWrapper(BeanWrapper bw) {
    ConversionService cs = getConversionService();
    if (cs != null) {
      bw.setConversionService(cs);
    }
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
  protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor pd) throws SQLException {
    return JdbcUtils.getResultSetValue(rs, index, pd.getPropertyType());
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
   * @since 4.0
   */
  @Nullable
  protected Object getColumnValue(ResultSet rs, int index, Class<?> paramType) throws SQLException {
    return JdbcUtils.getResultSetValue(rs, index, paramType);
  }

  /**
   * Static factory method to create a new {@code BeanPropertyRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   * @see #newInstance(Class, ConversionService)
   */
  public static <T> BeanPropertyRowMapper<T> newInstance(Class<T> mappedClass) {
    return new BeanPropertyRowMapper<>(mappedClass);
  }

  /**
   * Static factory method to create a new {@code BeanPropertyRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   * @param conversionService the {@link ConversionService} for binding
   * JDBC values to bean properties, or {@code null} for none
   * @see #newInstance(Class)
   * @see #setConversionService
   * @since 4.0
   */
  public static <T> BeanPropertyRowMapper<T> newInstance(
          Class<T> mappedClass, @Nullable ConversionService conversionService) {

    BeanPropertyRowMapper<T> rowMapper = newInstance(mappedClass);
    rowMapper.setConversionService(conversionService);
    return rowMapper;
  }

}
