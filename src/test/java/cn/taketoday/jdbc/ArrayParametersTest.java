package cn.taketoday.jdbc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.junit.Test;

import java.util.ArrayList;

import cn.taketoday.jdbc.parsing.ParameterApplier;

import static junit.framework.TestCase.assertEquals;

public class ArrayParametersTest {

  @Test
  public void testUpdateParameterNamesToIndexes() {
    final ImmutableList<Integer> of = ImmutableList.of(3, 5);
    assertEquals(
            ImmutableMap.of("paramName", ParameterApplier.valueOf(ImmutableList.of(3, 5))),
            ArrayParameters.updateParameterNamesToIndexes(
                    Maps.newHashMap(ImmutableMap.of("paramName", ParameterApplier.valueOf(of))),
                    ImmutableList.of(
                            new ArrayParameters.ArrayParameter(6, 3))));

    assertEquals(
            ImmutableMap.of("paramName", ParameterApplier.valueOf(ImmutableList.of(3, 9))),
            ArrayParameters.updateParameterNamesToIndexes(
                    Maps.newHashMap(ImmutableMap.of("paramName",
                                                    ParameterApplier.valueOf(ImmutableList.of(3, 7)))),
                    ImmutableList.of(
                            new ArrayParameters.ArrayParameter(6, 3))));
  }

  @Test
  public void testComputeNewIndex() {
    assertEquals(
            2,
            ArrayParameters.computeNewIndex(
                    2,
                    ImmutableList.of(
                            new ArrayParameters.ArrayParameter(3, 5))));

    assertEquals(
            3,
            ArrayParameters.computeNewIndex(
                    3,
                    ImmutableList.of(
                            new ArrayParameters.ArrayParameter(3, 5))));

    assertEquals(
            8,
            ArrayParameters.computeNewIndex(
                    4,
                    ImmutableList.of(
                            new ArrayParameters.ArrayParameter(3, 5))));

    assertEquals(
            9,
            ArrayParameters.computeNewIndex(
                    4,
                    ImmutableList.of(
                            new ArrayParameters.ArrayParameter(1, 2),
                            new ArrayParameters.ArrayParameter(3, 5))));

    assertEquals(
            9,
            ArrayParameters.computeNewIndex(
                    4,
                    ImmutableList.of(
                            new ArrayParameters.ArrayParameter(1, 2),
                            new ArrayParameters.ArrayParameter(3, 5),
                            new ArrayParameters.ArrayParameter(4, 5))));

    assertEquals(
            9,
            ArrayParameters.computeNewIndex(
                    4,
                    ImmutableList.of(
                            new ArrayParameters.ArrayParameter(1, 2),
                            new ArrayParameters.ArrayParameter(3, 5),
                            new ArrayParameters.ArrayParameter(5, 5))));
  }

  @Test
  public void testUpdateQueryWithArrayParameters() {
    assertEquals(
            "SELECT * FROM user WHERE id IN(?,?,?,?,?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    ImmutableList.of(new ArrayParameters.ArrayParameter(1, 5))));

    assertEquals(
            "SELECT * FROM user WHERE id IN(?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    new ArrayList<ArrayParameters.ArrayParameter>()));

    assertEquals(
            "SELECT * FROM user WHERE id IN(?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    ImmutableList.of(new ArrayParameters.ArrayParameter(1, 0))));

    assertEquals(
            "SELECT * FROM user WHERE id IN(?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    ImmutableList.of(new ArrayParameters.ArrayParameter(1, 1))));

    assertEquals(
            "SELECT * FROM user WHERE login = ? AND id IN(?,?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE login = ? AND id IN(?)",
                    ImmutableList.of(new ArrayParameters.ArrayParameter(2, 2))));

    assertEquals(
            "SELECT * FROM user WHERE login = ? AND id IN(?,?) AND name = ?",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE login = ? AND id IN(?) AND name = ?",
                    ImmutableList.of(new ArrayParameters.ArrayParameter(2, 2))));

    assertEquals(
            "SELECT ... WHERE other_id IN (?,?,?) login = ? AND id IN(?,?,?) AND name = ?",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT ... WHERE other_id IN (?) login = ? AND id IN(?) AND name = ?",
                    ImmutableList.of(
                            new ArrayParameters.ArrayParameter(1, 3),
                            new ArrayParameters.ArrayParameter(3, 3))));

    assertEquals(
            "SELECT ... WHERE other_id IN (?,?,?,?,?) login = ? AND id IN(?,?,?) AND name = ?",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT ... WHERE other_id IN (?) login = ? AND id IN(?) AND name = ?",
                    ImmutableList.of(
                            new ArrayParameters.ArrayParameter(1, 5),
                            new ArrayParameters.ArrayParameter(3, 3))));
  }

}
