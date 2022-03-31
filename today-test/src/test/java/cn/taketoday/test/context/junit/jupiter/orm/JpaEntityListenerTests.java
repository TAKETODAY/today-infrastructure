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

package cn.taketoday.test.context.junit.jupiter.orm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.orm.jpa.JpaTransactionManager;
import cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean;
import cn.taketoday.orm.jpa.vendor.Database;
import cn.taketoday.orm.jpa.vendor.HibernateJpaVendorAdapter;
import cn.taketoday.test.context.jdbc.Sql;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;
import cn.taketoday.test.context.junit.jupiter.orm.domain.JpaPersonRepository;
import cn.taketoday.test.context.junit.jupiter.orm.domain.Person;
import cn.taketoday.test.context.junit.jupiter.orm.domain.PersonListener;
import cn.taketoday.test.context.junit.jupiter.orm.domain.PersonRepository;
import cn.taketoday.test.context.junit4.orm.HibernateSessionFlushingTests;
import cn.taketoday.transaction.annotation.EnableTransactionManagement;
import cn.taketoday.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Transactional tests for JPA entity listener support (a.k.a. lifecycle callback
 * methods).
 *
 * @author Sam Brannen
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/28228">issue gh-28228</a>
 * @see HibernateSessionFlushingTests
 */
@JUnitConfig
@Transactional
@Sql(statements = "insert into person(id, name) values(0, 'Jane')")
class JpaEntityListenerTests {

  @PersistenceContext
  EntityManager entityManager;

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Autowired
  PersonRepository repo;

  @BeforeEach
  void setUp() {
    assertPeople("Jane");
    PersonListener.methodsInvoked.clear();
  }

  @Test
  void find() {
    Person jane = repo.findByName("Jane");
    assertCallbacks("@PostLoad: Jane");

    // Does not cause an additional @PostLoad
    repo.findById(jane.getId());
    assertCallbacks("@PostLoad: Jane");

    // Clear to cause a new @PostLoad
    entityManager.clear();
    repo.findById(jane.getId());
    assertCallbacks("@PostLoad: Jane", "@PostLoad: Jane");
  }

  @Test
  void save() {
    Person john = repo.save(new Person("John"));
    assertCallbacks("@PrePersist: John");

    // Flush to cause a @PostPersist
    entityManager.flush();
    assertPeople("Jane", "John");
    assertCallbacks("@PrePersist: John", "@PostPersist: John");

    // Does not cause a @PostLoad
    repo.findById(john.getId());
    assertCallbacks("@PrePersist: John", "@PostPersist: John");

    // Clear to cause a @PostLoad
    entityManager.clear();
    repo.findById(john.getId());
    assertCallbacks("@PrePersist: John", "@PostPersist: John", "@PostLoad: John");
  }

  @Test
  void update() {
    Person jane = repo.findByName("Jane");
    assertCallbacks("@PostLoad: Jane");

    jane.setName("Jane Doe");
    // Does not cause a @PreUpdate or @PostUpdate
    repo.save(jane);
    assertCallbacks("@PostLoad: Jane");

    // Flush to cause a @PreUpdate and @PostUpdate
    entityManager.flush();
    assertPeople("Jane Doe");
    assertCallbacks("@PostLoad: Jane", "@PreUpdate: Jane Doe", "@PostUpdate: Jane Doe");
  }

  @Test
  void remove() {
    Person jane = repo.findByName("Jane");
    assertCallbacks("@PostLoad: Jane");

    // Does not cause a @PostRemove
    repo.remove(jane);
    assertCallbacks("@PostLoad: Jane", "@PreRemove: Jane");

    // Flush to cause a @PostRemove
    entityManager.flush();
    assertPeople();
    assertCallbacks("@PostLoad: Jane", "@PreRemove: Jane", "@PostRemove: Jane");
  }

  private void assertCallbacks(String... callbacks) {
    assertThat(PersonListener.methodsInvoked).containsExactly(callbacks);
  }

  private void assertPeople(String... expectedNames) {
    List<String> names = this.jdbcTemplate.queryForList("select name from person", String.class);
    if (expectedNames.length == 0) {
      assertThat(names).isEmpty();
    }
    else {
      assertThat(names).containsExactlyInAnyOrder(expectedNames);
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableTransactionManagement
  static class Config {

    @Bean
    PersonRepository personRepository() {
      return new JpaPersonRepository();
    }

    @Bean
    DataSource dataSource() {
      return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
      return new JdbcTemplate(dataSource);
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
      LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
      emfb.setDataSource(dataSource);
      emfb.setPackagesToScan(Person.class.getPackage().getName());
      HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
      hibernateJpaVendorAdapter.setGenerateDdl(true);
      hibernateJpaVendorAdapter.setDatabase(Database.HSQL);
      emfb.setJpaVendorAdapter(hibernateJpaVendorAdapter);
      return emfb;
    }

    @Bean
    JpaTransactionManager transactionManager(EntityManagerFactory emf) {
      return new JpaTransactionManager(emf);
    }

  }

}
