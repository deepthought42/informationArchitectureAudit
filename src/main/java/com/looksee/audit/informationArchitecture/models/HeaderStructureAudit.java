package com.looksee.audit.informationArchitecture.models;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
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

import com.looksee.audit.informationArchitecture.models.enums.AuditCategory;
import com.looksee.audit.informationArchitecture.models.enums.AuditLevel;
import com.looksee.audit.informationArchitecture.models.enums.AuditName;
import com.looksee.audit.informationArchitecture.models.enums.AuditSubcategory;
import com.looksee.audit.informationArchitecture.models.enums.Priority;
import com.looksee.audit.informationArchitecture.services.AuditService;

/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class HeaderStructureAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(LinksAudit.class);

	@Autowired
	private AuditService audit_service;

	List<String> bad_link_text_list;
	
	public HeaderStructureAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
		
		bad_link_text_list = new ArrayList<>();
		bad_link_text_list.add("click here");
		bad_link_text_list.add("here");
		bad_link_text_list.add("more");
		bad_link_text_list.add("read more");
		bad_link_text_list.add("learn more");
		bad_link_text_list.add("info");
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
		String ada_compliance = "WCAG 2.1 Section 1.3.1 - Structure";

		Set<String> labels = new HashSet<>();
		labels.add("information architecture");
		labels.add("accessibility");
		labels.add("headers");
		labels.add("wcag");
		
		Document jsoup_doc = Jsoup.parse(page_state.getSrc());

        Boolean h1CheckPassed = checkH1Headers(jsoup_doc);
        if(h1CheckPassed == null){

        }
        else if(h1CheckPassed = Boolean.TRUE){

        }
        else if(h1CheckPassed = Boolean.FALSE){

        }

        // Identify and print out-of-order headers
        Map<Element, List<Element>> outOfOrderHeaders = mapHeadersByAncestor(jsoup_doc);
        System.out.println("Out-of-order headers:"+outOfOrderHeaders);
        String issue_description = "Headers are not in hierarchical order.";
        String recommendation = "Reconfigure document so that headers are in hierarchical order. When headers are not in hierarchical order, it makes content difficult to understand for people that require assistive technology";
        String title = "Header Structure";

        for (Element header : outOfOrderHeaders.keySet()) {
            issue_messages.add(new PageStateIssueMessage(page_state, 
                                                        issue_description, 
                                                        recommendation, 
                                                        Priority.MEDIUM, 
                                                        AuditCategory.ACCESSIBILITY, 
                                                        labels, 
                                                        ada_compliance, 
                                                        title, 
                                                        0, 
                                                        1));
            System.out.println(header.tagName() + ": " + header.text());
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
		
		return audit_service.save(audit);
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
        System.out.println("Out-of-order headers:"+ancestorHeaderMap);

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
