package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.HeaderStructureAudit;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;
import com.looksee.services.UXIssueMessageService;

public class HeaderStructureAuditExecuteTest2 {

    private HeaderStructureAudit audit;
    private AuditService mockAuditService;
    private UXIssueMessageService mockIssueService;
    private ElementStateService mockElementStateService;
    private AtomicLong idCounter;

    @BeforeEach
    void setUp() throws Exception {
        audit = new HeaderStructureAudit();
        mockAuditService = mock(AuditService.class);
        mockIssueService = mock(UXIssueMessageService.class);
        mockElementStateService = mock(ElementStateService.class);
        idCounter = new AtomicLong(1000L);

        Field f1 = HeaderStructureAudit.class.getDeclaredField("auditService");
        f1.setAccessible(true);
        f1.set(audit, mockAuditService);

        Field f2 = HeaderStructureAudit.class.getDeclaredField("issueMessageService");
        f2.setAccessible(true);
        f2.set(audit, mockIssueService);

        Field f3 = HeaderStructureAudit.class.getDeclaredField("elementStateService");
        f3.setAccessible(true);
        f3.set(audit, mockElementStateService);

        when(mockIssueService.save(any(UXIssueMessage.class))).thenAnswer(inv -> {
            UXIssueMessage msg = inv.getArgument(0);
            msg.setId(idCounter.getAndIncrement());
            return msg;
        });
        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PageState createPageState(String html) {
        PageState ps = mock(PageState.class);
        when(ps.getId()).thenReturn(100L);
        when(ps.getUrl()).thenReturn("https://example.com");
        when(ps.getSrc()).thenReturn(html);
        return ps;
    }

    @Test
    void testExecute_noHeaders() {
        PageState ps = createPageState("<html><body><p>No headers here</p></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("H1 level header not found")));
        assertEquals(0, result.getPoints());
    }

    @Test
    void testExecute_exactlyOneH1() {
        PageState ps = createPageState("<html><body><h1>Main Title</h1><p>Content</p></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("exactly 1 H1 header")));
    }

    @Test
    void testExecute_multipleH1s() {
        PageState ps = createPageState("<html><body><h1>Title 1</h1><h1>Title 2</h1></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("Too many H1 level headers")));
    }

    @Test
    void testExecute_properHierarchyNoOutOfOrder() {
        // No skipped levels, so findOutOfOrderHeaders returns empty list - no BrowserService needed
        PageState ps = createPageState(
                "<html><body><h1>Title</h1><h2>Section</h2><h3>Subsection</h3></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("exactly 1 H1 header")));
        assertFalse(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("not in hierarchical order")));
    }

    @Test
    void testExecute_emptyBody() {
        PageState ps = createPageState("<html><body></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        // No H1 found
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("H1 level header not found")));
    }

    @Test
    void testExecute_h1AndH2Only() {
        PageState ps = createPageState(
                "<html><body><h1>Title</h1><h2>Section 1</h2><h2>Section 2</h2></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        // Proper hierarchy, exactly 1 H1
        assertEquals(2, result.getPoints());
        assertEquals(2, result.getTotalPossiblePoints());
    }
}
