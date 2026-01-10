/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.jdbc.converters;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import infra.jdbc.NamedQuery;
import infra.jdbc.PersistenceException;
import infra.jdbc.RepositoryManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BidirectionalConverterTests {

  private RepositoryManager sql2o;
  private List<UUIDWrapper> wrappers;

  static class UUIDWrapperComparator implements Comparator<UUIDWrapper> {
    @Override
    public int compare(UUIDWrapper o1, UUIDWrapper o2) {
      final UUID text = o1.getText();
      return text.compareTo(o2.getText());
    }
  }

  UUIDWrapperComparator comparator = new UUIDWrapperComparator();

  @BeforeEach
  public void setUp() {
    this.sql2o = new RepositoryManager("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "");
    this.wrappers = randomWrappers();

    wrappers.sort(comparator);
    this.createAndFillTable(this.wrappers);
  }

  @AfterEach
  public void tearDown() {
    deleteTable();
  }

  @Test
  public void toDatabase_fromDatabase_doExecute() {
    List<String> notConverted = sql2o.createNamedQuery("select text from uuid_wrapper")
            .scalars(String.class);

    // if conversion to database worked, all "-" from UUID were replaced with "!"
    for (String s : notConverted) {
      assertNotNull(UUID.fromString(s.replace('!', '-')));
    }

    List<UUIDWrapper> converted = sql2o.createNamedQuery("select * from uuid_wrapper")
            .fetch(UUIDWrapper.class);

    converted.sort(comparator);
    // if conversion from database worked, should have the list we inserted
    assertEquals(wrappers, converted);
  }

  /************** Helper stuff ******************/

  private List<UUIDWrapper> randomWrappers() {
    List<UUIDWrapper> wrappers = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      wrappers.add(new UUIDWrapper(UUID.randomUUID()));
    }
    return wrappers;
  }

  private void createAndFillTable(List<UUIDWrapper> wrappers) {
    sql2o.createNamedQuery("create table uuid_wrapper(\n" +
            "text varchar(100) primary key)").executeUpdate();

    NamedQuery insQuery = sql2o.createNamedQuery("insert into uuid_wrapper(text) values (:text)");
    for (UUIDWrapper wrapper : wrappers) {
      insQuery.addParameter("text", wrapper.getText()).addToBatch();
    }
    insQuery.executeBatch();
  }

  private void deleteTable() {
    try {
      sql2o.createNamedQuery("drop table uuid_wrapper").executeUpdate();
    }
    catch (PersistenceException e) {
      // if it fails, its because the User table doesn't exists. Just ignore this.
    }
  }
}
