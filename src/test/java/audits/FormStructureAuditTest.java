package audits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.FormStructureAudit;
import com.looksee.models.audit.GenericIssue;

public class FormStructureAuditTest {

    @Test
    void testValidateFormFlagsControlWithoutLabelOrAria() {
        Document doc = Jsoup.parse("<form><input id='name' type='text'/></form>");
        Element form = doc.selectFirst("form");

        List<GenericIssue> issues = FormStructureAudit.validateForm(form);

        assertEquals(1, issues.size());
        assertEquals("Form control is missing label", issues.get(0).getTitle());
    }

    @Test
    void testValidateFormAcceptsLabelAndAria() {
        Document doc = Jsoup.parse("<form>"
            + "<label for='email'>Email</label><input id='email' type='email'/>"
            + "<input id='search' aria-label='Search'/>"
            + "</form>");
        Element form = doc.selectFirst("form");

        List<GenericIssue> issues = FormStructureAudit.validateForm(form);

        assertEquals(2, issues.size());
        assertTrue(issues.stream().allMatch(issue -> issue.getRecommendation().isEmpty()));
    }

    @Test
    void testValidateFieldsetGroupingDetectsMissingFieldsetAndLooseControls() {
        Document doc = Jsoup.parse("<form><input id='x'/></form>");
        Element form = doc.selectFirst("form");

        List<String> messages = FormStructureAudit.validateFieldsetGrouping(form);

        assertEquals(2, messages.size());
        assertTrue(messages.get(0).contains("No <fieldset> elements found"));
        assertTrue(messages.get(1).contains("outside of fieldsets"));
    }
}
