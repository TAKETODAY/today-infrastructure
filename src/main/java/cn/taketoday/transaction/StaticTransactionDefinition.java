/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.transaction;

/**
 * A static unmodifiable transaction definition.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/11/5 15:00
 * @see TransactionDefinition#withDefaults()
 * @since 4.0
 */
final class StaticTransactionDefinition implements TransactionDefinition {

  static final StaticTransactionDefinition INSTANCE = new StaticTransactionDefinition();

  private StaticTransactionDefinition() { }

}
