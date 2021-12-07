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

package cn.taketoday.jdbc.datasource.lookup;

import javax.naming.NamingException;
import javax.sql.DataSource;

import cn.taketoday.jndi.JndiLocatorSupport;

/**
 * JNDI-based {@link DataSourceLookup} implementation.
 *
 * <p>For specific JNDI configuration, it is recommended to configure
 * the "jndiEnvironment"/"jndiTemplate" properties.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @see #setJndiEnvironment
 * @see #setJndiTemplate
 * @since 4.0
 */
public class JndiDataSourceLookup extends JndiLocatorSupport implements DataSourceLookup {

  public JndiDataSourceLookup() {
    setResourceRef(true);
  }

  @Override
  public DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException {
    try {
      return lookup(dataSourceName, DataSource.class);
    }
    catch (NamingException ex) {
      throw new DataSourceLookupFailureException(
              "Failed to look up JNDI DataSource with name '" + dataSourceName + "'", ex);
    }
  }

}
