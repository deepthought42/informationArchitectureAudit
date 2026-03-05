package audits;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.SecurityAudit;

public class SecurityAuditTest {

    @Test
    void testMakeDistinctSortsAndRemovesDuplicates() {
        List<String> input = Arrays.asList("beta", "alpha", "beta", "gamma", "alpha");

        List<String> result = SecurityAudit.makeDistinct(input);

        assertEquals(Arrays.asList("alpha", "beta", "gamma"), result);
    }

    @Test
    void testMakeDistinctHandlesEmptyInput() {
        assertEquals(Collections.emptyList(), SecurityAudit.makeDistinct(Collections.emptyList()));
    }
}
