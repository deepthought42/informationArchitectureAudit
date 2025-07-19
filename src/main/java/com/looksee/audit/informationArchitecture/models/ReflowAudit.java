package com.looksee.audit.informationArchitecture.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.IExecutablePageStateAudit;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;
import com.looksee.services.PageStateService;

/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class ReflowAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(LinksAudit.class);

	@Autowired
	private AuditService auditService;

    @Autowired
    private ElementStateService elementStateService;

    @Autowired
    private PageStateService pageStateService;

	List<String> bad_link_text_list;
	
	public ReflowAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Scores links on a page based on if the link has an href value present, the url format is valid and the 
	 *   url goes to a location that doesn't produce a 4xx error 
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
		
        issue_messages.addAll(checkForCompliance(page_state));
        
        

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

    private List<UXIssueMessage> checkForCompliance(PageState page_state) {
        List<UXIssueMessage> issues = new ArrayList<>();
        List<ElementState> elements = pageStateService.getElementStates(page_state.getId());
        for(ElementState element: elements){
            issues.add(checkElementForReflowCompliance(element));
        }

        return issues;
    }

    // Helper method to check individual elements
    private UXIssueMessage checkElementForReflowCompliance(ElementState element) {
        // Precondition: Element must be valid and displayed
        assert element != null : "Precondition failed: Element must not be null";

        // Check for fixed-width elements that could cause reflow issues
        String width = element.getRenderedCssValues().get("width");
        String overflow = element.getRenderedCssValues().get("overflow");

        if (isFixedWidth(width) && !"visible".equals(overflow)) {
            System.out.println("Potential reflow issue found in element: " + element.getName() + " with CSS Selector: " + element.getCssSelector());

            String description = "Web content doesn't properly adjust to fit within the viewport, causing users to scroll horizontally or lose access to information. This often happens on small screens or when zoomed in, making it difficult for users to read or interact with the content effectively.";
            String wcag_compliance = "WCAG 2.1 Section 1.4.10 - Reflow";
            String why_it_matters = "Reflow matters because it ensures that all users, including those with low vision or those accessing content on smaller screens, can easily read and interact with web content without excessive horizontal scrolling. This improves accessibility by providing a more seamless and user-friendly experience, allowing everyone to access information without barriers.";
            String title = "Web content doesn't adjust to fit the viewport";
            String recommendation = "Ensure that your web content is designed to be responsive by using flexible layouts, relative units like percentages for widths, and CSS media queries. Avoid fixed-width elements and make sure that all content, including text, images, and interactive elements, can adapt to fit within the viewport without requiring horizontal scrolling.";
    
            return new UXIssueMessage(Priority.MEDIUM,
                                    description,
                                    ObservationType.REFLOW,
                                    AuditCategory.ACCESSIBILITY,
                                    wcag_compliance,
                                    new HashSet<>(),
                                    why_it_matters, 
                                    title, 
                                    0, 
                                    0, 
                                    recommendation);
    
        }

        // Postcondition: Ensures that checks are performed on the element
        assert element.getName() != null : "Postcondition failed: Element tag name should not be null";

        String description = "Web content adjusts to fit the viewport";
        String wcag_compliance = "WCAG 2.1 Section 1.4.10 - Reflow";
        String why_it_matters = "Reflow matters because it ensures that all users, including those with low vision or those accessing content on smaller screens, can easily read and interact with web content without excessive horizontal scrolling. This improves accessibility by providing a more seamless and user-friendly experience, allowing everyone to access information without barriers.";
        String title = "Web content adjusts to fit the viewport";
        String recommendation = "Ensure that your web content is designed to be responsive by using flexible layouts, relative units like percentages for widths, and CSS media queries. Avoid fixed-width elements and make sure that all content, including text, images, and interactive elements, can adapt to fit within the viewport without requiring horizontal scrolling.";

        return new UXIssueMessage(Priority.MEDIUM,
                                description,
                                ObservationType.REFLOW,
                                AuditCategory.ACCESSIBILITY,
                                wcag_compliance,
                                new HashSet<>(),
                                why_it_matters,
                                title,
                                0,
                                0,
                                recommendation);
    }

    // Helper method to determine if the width is fixed
    private boolean isFixedWidth(String width) {
        // Invariant: Width should be in pixels or percentage
        assert width != null && !width.isEmpty() : "Invariant failed: Width value must not be null or empty";
        return width.endsWith("px") || width.endsWith("%");
    }
}
