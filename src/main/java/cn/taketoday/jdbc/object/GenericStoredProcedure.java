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

package cn.taketoday.jdbc.object;

/**
 * Concrete implementation making it possible to define the RDBMS stored procedures
 * in an application context without writing a custom Java implementation class.
 * <p>
 * This implementation does not provide a typed method for invocation so executions
 * must use one of the generic {@link StoredProcedure#execute(java.util.Map)} or
 * {@link StoredProcedure#execute(cn.taketoday.jdbc.core.ParameterMapper)} methods.
 *
 * @author Thomas Risberg
 * @see cn.taketoday.jdbc.object.StoredProcedure
 */
public class GenericStoredProcedure extends StoredProcedure {

}
