/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.jdbc.mapping;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.RandomAccess;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.jdbc.DefaultFieldColumnConverter;
import cn.taketoday.jdbc.FieldColumnConverter;
import cn.taketoday.jdbc.annotation.Table;
import cn.taketoday.jdbc.annotation.Transient;

/**
 * @author TODAY <br>
 *         2019-08-21 21:36
 */
@SuppressWarnings("serial")
public class TableMapping implements RandomAccess, Serializable {

    private final String tableName;
    private final Class<?> beanClass;
    private final Map<String, ColumnMapping> properties;

    private static FieldColumnConverter fieldColumnConverter = new DefaultFieldColumnConverter();

    public TableMapping(Class<?> beanClass) throws ConfigurationException {

        this.beanClass = beanClass;
        this.tableName = resolveTableName(beanClass);

        final Map<String, ColumnMapping> properties = new HashMap<>();

        final FieldColumnConverter fieldColumnConverter = getFieldColumnConverter();
        for (final Field field : ClassUtils.getFields(beanClass)) {
            if (!field.isAnnotationPresent(Transient.class)) {
                final ColumnMapping columnMapping = new ColumnMapping(field,fieldColumnConverter);
                properties.put(columnMapping.getColumn(), columnMapping);
            }
        }
        this.properties = properties;
    }

    public static String resolveTableName(Class<?> beanClass) {
        String name = null;
        final Table table = ClassUtils.getAnnotation(Table.class, beanClass);
        if (table != null) {
            name = table.value();
        }
        if (StringUtils.isEmpty(name)) {
            return beanClass.getSimpleName().toLowerCase(); // 小写
        }
        return name;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public Map<String, ColumnMapping> getProperties() {
        return properties;
    }

    public ColumnMapping get(String column) {
        return properties.get(column);
    }

    public String getTableName() {
        return tableName;
    }

    public static FieldColumnConverter getFieldColumnConverter() {
        return fieldColumnConverter;
    }

    public static void setFieldColumnConverter(FieldColumnConverter fieldColumnConverter) {
        TableMapping.fieldColumnConverter = fieldColumnConverter;
    }

}
