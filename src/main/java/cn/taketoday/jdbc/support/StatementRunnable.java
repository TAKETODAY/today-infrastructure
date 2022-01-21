/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.jdbc.support;

import cn.taketoday.jdbc.JdbcConnection;
import cn.taketoday.jdbc.JdbcOperations;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;

/**
 * Represents a method with a {@link JdbcConnection} and an optional argument.
 * Implementations of this interface be used as a parameter to one of the
 * {@link JdbcOperations#runInTransaction(StatementRunnable) Sql2o.runInTransaction}
 * overloads, to run code safely in a transaction.
 */
public interface StatementRunnable {

  void run(@NonNull JdbcConnection connection, @Nullable Object argument) throws Throwable;
}
