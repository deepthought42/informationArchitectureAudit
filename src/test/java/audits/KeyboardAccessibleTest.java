package audits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.looksee.audit.informationArchitecture.models.KeyboardAccessibleAudit;
import com.looksee.models.ElementState;

public class KeyboardAccessibleTest {

    @Test
    void testAllElementsAccessibleWhenTabindexOrRolePresent() {
        ElementState tabindexElement = Mockito.mock(ElementState.class);
        Mockito.when(tabindexElement.getAttribute("tabindex")).thenReturn("0");
        Mockito.when(tabindexElement.getAttribute("role")).thenReturn("");

        ElementState roleElement = Mockito.mock(ElementState.class);
        Mockito.when(roleElement.getAttribute("tabindex")).thenReturn("");
        Mockito.when(roleElement.getAttribute("role")).thenReturn("button");

        List<ElementState> result = KeyboardAccessibleAudit.checkKeyboardAccessibility(
            Arrays.asList(tabindexElement, roleElement)
        );

        assertTrue(result.isEmpty(), "Elements with tabindex or role should be considered accessible");
    }

    @Test
    void testReturnsOnlyNonAccessibleElements() {
        ElementState accessible = Mockito.mock(ElementState.class);
        Mockito.when(accessible.getAttribute("tabindex")).thenReturn("0");
        Mockito.when(accessible.getAttribute("role")).thenReturn(null);

        ElementState inaccessible = Mockito.mock(ElementState.class);
        Mockito.when(inaccessible.getAttribute("tabindex")).thenReturn(null);
        Mockito.when(inaccessible.getAttribute("role")).thenReturn(null);

        List<ElementState> result = KeyboardAccessibleAudit.checkKeyboardAccessibility(
            Arrays.asList(accessible, inaccessible)
        );

        assertEquals(1, result.size());
        assertEquals(inaccessible, result.get(0));
    }
}
