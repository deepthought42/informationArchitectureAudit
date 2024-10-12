package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import com.looksee.audit.informationArchitecture.models.ElementState;
import com.looksee.audit.informationArchitecture.models.KeyboardAccessibleAudit;
import com.looksee.audit.informationArchitecture.models.PageState;

public class KeyboardAccessibleTest {
    private List<ElementState> elements;
    private ElementState mockElement;
    private PageState mockPage;

    @BeforeEach
    void setUp() {
        mockElement = Mockito.mock(ElementState.class);
        elements = new ArrayList<>();
        elements.add(mockElement);
        elements.add(Mockito.mock(ElementState.class));
        mockPage = Mockito.mock(PageState.class);
    }

    //@Test
    void testElementWithTabindexIsAccessible() {
        Mockito.when(mockElement.getAttribute("tabindex")).thenReturn("0");
        Mockito.when(mockElement.getAttribute("role")).thenReturn(null);
        
        assertTrue(KeyboardAccessibleAudit.checkKeyboardAccessibility(elements).isEmpty(), "Element with tabindex='0' should be accessible");
    }

    //@Test
    void testElementWithButtonRoleIsAccessible() {
        Mockito.when(mockElement.getAttribute("tabindex")).thenReturn(null);
        Mockito.when(mockElement.getAttribute("role")).thenReturn("button");

        assertTrue(KeyboardAccessibleAudit.checkKeyboardAccessibility(elements).isEmpty(), "Element with role='button' should be accessible");
    }

    //@Test
    void testElementWithNoAttributesIsNotAccessible() {
        Mockito.when(mockElement.getAttribute("tabindex")).thenReturn(null);
        Mockito.when(mockElement.getAttribute("role")).thenReturn(null);

        assertFalse(KeyboardAccessibleAudit.checkKeyboardAccessibility(elements).isEmpty(), "Element with no attributes should not be accessible");
    }

    //@Test
    void testElementWithNonZeroTabindexIsNotAccessible() {
        Mockito.when(mockElement.getAttribute("tabindex")).thenReturn("1");
        Mockito.when(mockElement.getAttribute("role")).thenReturn(null);

        assertFalse(!KeyboardAccessibleAudit.checkKeyboardAccessibility(elements).isEmpty(), "Element with tabindex not equal to 0 should not be accessible");
    }
}
