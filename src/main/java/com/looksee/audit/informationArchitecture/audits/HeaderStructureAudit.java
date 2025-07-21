package com.looksee.audit.informationArchitecture.audits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import com.looksee.services.AuditService;
import com.looksee.services.BrowserService;
import com.looksee.services.ElementStateService;
import com.looksee.services.UXIssueMessageService;

/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class HeaderStructureAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(LinksAudit.class);

	@Autowired
	private AuditService auditService;

    @Autowired
    private UXIssueMessageService issueMessageService;

    @Autowired
    private ElementStateService elementStateService;

	List<String> bad_link_text_list;
	
	public HeaderStructureAudit() {
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
		String ada_compliance = "WCAG 2.1 Section 1.3.1 - Structure";

		Set<String> labels = new HashSet<>();
		labels.add("information architecture");
		labels.add("accessibility");
		labels.add("headers");
		labels.add("wcag");
		
		Document jsoup_doc = Jsoup.parse(page_state.getSrc());
        Boolean h1CheckPassed = checkH1Headers(jsoup_doc);
        
        if(h1CheckPassed == null){
            String description = "The <h1> header is vital for accessibility and WCAG 2.1 compliance, as it guides users with assistive technologies through the main topic of the page, ensuring a clear and accessible content structure.";
            String title = "H1 level header not found";
            String recommendation = "To fix the issue of no <h1> header on a webpage, identify the main topic, add an <h1> at the beginning, ensure proper heading hierarchy, and use accessibility tools to test for compliance with WCAG 2.1.";
            UXIssueMessage favicon_issue = new UXIssueMessage(Priority.NONE,
															description,
															ObservationType.PAGE_STATE,
															AuditCategory.INFORMATION_ARCHITECTURE,
															ada_compliance,
															labels,
															"",
															title,
															0,
															2,
															recommendation);
			
			favicon_issue = issueMessageService.save(favicon_issue);
			issue_messages.add(favicon_issue);
        }
        else if(h1CheckPassed = Boolean.FALSE){
            String description = "Using only one <h1> header per webpage is crucial for accessibility and WCAG 2.1 compliance, ensuring clear content structure and preventing confusion for users, especially those using assistive technologies.\n";
            String title = "Too many H1 level headers";
            String recommendation = "To fix the issue of multiple <h1> headers on a webpage, define the primary topic, assign a single <h1> tag, and reorganize additional headings to maintain a clear content hierarchy. Test with accessibility tools to ensure WCAG 2.1 compliance.\n";
            UXIssueMessage favicon_issue = new UXIssueMessage(Priority.NONE,
                        description, 
                        ObservationType.PAGE_STATE,
                        AuditCategory.INFORMATION_ARCHITECTURE,
                        ada_compliance,
                        labels,
                        "",
                        title,
                        1,
                        2,
                        recommendation);

            favicon_issue = issueMessageService.save(favicon_issue);
            issue_messages.add(favicon_issue);
        }
        else if(h1CheckPassed = Boolean.TRUE){
            String description = "";
            String title = "This page has exactly 1 H1 header!";
            String recommendation = "";
            UXIssueMessage favicon_issue = new UXIssueMessage(Priority.NONE,
                        description,
                        ObservationType.PAGE_STATE,
                        AuditCategory.INFORMATION_ARCHITECTURE,
                        ada_compliance,
                        labels,
                        "",
                        title,
                        2,
                        2,
                        recommendation);

            favicon_issue = issueMessageService.save(favicon_issue);
            issue_messages.add(favicon_issue);
        }

        // Identify and print out-of-order headers
        Map<Element, List<Element>> outOfOrderHeaders = mapHeadersByAncestor(jsoup_doc);

        for (Element header : outOfOrderHeaders.keySet()) {
            String header_xpath = BrowserService.getXPath(header);
            String css_selector = BrowserService.generateCssSelectorFromXpath(header_xpath);
            ElementState header_elem = elementStateService.findByPageAndCssSelector(page_state.getId(), css_selector);
            log.warn("found header "+header_elem + ";   css selector = "+css_selector);
            String issue_description = "Having headers in hierarchical order is crucial for accessibility and WCAG 2.1 compliance because it provides a clear and logical structure to the content. This hierarchy helps users, especially those using assistive technologies like screen readers, to easily navigate the webpage and understand the relationship between different sections. Properly ordered headers guide users through the content, improving their experience and ensuring the website is accessible to all.\n";
            String recommendation = "Reconfigure document so that headers are in hierarchical order. When headers are not in hierarchical order, it makes content difficult to understand for people that require assistive technology";
            String title = "Headers are not in hierarchical order.";
            issue_messages.add(new ElementStateIssueMessage(
                                        Priority.MEDIUM,
                                        issue_description,
                                        recommendation,
                                        header_elem,
                                        AuditCategory.ACCESSIBILITY,
                                        labels,
                                        ada_compliance,
                                        title,
                                        0,
                                        1));
        }

		String why_it_matters = "A well-structured header hierarchy is like a road map for your content—it helps screen readers and assistive technologies navigate the page, making it easier for everyone to understand the content flow. When headers are in the correct order, users can skim and comprehend information more efficiently, which is key to meeting WCAG 2.1 Section 1.3.1 requirements.";
		
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
     * Checks the number of <h1> headers in the given JSoup Document.
     *
     * Preconditions:
     * - The input Document (doc) must not be null.
     *
     * Postconditions:
     * - Returns null if there are no <h1> headers in the document.
     * - Returns true if there is exactly one <h1> header in the document.
     * - Returns false if there are more than one <h1> headers in the document.
     *
     * @param doc the JSoup Document to check for <h1> headers
     * @return Boolean indicating the number of <h1> headers (null, true, or false)
     * @throws IllegalArgumentException if the input Document is null
     */
    public static Boolean checkH1Headers(Document doc) {
        // Preconditions: doc should not be null
        if (doc == null) {
            throw new IllegalArgumentException("Document must not be null");
        }

        // Retrieve all <h1> elements from the document
        Elements h1Headers = doc.select("h1");

        // Postconditions: ensure return value follows the contract
        int headerCount = h1Headers.size();

        // Contract:
        // 1. If headerCount is 0, return null.
        // 2. If headerCount is 1, return true.
        // 3. If headerCount > 1, return false.

        if (headerCount == 0) {
            assert (headerCount == 0) : "Expected no <h1> headers, returning null.";
            return null;  // No <h1> headers found
        } else if (headerCount == 1) {
            assert (headerCount == 1) : "Expected exactly one <h1> header, returning true.";
            return true;  // Exactly one <h1> header found
        } else {
            assert (headerCount > 1) : "Expected more than one <h1> header, returning false.";
            return false;  // More than one <h1> header found
        }
    }

    /**
     * Recursively maps headers in an HTML document, grouping them by their common ancestor elements.
     *
     * @param doc the JSoup Document to be mapped
     * @return a map where the keys are ancestor elements and the values are lists of header elements grouped under each ancestor
     */
    public static Map<Element, List<Element>> mapHeadersByAncestor(Document doc) {
        // Create a map to store ancestor elements and their associated headers
        Map<Element, List<Element>> ancestorHeaderMap = new HashMap<>();

        // Start the recursive process from the root of the document
        mapHeadersRecursive(doc.body(), ancestorHeaderMap);
        System.out.println("Out-of-order headers by ancestor:"+ancestorHeaderMap);

        return ancestorHeaderMap;
    }

    /**
     * Recursive helper method to traverse the DOM tree and map headers to their common ancestors.
     *
     * @param element the current DOM element being processed
     * @param ancestorHeaderMap the map storing ancestor elements and their associated headers
     */
    private static void mapHeadersRecursive(Element element, Map<Element, List<Element>> ancestorHeaderMap) {
        // Identify if the current element is a header (h1, h2, h3, h4, h5, h6)
        if (isHeader(element)) {
            // Find the nearest common ancestor that is not a header
            Element commonAncestor = findNearestAncestor(element);

            // Add the header to the ancestor's list in the map
            ancestorHeaderMap.computeIfAbsent(commonAncestor, k -> new ArrayList<>()).add(element);
        }

        // Recursively process each child element
        for (Element child : element.children()) {
            mapHeadersRecursive(child, ancestorHeaderMap);
        }
    }

    /**
     * Determines if a given element is a header (h1, h2, h3, h4, h5, h6).
     *
     * @param element the element to check
     * @return true if the element is a header, false otherwise
     */
    private static boolean isHeader(Element element) {
        String tagName = element.tagName();
        return tagName.equals("h1") || tagName.equals("h2") || tagName.equals("h3") ||
               tagName.equals("h4") || tagName.equals("h5") || tagName.equals("h6");
    }

    /**
     * Finds the nearest common ancestor of an element that is not a header.
     *
     * @param element the element for which to find the nearest ancestor
     * @return the nearest ancestor that is not a header
     */
    private static Element findNearestAncestor(Element element) {
        Element parent = element.parent();

        // Traverse up the DOM tree until a non-header ancestor is found
        while (parent != null && isHeader(parent)) {
            parent = parent.parent();
        }

        return parent != null ? parent : element;  // If no parent found, return the element itself
    }
}
