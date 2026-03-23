package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.TableStructureAudit;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;

public class TableStructureAuditExecuteTest2 {

    private TableStructureAudit audit;
    private AuditService mockAuditService;
    private ElementStateService mockElementStateService;

    @BeforeEach
    void setUp() throws Exception {
        audit = new TableStructureAudit();
        mockAuditService = mock(AuditService.class);
        mockElementStateService = mock(ElementStateService.class);

        Field f1 = TableStructureAudit.class.getDeclaredField("auditService");
        f1.setAccessible(true);
        f1.set(audit, mockAuditService);

        Field f2 = TableStructureAudit.class.getDeclaredField("elementStateService");
        f2.setAccessible(true);
        f2.set(audit, mockElementStateService);

        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void testExecute_noTables() {
        PageState ps = mock(PageState.class);
        when(ps.getId()).thenReturn(100L);
        when(ps.getUrl()).thenReturn("https://example.com");
        when(ps.getSrc()).thenReturn("<html><body><p>No tables here</p></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
        assertEquals(0, result.getPoints());
    }

    @Test
    void testExecute_emptyBody() {
        PageState ps = mock(PageState.class);
        when(ps.getId()).thenReturn(100L);
        when(ps.getUrl()).thenReturn("https://example.com");
        when(ps.getSrc()).thenReturn("<html><body></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void testConstructor() {
        TableStructureAudit a = new TableStructureAudit();
        assertNotNull(a);
    }
}
