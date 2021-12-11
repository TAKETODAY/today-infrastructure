package cn.taketoday.jdbc.converters;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import cn.taketoday.jdbc.JdbcOperations;
import cn.taketoday.jdbc.PersistenceException;
import cn.taketoday.jdbc.Query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BidirectionalConverterTest {

  private JdbcOperations sql2o;
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
    this.sql2o = new JdbcOperations("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "");
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
    List<String> notConverted = sql2o.createQuery("select text from uuid_wrapper")
            .fetchScalars(String.class);

    // if conversion to database worked, all "-" from UUID were replaced with "!"
    for (String s : notConverted) {
      assertNotNull(UUID.fromString(s.replace('!', '-')));
    }

    List<UUIDWrapper> converted = sql2o.createQuery("select * from uuid_wrapper")
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
    sql2o.createQuery("create table uuid_wrapper(\n" +
            "text varchar(100) primary key)").executeUpdate();

    Query insQuery = sql2o.createQuery("insert into uuid_wrapper(text) values (:text)");
    for (UUIDWrapper wrapper : wrappers) {
      insQuery.addParameter("text", wrapper.getText()).addToBatch();
    }
    insQuery.executeBatch();
  }

  private void deleteTable() {
    try {
      sql2o.createQuery("drop table uuid_wrapper").executeUpdate();
    }
    catch (PersistenceException e) {
      // if it fails, its because the User table doesn't exists. Just ignore this.
    }
  }
}
