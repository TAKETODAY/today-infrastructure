/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.jdbc;

/**
 * @author TODAY <br>
 *         2018-08-28 17:57
 */
public interface Constant extends cn.taketoday.context.Constant {

  String VERSION = "1.0.0.RELEASE";

  /**
   ********************************************
   * ON DELETE or ON UPDATE value
   */
  String CASCADE = "CASCADE";
  String RESTRICT = "RESTRICT";
  String SET_NULL = "SET NULL";
  String NO_ACTION = "NO ACTION";

  /**
   ********************************************
   * timestamp
   */
  String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

  /**
   ********************************************
   * FETCH
   *
   */
  String FETCH_JOIN = "join";
  String FETCH_SELECT = "select";
  String FETCH_SUB_SELECT = "subselect";

}
