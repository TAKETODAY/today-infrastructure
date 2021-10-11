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
package cn.taketoday.orm.hibernate5;

import org.hibernate.Filter;

import java.util.Iterator;

import javax.persistence.PersistenceException;

/**
 * @author TODAY <br>
 * 2018-09-15 15:24
 */
public interface HibernateOperations<T> extends HibernateUpdateOperations<T>, HibernateQueryOperations<T> {

  void flush() throws PersistenceException;

  void clear() throws PersistenceException;

  void closeIterator(Iterator<?> it) throws PersistenceException;

  Filter enableFilter(String filterName) throws IllegalStateException;

  <R> R execute(HibernateCallback<R> session) throws PersistenceException;

}
