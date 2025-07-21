package com.looksee.audit.informationArchitecture.models;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.audit.informationArchitecture.audits.LinksAudit;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.ElementStateIssueMessage;
import com.looksee.models.audit.IExecutablePageStateAudit;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.services.AuditService;
import com.looksee.services.BrowserService;
import com.looksee.services.ElementStateService;

/**
 * Responsible for executing an audit on the input labels on a page for the information architecture audit category
 */
@Component
public class InputLabelAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(LinksAudit.class);

	@Autowired
	private AuditService auditService;

    @Autowired
    private ElementStateService elementStateService;

	public InputLabelAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * Scores input labels on a page based on if the input has a label associated with it
	 *   
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		assert page_state != null;
		assert audit_record != null;

		//check if page state already had a link audit performed.
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		Set<String> labels = new HashSet<>();
		labels.add("information architecture");
		labels.add("accessibility");
		labels.add("tables");
		labels.add("wcag");
		
		Document jsoup_doc = Jsoup.parse(page_state.getSrc());
        issue_messages.addAll(checkWCAGCompliance(jsoup_doc, page_state));

		String why_it_matters = "Grouping form controls within a <fieldset> element is important for accessibility because it provides a clear, semantic structure that enhances the understanding of the form's organization, especially for users with disabilities. The <fieldset> element, often paired with a <legend>, helps screen readers and other assistive technologies to convey related groups of controls as a single, coherent unit, ensuring that users can navigate and comprehend the form's layout more effectively. This practice aligns with WCAG 2.1 guidelines, supporting a more inclusive and accessible web experience.";
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.INFORMATION_ARCHITECTURE.getShortName());
		
		String description = "Making sure your links are setup correctly is incredibly important";
		
		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			points_earned += issue_msg.getPoints();
			max_points += issue_msg.getMaxPoints();
		}
		
		Audit audit = new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
								 AuditSubcategory.NAVIGATION,
								 AuditName.LINKS,
								 points_earned,
								 issue_messages,
								 AuditLevel.PAGE,
								 max_points,
								 page_state.getUrl(),
								 why_it_matters,
								 description,
								 true);
		
		return auditService.save(audit);
	}

    /**
     * Checks if the given HTML document complies with WCAG 2.1 Section 3.3.2.
     * This section requires labels or instructions to be provided for user input fields.
     * 
     * @param doc the JSoup Document representing the HTML to be validated
     * @return true if the document is compliant, false otherwise
     */
    public List<UXIssueMessage> checkWCAGCompliance(Document doc, PageState page_state) {
        // Design by Contract - Precondition: The document must contain at least one form
        Elements forms = doc.getElementsByTag("form");
        List<UXIssueMessage> issues = new ArrayList<>();
        // Iterate through each form element found in the document
        for (Element form : forms) {
            // Design by Contract - Precondition: The form must contain input elements (e.g., input, textarea, select)
            Elements inputs = form.select("input, textarea, select");

            // Iterate through each input element inside the form
            for (Element input : inputs) {
                // Precondition: Input elements must have an 'id' attribute for label association
                String inputId = input.id();

                String xpath = BrowserService.getXPath(input);
                String cssSelector = BrowserService.generateCssSelectorFromXpath(xpath);
                ElementState input_element = elementStateService.findByPageAndCssSelector(page_state.getId(), cssSelector);

                // Search for a label that is associated with the input by the 'for' attribute
                Elements labels = form.select("label[for=" + inputId + "]");
                String description = "Clear labels or instructions for form inputs are essential to making websites accessible for everyone, especially users with disabilities. They help people understand what’s required, reducing confusion and errors when filling out forms. By ensuring input fields are properly labeled, websites become easier to use for a broader audience, including those relying on assistive technologies.";
                String wcag_compliance = "WCAG 2.1 Section 3.3.2 - Labels or Instructions";

                // Design by Contract - Postcondition: Each input must have at least one associated label
                if (labels.isEmpty()) {
                    String recommendation = "add a <label> element that clearly describes the purpose of the input and associate it with the field using the for attribute, which should match the input's id. If a visible label isn't suitable, consider using an aria-label or aria-labelledby attribute to provide descriptive text for screen readers.";
                    String title = "Missing label for input";

                    issues.add(new ElementStateIssueMessage(Priority.HIGH,
                                    description,
                                    recommendation,
                                    input_element,
                                    AuditCategory.ACCESSIBILITY,
                                    new HashSet<String>(),
                                    wcag_compliance,
                                    title,
                                    0,
                                    1));
                }
                else{
                    String recommendation = "";
                    String title = "Label exists for input element";
                    issues.add(new ElementStateIssueMessage(Priority.HIGH,
                                    description,
                                    recommendation,
                                    input_element,
                                    AuditCategory.ACCESSIBILITY,
                                    new HashSet<String>(),
                                    wcag_compliance,
                                    title,
                                    0,
                                    1));
                }
            }
        }

        return issues; // The document is compliant
    }
}
