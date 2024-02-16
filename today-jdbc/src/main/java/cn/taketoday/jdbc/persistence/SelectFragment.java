/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */


package cn.taketoday.jdbc.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static cn.taketoday.jdbc.persistence.CaseFragment.qualify;

/**
 * A fragment of an SQL <tt>SELECT</tt> clause
 * <p> from hibernate
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Gavin King
 * @since 4.0
 */
public class SelectFragment {

  private String suffix;

  private final ArrayList<String> columns = new ArrayList<>();

  private final ArrayList<String> columnAliases = new ArrayList<>();

  private String extraSelectList;

  private String[] usedAliases;

  public SelectFragment() { }

  public List<String> getColumns() {
    return columns;
  }

  public String getExtraSelectList() {
    return extraSelectList;
  }

  public SelectFragment setUsedAliases(String[] aliases) {
    usedAliases = aliases;
    return this;
  }

  public SelectFragment setExtraSelectList(String extraSelectList) {
    this.extraSelectList = extraSelectList;
    return this;
  }

  public SelectFragment setExtraSelectList(CaseFragment caseFragment, String fragmentAlias) {
    setExtraSelectList(caseFragment.setReturnColumnName(fragmentAlias, suffix).toFragmentString());
    return this;
  }

  public SelectFragment setSuffix(String suffix) {
    this.suffix = suffix;
    return this;
  }

  public SelectFragment addColumn(String columnName) {
    addColumn(null, columnName);
    return this;
  }

  public SelectFragment addColumns(String[] columnNames) {
    for (String columnName : columnNames) {
      addColumn(columnName);
    }
    return this;
  }

  public SelectFragment addColumn(String tableAlias, String columnName) {
    return addColumn(tableAlias, columnName, columnName);
  }

  public SelectFragment addColumn(String tableAlias, String columnName, String columnAlias) {
    columns.add(qualify(tableAlias, columnName));
    //columns.add(columnName);
    //aliases.add(tableAlias);
    columnAliases.add(columnAlias);
    return this;
  }

  public SelectFragment addColumns(String tableAlias, String[] columnNames) {
    for (String columnName : columnNames) {
      addColumn(tableAlias, columnName);
    }
    return this;
  }

  public SelectFragment addColumns(String tableAlias, String[] columnNames, String[] columnAliases) {
    for (int i = 0; i < columnNames.length; i++) {
      if (columnNames[i] != null) {
        addColumn(tableAlias, columnNames[i], columnAliases[i]);
      }
    }
    return this;
  }

  public String toFragmentString() {
    StringBuilder buf = new StringBuilder(columns.size() * 10);
    Iterator<String> iter = columns.iterator();
    Iterator<String> columnAliasIter = columnAliases.iterator();
    //HashMap columnsUnique = new HashMap();
    HashSet<String> columnsUnique = new HashSet<String>();
    if (usedAliases != null) {
      columnsUnique.addAll(Arrays.asList(usedAliases));
    }
    while (iter.hasNext()) {
      String column = iter.next();
      String columnAlias = columnAliasIter.next();
      //TODO: eventually put this back in, once we think all is fixed
      //Object otherAlias = columnsUnique.put(qualifiedColumn, columnAlias);
			/*if ( otherAlias!=null && !columnAlias.equals(otherAlias) ) {
				throw new AssertionFailure("bug in Hibernate SQL alias generation");
			}*/
      if (columnsUnique.add(columnAlias)) {
        buf.append(", ")
                .append(column)
                .append(" as ");
        if (suffix == null) {
          buf.append(columnAlias);
        }
        else {
          buf.append(new Alias(suffix).toAliasString(columnAlias));
        }
      }
    }
    if (extraSelectList != null) {
      buf.append(", ")
              .append(extraSelectList);
    }
    return buf.toString();
  }

}
