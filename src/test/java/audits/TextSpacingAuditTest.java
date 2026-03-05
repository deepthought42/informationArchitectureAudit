package audits;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.looksee.audit.informationArchitecture.audits.TextSpacingAudit;
import com.looksee.models.ElementState;
import com.looksee.models.audit.messages.UXIssueMessage;

public class TextSpacingAuditTest {

    @Test
    void testParseCssValueSupportsKnownUnitsAndInvalidInput() {
        assertEquals(16.0, TextSpacingAudit.parseCssValue("16px"));
        assertEquals(32.0, TextSpacingAudit.parseCssValue("2em"));
        assertEquals(16.0, TextSpacingAudit.parseCssValue("100%"));
        assertEquals(0.0, TextSpacingAudit.parseCssValue(""));
        assertEquals(0.0, TextSpacingAudit.parseCssValue("not-a-value"));
    }

    @Test
    void testEvaluateTextSpacingReturnsIssuesWhenThresholdsNotMet() {
        ElementState element = Mockito.mock(ElementState.class);
        Map<String, String> css = new HashMap<>();
        css.put("font-size", "16px");
        css.put("line-height", "20px");
        css.put("letter-spacing", "1px");
        css.put("word-spacing", "2px");
        css.put("margin-bottom", "10px");
        Mockito.when(element.getRenderedCssValues()).thenReturn(css);

        List<UXIssueMessage> issues = TextSpacingAudit.evaluateTextSpacing(Collections.singletonList(element));

        assertEquals(4, issues.size());
    }

    @Test
    void testEvaluateTextSpacingReturnsNoIssuesWhenThresholdsMet() {
        ElementState element = Mockito.mock(ElementState.class);
        Map<String, String> css = new HashMap<>();
        css.put("font-size", "16px");
        css.put("line-height", "24px");
        css.put("letter-spacing", "2px");
        css.put("word-spacing", "3px");
        css.put("margin-bottom", "32px");
        Mockito.when(element.getRenderedCssValues()).thenReturn(css);

        List<UXIssueMessage> issues = TextSpacingAudit.evaluateTextSpacing(Collections.singletonList(element));

        assertEquals(0, issues.size());
    }
}
