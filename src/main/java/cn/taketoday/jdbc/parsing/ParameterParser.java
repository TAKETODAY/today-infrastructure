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

package cn.taketoday.jdbc.parsing;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by lars on 22.09.2014.
 */
public class ParameterParser extends CharParser {

  private final Map<String, QueryParameter> parameterMap;
  int paramIdx = 1;

  public ParameterParser(Map<String, QueryParameter> parameterMap) {
    this.parameterMap = parameterMap;
  }

  @Override
  public boolean supports(char c, String sql, int idx) {
    return sql.length() > idx + 1
            && c == ':'
            && Character.isJavaIdentifierStart(sql.charAt(idx + 1))
            && sql.charAt(idx - 1) != ':';
  }

  @Override
  public int parse(char c, int idx, StringBuilder parsedSql, String sql, int length) {
    int startIdx = idx;
    idx += 1;

    while (idx + 1 < length && Character.isJavaIdentifierPart(sql.charAt(idx + 1))) {
      idx += 1;
    }

    String name = sql.substring(startIdx + 1, idx + 1);
    QueryParameter queryParameter = parameterMap.get(name);

    if (queryParameter == null) {
      queryParameter = new QueryParameter(name, ParameterIndexHolder.valueOf(paramIdx));
      parameterMap.put(name, queryParameter);
    }
    else {
      // set ParameterApplier
      ParameterIndexHolder indexHolder = queryParameter.getHolder();
      if (indexHolder == null) {
        indexHolder = ParameterIndexHolder.valueOf(paramIdx);
        queryParameter.setHolder(indexHolder);
      }
      else if (indexHolder instanceof ListParameterIndexApplier) {
        ((ListParameterIndexApplier) indexHolder).addIndex(paramIdx);
      }
      else if (indexHolder instanceof DefaultParameterIndexHolder) {
        ArrayList<Integer> indices = new ArrayList<>();
        final int index = ((DefaultParameterIndexHolder) indexHolder).getIndex();
        indices.add(index);
        indices.add(paramIdx);
        queryParameter.setHolder(ParameterIndexHolder.valueOf(indices));
      }
    }

    paramIdx++;
    parsedSql.append('?');
    return idx;
  }
}
