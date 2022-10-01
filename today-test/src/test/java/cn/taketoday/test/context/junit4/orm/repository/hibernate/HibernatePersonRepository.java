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

package cn.taketoday.test.context.junit4.orm.repository.hibernate;

import org.hibernate.SessionFactory;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.stereotype.Repository;
import cn.taketoday.test.context.junit4.orm.domain.Person;
import cn.taketoday.test.context.junit4.orm.repository.PersonRepository;

/**
 * Hibernate implementation of the {@link PersonRepository} API.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Repository
public class HibernatePersonRepository implements PersonRepository {

  private final SessionFactory sessionFactory;

  @Autowired
  public HibernatePersonRepository(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Person save(Person person) {
    this.sessionFactory.getCurrentSession().save(person);
    return person;
  }

  @Override
  public Person findByName(String name) {
    return (Person) this.sessionFactory.getCurrentSession().createQuery(
            "from JpaPerson person where person.name = :name").setParameter("name", name).getSingleResult();
  }

}
