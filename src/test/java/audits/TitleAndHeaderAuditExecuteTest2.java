package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;

import com.looksee.audit.informationArchitecture.audits.TitleAndHeaderAudit;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.BrowserUtils;

public class TitleAndHeaderAuditExecuteTest2 {

    private TitleAndHeaderAudit audit;
    private AuditService mockAuditService;
    private UXIssueMessageService mockIssueService;
    private AtomicLong idCounter;

    @BeforeEach
    void setUp() throws Exception {
        audit = new TitleAndHeaderAudit();
        mockAuditService = mock(AuditService.class);
        mockIssueService = mock(UXIssueMessageService.class);
        idCounter = new AtomicLong(1000L);

        Field f1 = TitleAndHeaderAudit.class.getDeclaredField("audit_service");
        f1.setAccessible(true);
        f1.set(audit, mockAuditService);

        Field f2 = TitleAndHeaderAudit.class.getDeclaredField("issue_message_service");
        f2.setAccessible(true);
        f2.set(audit, mockIssueService);

        when(mockIssueService.save(any(UXIssueMessage.class))).thenAnswer(inv -> {
            UXIssueMessage msg = inv.getArgument(0);
            msg.setId(idCounter.getAndIncrement());
            return msg;
        });
        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Object browserUtilsWithTitle(InvocationOnMock inv) {
        String methodName = inv.getMethod().getName();
        switch (methodName) {
            case "getTitle": return "My Page Title";
            case "sanitizeUrl": return "https://example.com";
            case "formatUrl": return "https://example.com";
            case "isJavascript": return false;
            case "doesUrlExist": return true;
            default: return null;
        }
    }

    private static Object browserUtilsNoTitle(InvocationOnMock inv) {
        String methodName = inv.getMethod().getName();
        switch (methodName) {
            case "getTitle": return "";
            case "sanitizeUrl": return "https://example.com";
            case "formatUrl": return "https://example.com";
            case "isJavascript": return false;
            case "doesUrlExist": return true;
            default: return null;
        }
    }

    private static Object browserUtilsNullTitle(InvocationOnMock inv) {
        String methodName = inv.getMethod().getName();
        switch (methodName) {
            case "getTitle": return null;
            case "sanitizeUrl": return "https://example.com";
            default: return null;
        }
    }

    private PageState createPageState(String html) {
        PageState ps = mock(PageState.class);
        when(ps.getId()).thenReturn(100L);
        when(ps.getUrl()).thenReturn("https://example.com");
        when(ps.getSrc()).thenReturn(html);
        return ps;
    }

    @Test
    void testExecute_withTitleAndFavicon() {
        PageState ps = createPageState(
                "<html><head><link rel=\"icon\" href=\"/favicon.ico\"></head><body><h1>Title</h1><p>Content</p></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                TitleAndHeaderAuditExecuteTest2::browserUtilsWithTitle)) {
            Audit result = audit.execute(ps, ar, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("Page has a title")));
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("Favicon is present")));
        }
    }

    @Test
    void testExecute_noTitleNoFavicon() {
        PageState ps = createPageState(
                "<html><head></head><body><p>Content</p></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                TitleAndHeaderAuditExecuteTest2::browserUtilsNoTitle)) {
            Audit result = audit.execute(ps, ar, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("missing a title")));
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("favicon is missing")));
        }
    }

    @Test
    void testExecute_nullTitle() {
        PageState ps = createPageState(
                "<html><head><link rel=\"icon\" href=\"/fav.ico\"></head><body></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                TitleAndHeaderAuditExecuteTest2::browserUtilsNullTitle)) {
            Audit result = audit.execute(ps, ar, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("missing a title")));
        }
    }

    @Test
    void testExecute_withHeadingsAndTextContent() {
        // Text following a header sibling should get scored
        PageState ps = createPageState(
                "<html><head><link rel=\"icon\" href=\"/fav.ico\"></head>" +
                "<body><h1>Main</h1><div><h2>Section</h2><div>Some text content here</div></div></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                TitleAndHeaderAuditExecuteTest2::browserUtilsWithTitle)) {
            Audit result = audit.execute(ps, ar, null);

            assertNotNull(result);
            // Just verify it completes without error and produces results
            assertFalse(result.getMessages().isEmpty());
        }
    }

    @Test
    void testExecute_withOrderedList() {
        PageState ps = createPageState(
                "<html><head><link rel=\"icon\" href=\"/fav.ico\"></head>" +
                "<body><h1>Title</h1><ol><li>Item 1</li><li>Item 2</li></ol><p>Some text</p></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                TitleAndHeaderAuditExecuteTest2::browserUtilsWithTitle)) {
            Audit result = audit.execute(ps, ar, null);

            assertNotNull(result);
            assertFalse(result.getMessages().isEmpty());
        }
    }

    @Test
    void testExecute_textWithHeaderSiblingBefore() {
        // scoreTextElementHeaders: text element with header as earlier sibling = 3 pts
        PageState ps = createPageState(
                "<html><head></head><body>" +
                "<h2>Section Header</h2>" +
                "<div>Text block following header</div>" +
                "</body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                TitleAndHeaderAuditExecuteTest2::browserUtilsWithTitle)) {
            Audit result = audit.execute(ps, ar, null);

            assertNotNull(result);
            assertFalse(result.getMessages().isEmpty());
        }
    }

    @Test
    void testExecute_textWithHeaderSiblingAfter() {
        // scoreTextElementHeaders: text element with header as later sibling = 1 pt
        PageState ps = createPageState(
                "<html><head></head><body>" +
                "<div>Text block before header</div>" +
                "<h2>Section Header</h2>" +
                "</body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                TitleAndHeaderAuditExecuteTest2::browserUtilsWithTitle)) {
            Audit result = audit.execute(ps, ar, null);

            assertNotNull(result);
        }
    }
}
