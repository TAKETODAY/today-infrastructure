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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.RandomAccess;

import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.jdbc.annotation.Table;

/**
 * @author TODAY <br>
 *         2019-08-21 21:36
 */
@SuppressWarnings("serial")
public class TableMapping implements RandomAccess, Serializable {

    private ColumnMapping[] array;

    private final String tableName;
    private final Class<?> beanClass;
    private final Map<String, ColumnMapping> properties;

    public TableMapping(Class<?> beanClass) throws Exception {

        this.beanClass = beanClass;
        this.tableName = resolveTableName(beanClass);

        final Map<String, ColumnMapping> properties = new HashMap<>();
        final Collection<Field> fields = ClassUtils.getFields(beanClass);
        for (Field field : fields) {
            properties.put(field.getName(), new ColumnMapping(field));
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

    public ColumnMapping get(int column) {
        return array[column]; // none check
    }

    void add() {

    }

    public String getTableName() {
        return tableName;
    }

}
