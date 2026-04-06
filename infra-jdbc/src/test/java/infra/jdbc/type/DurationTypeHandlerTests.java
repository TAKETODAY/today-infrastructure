package infra.jdbc.type;

import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

import infra.jdbc.type.DurationTypeHandler.StorageFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/4/6 15:04
 */
class DurationTypeHandlerTests {

  // 测试数据
  private static final Duration SAMPLE_DURATION = Duration.ofSeconds(123, 456_789_000); // 123秒 + 456789000纳秒

  private static final long SAMPLE_SECONDS = 123L;
  private static final int SAMPLE_NANOS = 456_789_000;
  private static final long SAMPLE_MILLIS = SAMPLE_SECONDS * 1000 + SAMPLE_NANOS / 1_000_000; // 123456毫秒
  private static final long SAMPLE_NANOS_TOTAL = SAMPLE_SECONDS * 1_000_000_000L + SAMPLE_NANOS; // 123456789000纳秒
  private static final String SAMPLE_ISO_STRING = "PT2M3.456789S"; // 实际是 123.456789秒 => PT2M3.456789S

  // 更精确：Duration.ofSeconds(123, 456789000).toString() => "PT2M3.456789S"
  // 为避免手动拼错，直接使用 Duration 的 toString

  @Test
  void setNonNullParameter_withNanosFormat() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler();
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    handler.setNonNullParameter(preparedStatement, 1, SAMPLE_DURATION);
    verify(preparedStatement).setLong(1, SAMPLE_NANOS_TOTAL);
  }

  @Test
  void setNonNullParameter_withMillisFormat() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.MILLISECONDS);
    handler.setNonNullParameter(preparedStatement, 1, SAMPLE_DURATION);
    verify(preparedStatement).setLong(1, SAMPLE_MILLIS);
  }

  @Test
  void setNonNullParameter_withSecondsFormat() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.SECONDS);
    handler.setNonNullParameter(preparedStatement, 1, SAMPLE_DURATION);
    verify(preparedStatement).setLong(1, SAMPLE_SECONDS);
  }

  @Test
  void setNonNullParameter_withIsoStringFormat() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.ISO_STRING);
    handler.setNonNullParameter(preparedStatement, 1, SAMPLE_DURATION);
    verify(preparedStatement).setString(1, SAMPLE_DURATION.toString());
  }

  @Test
  void getResult_byColumnName_withNanosFormat() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.NANOSECONDS);
    ResultSet resultSet = mock();
    when(resultSet.getLong("duration_col")).thenReturn(SAMPLE_NANOS_TOTAL);
    when(resultSet.wasNull()).thenReturn(false);

    Duration result = handler.getResult(resultSet, "duration_col");
    assertThat(result).isEqualTo(SAMPLE_DURATION);
  }

  @Test
  void getResult_byColumnName_withNanosFormat_null() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.NANOSECONDS);
    ResultSet resultSet = mock();
    when(resultSet.getLong("duration_col")).thenReturn(0L);
    when(resultSet.wasNull()).thenReturn(true);

    Duration result = handler.getResult(resultSet, "duration_col");
    assertThat(result).isNull();
  }

  @Test
  void getResult_byColumnName_withMillisFormat() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.MILLISECONDS);
    ResultSet resultSet = mock();
    when(resultSet.getLong("duration_col")).thenReturn(SAMPLE_MILLIS);
    when(resultSet.wasNull()).thenReturn(false);

    Duration result = handler.getResult(resultSet, "duration_col");
    assertThat(result).isEqualTo(Duration.ofMillis(SAMPLE_MILLIS));
  }

  @Test
  void getResult_byColumnName_withSecondsFormat() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.SECONDS);
    ResultSet resultSet = mock();
    when(resultSet.getLong("duration_col")).thenReturn(SAMPLE_SECONDS);
    when(resultSet.wasNull()).thenReturn(false);

    Duration result = handler.getResult(resultSet, "duration_col");
    assertThat(result).isEqualTo(Duration.ofSeconds(SAMPLE_SECONDS));
  }

  @Test
  void getResult_byColumnName_withIsoStringFormat() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.ISO_STRING);
    String iso = SAMPLE_DURATION.toString();
    ResultSet resultSet = mock(ResultSet.class);

//    when(resultSet.wasNull()).thenReturn(false);
    given(resultSet.getString("duration_col")).willReturn(iso);

    Duration result = handler.getResult(resultSet, "duration_col");
    assertThat(result).isEqualTo(SAMPLE_DURATION);
  }

  @Test
  void getResult_byColumnName_withIsoStringFormat_null() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.ISO_STRING);
    ResultSet resultSet = mock();
    when(resultSet.getString("duration_col")).thenReturn(null);
    // wasNull 被 getString 内部置为 true，这里不需要显式 when

    Duration result = handler.getResult(resultSet, "duration_col");
    assertThat(result).isNull();
  }

  @Test
  void getResult_byColumnIndex_withNanosFormat() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.NANOSECONDS);
    ResultSet resultSet = mock();
    when(resultSet.getLong(1)).thenReturn(SAMPLE_NANOS_TOTAL);
    when(resultSet.wasNull()).thenReturn(false);

    Duration result = handler.getResult(resultSet, 1);
    assertThat(result).isEqualTo(SAMPLE_DURATION);
  }

  @Test
  void getResult_byColumnIndex_withMillisFormat() throws SQLException {
    ResultSet resultSet = mock();
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.MILLISECONDS);
    when(resultSet.getLong(1)).thenReturn(SAMPLE_MILLIS);
    when(resultSet.wasNull()).thenReturn(false);

    Duration result = handler.getResult(resultSet, 1);
    assertThat(result).isEqualTo(Duration.ofMillis(SAMPLE_MILLIS));
  }

  @Test
  void getResult_byColumnIndex_withSecondsFormat() throws SQLException {
    ResultSet resultSet = mock();
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.SECONDS);
    when(resultSet.getLong(1)).thenReturn(SAMPLE_SECONDS);
    when(resultSet.wasNull()).thenReturn(false);

    Duration result = handler.getResult(resultSet, 1);
    assertThat(result).isEqualTo(Duration.ofSeconds(SAMPLE_SECONDS));
  }

  @Test
  void getResult_byColumnIndex_withIsoStringFormat() throws SQLException {
    ResultSet resultSet = mock();
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.ISO_STRING);
    String iso = SAMPLE_DURATION.toString();
    when(resultSet.getString(1)).thenReturn(iso);

    Duration result = handler.getResult(resultSet, 1);
    assertThat(result).isEqualTo(SAMPLE_DURATION);
  }

  @Test
  void getResult_fromCallableStatement_withNanosFormat() throws SQLException {
    CallableStatement callableStatement = mock(CallableStatement.class);
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.NANOSECONDS);
    when(callableStatement.getLong(1)).thenReturn(SAMPLE_NANOS_TOTAL);
    when(callableStatement.wasNull()).thenReturn(false);

    Duration result = handler.getResult(callableStatement, 1);
    assertThat(result).isEqualTo(SAMPLE_DURATION);
  }

  @Test
  void getResult_fromCallableStatement_withMillisFormat() throws SQLException {
    CallableStatement callableStatement = mock(CallableStatement.class);
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.MILLISECONDS);
    when(callableStatement.getLong(1)).thenReturn(SAMPLE_MILLIS);
    when(callableStatement.wasNull()).thenReturn(false);

    Duration result = handler.getResult(callableStatement, 1);
    assertThat(result).isEqualTo(Duration.ofMillis(SAMPLE_MILLIS));
  }

  @Test
  void getResult_fromCallableStatement_withSecondsFormat() throws SQLException {
    CallableStatement callableStatement = mock(CallableStatement.class);
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.SECONDS);
    when(callableStatement.getLong(1)).thenReturn(SAMPLE_SECONDS);
    when(callableStatement.wasNull()).thenReturn(false);

    Duration result = handler.getResult(callableStatement, 1);
    assertThat(result).isEqualTo(Duration.ofSeconds(SAMPLE_SECONDS));
  }

  @Test
  void getResult_fromCallableStatement_withIsoStringFormat() throws SQLException {
    CallableStatement callableStatement = mock(CallableStatement.class);
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.ISO_STRING);
    String iso = SAMPLE_DURATION.toString();
    when(callableStatement.getString(1)).thenReturn(iso);

    Duration result = handler.getResult(callableStatement, 1);
    assertThat(result).isEqualTo(SAMPLE_DURATION);
  }

  @Test
  void getResult_fromCallableStatement_withNull() throws SQLException {
    CallableStatement callableStatement = mock(CallableStatement.class);
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.NANOSECONDS);
    when(callableStatement.getLong(1)).thenReturn(0L);
    when(callableStatement.wasNull()).thenReturn(true);

    Duration result = handler.getResult(callableStatement, 1);
    assertThat(result).isNull();
  }




    @Test
  void constructor_withNullFormat() {
    assertThatThrownBy(() -> new DurationTypeHandler(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("format is required");
  }

  @Test
  void setNonNullParameter_withZeroDuration_nanosFormat() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.NANOSECONDS);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    Duration zeroDuration = Duration.ZERO;

    handler.setNonNullParameter(preparedStatement, 1, zeroDuration);
    verify(preparedStatement).setLong(1, 0L);
  }

  @Test
  void setNonNullParameter_withNegativeDuration_nanosFormat() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.NANOSECONDS);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    Duration negativeDuration = Duration.ofSeconds(-5, -300_000_000);

    handler.setNonNullParameter(preparedStatement, 1, negativeDuration);
    verify(preparedStatement).setLong(1, negativeDuration.toNanos());
  }

  @Test
  void setNonNullParameter_withLargeDuration_millisFormat() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.MILLISECONDS);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    Duration largeDuration = Duration.ofDays(365);

    handler.setNonNullParameter(preparedStatement, 1, largeDuration);
    verify(preparedStatement).setLong(1, largeDuration.toMillis());
  }

  @Test
  void setNonNullParameter_withTruncatedNanos_secondsFormat() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.SECONDS);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    Duration durationWithNanos = Duration.ofSeconds(123, 999_999_999);

    handler.setNonNullParameter(preparedStatement, 1, durationWithNanos);
    verify(preparedStatement).setLong(1, 123L);
  }

  @Test
  void getResult_byColumnName_withZeroNanos() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.NANOSECONDS);
    ResultSet resultSet = mock();
    when(resultSet.getLong("duration_col")).thenReturn(0L);
    when(resultSet.wasNull()).thenReturn(false);

    Duration result = handler.getResult(resultSet, "duration_col");
    assertThat(result).isEqualTo(Duration.ZERO);
  }

  @Test
  void getResult_byColumnName_withNegativeNanos() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.NANOSECONDS);
    ResultSet resultSet = mock();
    long negativeNanos = Duration.ofSeconds(-10).toNanos();
    when(resultSet.getLong("duration_col")).thenReturn(negativeNanos);
    when(resultSet.wasNull()).thenReturn(false);

    Duration result = handler.getResult(resultSet, "duration_col");
    assertThat(result).isEqualTo(Duration.ofNanos(negativeNanos));
  }

  @Test
  void getResult_byColumnName_withMillisFormat_null() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.MILLISECONDS);
    ResultSet resultSet = mock();
    when(resultSet.getLong("duration_col")).thenReturn(0L);
    when(resultSet.wasNull()).thenReturn(true);

    Duration result = handler.getResult(resultSet, "duration_col");
    assertThat(result).isNull();
  }

  @Test
  void getResult_byColumnName_withSecondsFormat_null() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.SECONDS);
    ResultSet resultSet = mock();
    when(resultSet.getLong("duration_col")).thenReturn(0L);
    when(resultSet.wasNull()).thenReturn(true);

    Duration result = handler.getResult(resultSet, "duration_col");
    assertThat(result).isNull();
  }

  @Test
  void getResult_byColumnIndex_withZeroMillis() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.MILLISECONDS);
    ResultSet resultSet = mock();
    when(resultSet.getLong(1)).thenReturn(0L);
    when(resultSet.wasNull()).thenReturn(false);

    Duration result = handler.getResult(resultSet, 1);
    assertThat(result).isEqualTo(Duration.ZERO);
  }

  @Test
  void getResult_byColumnIndex_withNegativeMillis() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.MILLISECONDS);
    ResultSet resultSet = mock();
    when(resultSet.getLong(1)).thenReturn(-5000L);
    when(resultSet.wasNull()).thenReturn(false);

    Duration result = handler.getResult(resultSet, 1);
    assertThat(result).isEqualTo(Duration.ofMillis(-5000L));
  }

  @Test
  void getResult_byColumnIndex_withSecondsFormat_null() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.SECONDS);
    ResultSet resultSet = mock();
    when(resultSet.getLong(1)).thenReturn(0L);
    when(resultSet.wasNull()).thenReturn(true);

    Duration result = handler.getResult(resultSet, 1);
    assertThat(result).isNull();
  }

  @Test
  void getResult_byColumnIndex_withIsoStringFormat_emptyString() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.ISO_STRING);
    ResultSet resultSet = mock();
    when(resultSet.getString(1)).thenReturn("");

    assertThatThrownBy(() -> handler.getResult(resultSet, 1))
        .isInstanceOf(Exception.class);
  }

  @Test
  void getResult_byColumnIndex_withIsoStringFormat_invalidFormat() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.ISO_STRING);
    ResultSet resultSet = mock();
    when(resultSet.getString(1)).thenReturn("invalid-duration");

    assertThatThrownBy(() -> handler.getResult(resultSet, 1))
        .isInstanceOf(Exception.class);
  }

  @Test
  void getResult_fromCallableStatement_withZeroSeconds() throws SQLException {
    CallableStatement callableStatement = mock(CallableStatement.class);
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.SECONDS);
    when(callableStatement.getLong(1)).thenReturn(0L);
    when(callableStatement.wasNull()).thenReturn(false);

    Duration result = handler.getResult(callableStatement, 1);
    assertThat(result).isEqualTo(Duration.ZERO);
  }

  @Test
  void getResult_fromCallableStatement_withNegativeSeconds() throws SQLException {
    CallableStatement callableStatement = mock(CallableStatement.class);
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.SECONDS);
    when(callableStatement.getLong(1)).thenReturn(-100L);
    when(callableStatement.wasNull()).thenReturn(false);

    Duration result = handler.getResult(callableStatement, 1);
    assertThat(result).isEqualTo(Duration.ofSeconds(-100L));
  }

  @Test
  void getResult_fromCallableStatement_withIsoStringFormat_null() throws SQLException {
    CallableStatement callableStatement = mock(CallableStatement.class);
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.ISO_STRING);
    when(callableStatement.getString(1)).thenReturn(null);

    Duration result = handler.getResult(callableStatement, 1);
    assertThat(result).isNull();
  }

  @Test
  void getResult_fromCallableStatement_withIsoStringFormat_valid() throws SQLException {
    CallableStatement callableStatement = mock(CallableStatement.class);
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.ISO_STRING);
    String iso = "PT1H30M";
    when(callableStatement.getString(1)).thenReturn(iso);

    Duration result = handler.getResult(callableStatement, 1);
    assertThat(result).isEqualTo(Duration.ofHours(1).plusMinutes(30));
  }

  @Test
  void roundTrip_withNanosFormat_preservesPrecision() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.NANOSECONDS);
    Duration original = Duration.ofSeconds(123, 456_789_123);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);

    handler.setNonNullParameter(preparedStatement, 1, original);

    ResultSet resultSet = mock();
    when(resultSet.getLong(1)).thenReturn(original.toNanos());
    when(resultSet.wasNull()).thenReturn(false);

    Duration retrieved = handler.getResult(resultSet, 1);
    assertThat(retrieved).isEqualTo(original);
  }

  @Test
  void roundTrip_withMillisFormat_truncatesNanos() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.MILLISECONDS);
    Duration original = Duration.ofSeconds(1, 999_999_999);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);

    handler.setNonNullParameter(preparedStatement, 1, original);

    ResultSet resultSet = mock();
    when(resultSet.getLong(1)).thenReturn(original.toMillis());
    when(resultSet.wasNull()).thenReturn(false);

    Duration retrieved = handler.getResult(resultSet, 1);
    assertThat(retrieved).isEqualTo(Duration.ofMillis(original.toMillis()));
  }

  @Test
  void roundTrip_withSecondsFormat_truncatesMillisAndNanos() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.SECONDS);
    Duration original = Duration.ofSeconds(123, 999_999_999);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);

    handler.setNonNullParameter(preparedStatement, 1, original);

    ResultSet resultSet = mock();
    when(resultSet.getLong(1)).thenReturn(original.getSeconds());
    when(resultSet.wasNull()).thenReturn(false);

    Duration retrieved = handler.getResult(resultSet, 1);
    assertThat(retrieved).isEqualTo(Duration.ofSeconds(original.getSeconds()));
  }

  @Test
  void roundTrip_withIsoStringFormat_preservesPrecision() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler(StorageFormat.ISO_STRING);
    Duration original = Duration.ofSeconds(123, 456_789_123);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);

    handler.setNonNullParameter(preparedStatement, 1, original);

    ResultSet resultSet = mock();
    when(resultSet.getString(1)).thenReturn(original.toString());

    Duration retrieved = handler.getResult(resultSet, 1);
    assertThat(retrieved).isEqualTo(original);
  }

  @Test
  void defaultConstructor_usesNanosFormat() throws SQLException {
    DurationTypeHandler handler = new DurationTypeHandler();
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    Duration duration = Duration.ofSeconds(1, 500_000_000);

    handler.setNonNullParameter(preparedStatement, 1, duration);
    verify(preparedStatement).setLong(1, duration.toNanos());
  }



}