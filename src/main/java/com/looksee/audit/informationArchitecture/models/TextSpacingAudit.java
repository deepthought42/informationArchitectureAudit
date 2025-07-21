package com.looksee.audit.informationArchitecture.models;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
import com.looksee.services.PageStateService;

/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class TextSpacingAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(LinksAudit.class);

	@Autowired
	private AuditService auditService;

    @Autowired
    private PageStateService pageStateService;

	List<String> bad_link_text_list;
	
	public TextSpacingAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * Scores links on a page based on if the link has an href value present, the url format is valid and the 
	 *   url goes to a location that doesn't produce a 4xx error 
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
		labels.add("wcag");
		
        List<ElementState> elements = pageStateService.getElementStates(page_state.getId());
        issue_messages.addAll(evaluateTextSpacing(elements));

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
     * Evaluates a list of ElementState objects for compliance with WCAG 2.1 Section 1.4.12 (Text Spacing).
     * 
     * @param elements The list of ElementState objects to evaluate.
     * @return A list of TextSpacingIssue objects representing any compliance issues found.
     */
    public static List<UXIssueMessage> evaluateTextSpacing(List<ElementState> elements) {
        List<UXIssueMessage> issues = new ArrayList<>();
        String ada_compliance = "WCAG 2.1 Section 1.4.12 - Text Spacing";
        for (ElementState element : elements) {
            // Retrieve necessary CSS properties
            double fontSize = parseCssValue(element.getRenderedCssValues().get("font-size"));
            double lineHeight = parseCssValue(element.getRenderedCssValues().get("line-height"));
            double letterSpacing = parseCssValue(element.getRenderedCssValues().get("letter-spacing"));
            double wordSpacing = parseCssValue(element.getRenderedCssValues().get("word-spacing"));
            double paragraphSpacing = parseCssValue(element.getRenderedCssValues().get("margin-bottom")); // Assuming margin-bottom is used for paragraph spacing

            // Check line height (should be at least 1.5 times the font size)
            if (lineHeight < 1.5 * fontSize) {
                String description = "Line height is less than 1.5 times the font size.";
                String title = "Insufficient line height";
                String recommendation = "Increase line height to at least 1.5 times the font size.";
                issues.add(new ElementStateIssueMessage(Priority.HIGH,
                                                        description,
                                                        recommendation,
                                                        element,
                                                        AuditCategory.ACCESSIBILITY,
                                                        new HashSet<>(),
                                                        ada_compliance,
                                                        title,
                                                        0,
                                                        1));
            }

            // Check letter spacing (should be at least 0.12 times the font size)
            if (letterSpacing < 0.12 * fontSize) {
                String description = "Letter spacing is less than 0.12 times the font size.";
                String title = "Insufficient letter spacing";
                String recommendation = "Increase letter spacing to at least 0.12 times the font size.";

                issues.add(new ElementStateIssueMessage(Priority.HIGH,
                    description,
                    recommendation,
                    element,
                    AuditCategory.ACCESSIBILITY,
                    new HashSet<>(),
                    ada_compliance,
                    title,
                    0,
                    1));
            }

            // Check word spacing (should be at least 0.16 times the font size)
            if (wordSpacing < 0.16 * fontSize) {
                String description = "Word spacing is less than 0.16 times the font size.";
                String title = "Insufficient word spacing";
                String recommendation = "Increase word spacing to at least 0.16 times the font size.";

                issues.add(new ElementStateIssueMessage(Priority.HIGH,
                    description,
                    recommendation,
                    element,
                    AuditCategory.ACCESSIBILITY,
                    new HashSet<>(),
                    ada_compliance,
                    title,
                    0,
                    1));
            }

            // Check paragraph spacing (should be at least 2 times the font size)
            if (paragraphSpacing < 2 * fontSize) {
                String description = "Paragraph spacing is less than 2 times the font size.";
                String title = "Insufficient paragraph spacing";
                String recommendation = "Increase paragraph spacing to at least 2 times the font size.";

                issues.add(new ElementStateIssueMessage(Priority.HIGH,
                    description,
                    recommendation,
                    element,
                    AuditCategory.ACCESSIBILITY,
                    new HashSet<>(),
                    ada_compliance,
                    title,
                    0,
                    1));
            }
        }

        return issues;
    }

    /**
     * Helper method to parse CSS values into double values, supporting various units.
     * 
     * @param cssValue The CSS value as a string (e.g., '16px', '1.2em').
     * @return The numeric value as a double in pixels.
     */
    public static double parseCssValue(String cssValue) {
        if (cssValue == null || cssValue.isEmpty()) {
            return 0.0;
        }

        // Supported units and their conversion factors to pixels (based on default browser settings)
        Map<String, Double> unitConversionMap = new HashMap<>();
        unitConversionMap.put("px", 1.0);
        unitConversionMap.put("em", 16.0); // Assuming 1em = 16px (default)
        unitConversionMap.put("rem", 16.0); // Assuming 1rem = 16px (default)
        unitConversionMap.put("pt", 1.333); // 1pt = 1.333px
        unitConversionMap.put("%", 0.16); // Assuming 100% = 16px
        unitConversionMap.put("cm", 37.795); // 1cm = 37.795px
        unitConversionMap.put("mm", 3.7795); // 1mm = 3.7795px
        unitConversionMap.put("in", 96.0); // 1in = 96px
        unitConversionMap.put("pc", 16.0); // 1pc = 16px
        unitConversionMap.put("ex", 8.0); // Assuming 1ex = 8px (varies between fonts)

        // Identify the unit
        String unit = cssValue.replaceAll("[0-9.]", "").toLowerCase().trim();
        double conversionFactor = unitConversionMap.getOrDefault(unit, 1.0);

        try {
            // Extract the numeric value
            double value = Double.parseDouble(cssValue.replace(unit, "").trim());
            return value * conversionFactor;
        } catch (NumberFormatException e) {
            // Handle invalid CSS values
            return 0.0;
        }
    }
}
