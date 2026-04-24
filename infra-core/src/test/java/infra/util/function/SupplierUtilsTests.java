package infra.util.function;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/4/24 18:34
 */
class SupplierUtilsTests {

  @Test
  void empty() {
    assertThat(SupplierUtils.empty()).isSameAs(SupplierUtils.empty());
    assertThat(SupplierUtils.empty()).isSameAs(SupplierUtils.always(null));

    assertThat(SupplierUtils.empty().get()).isEqualTo(SupplierUtils.always(null).get());

    assertThat(SupplierUtils.always("").get()).isEqualTo(SupplierUtils.always("").get());
  }

}