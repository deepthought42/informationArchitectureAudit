package com.looksee.audit.informationArchitecture.models;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.audit.informationArchitecture.services.AuditService;
import com.looksee.audit.informationArchitecture.services.ElementStateService;

/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class SeleniumDrivenAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(LinksAudit.class);

	@Autowired
	private AuditService auditService;

    @Autowired
    private ElementStateService elementStateService;

	List<String> bad_link_text_list;
	
	public SeleniumDrivenAudit() {
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
        assert audit_record != null;
        assert page_state != null;
        
		//check if page state already had a link audit performed.
		Set<UXIssueMessage> issue_messages = new HashSet<>();
        /**
		String ada_compliance = "WCAG 2.1 Section 1.3.1 - Tables";
        
		Set<String> labels = new HashSet<>();
		labels.add("information architecture");
		labels.add("accessibility");
		labels.add("wcag");
		
		Document jsoup_doc = Jsoup.parse(page_state.getSrc());
        List<GenericIssue> issues = checkCompliance(jsoup_doc);
        
        for(GenericIssue issue: issues){
            ElementState element_state = elementStateService.findByPageAndCssSelector(page_state.getId(), issue.getCssSelector());
            UXIssueMessage issue_msg = new ElementStateIssueMessage(Priority.HIGH,
                                                                issue.getDescription(),
                                                                issue.getRecommendation(),
                                                                element_state,
                                                                AuditCategory.ACCESSIBILITY,
                                                                labels,
                                                                ada_compliance,
                                                                issue.getTitle(),
                                                                0,
                                                                1);
            issue_messages.add(issue_msg);
        }

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
	*/
        return null;

    }

    public static void checkVisualPresentationCompliance(WebDriver driver) {
        // Get all text elements
        List<WebElement> textElements = driver.findElements(By.xpath("//*[not(self::script) and not(self::style)]/text()"));

        // Evaluate each text element
        for (WebElement element : textElements) {
            // Check text resizing by setting font size to 200%
            String script = "arguments[0].style.fontSize = '200%'; return arguments[0].offsetHeight;";
            int originalHeight = (int) ((JavascriptExecutor) driver).executeScript(script, element);
            int resizedHeight = (int) ((JavascriptExecutor) driver).executeScript(script, element);

            if (resizedHeight != originalHeight) {
                System.out.println("Text resizing failed for element: " + element.getText());
            }

            // Check line height (leading)
            String lineHeight = element.getCssValue("line-height");
            String fontSize = element.getCssValue("font-size");

            if (parsePxValue(lineHeight) < 1.5 * parsePxValue(fontSize)) {
                System.out.println("Insufficient line height for element: " + element.getText());
            }

            // Check paragraph spacing
            String marginBottom = element.getCssValue("margin-bottom");

            if (parsePxValue(marginBottom) < 2 * parsePxValue(fontSize)) {
                System.out.println("Insufficient paragraph spacing for element: " + element.getText());
            }

            // Check text justification
            String textAlign = element.getCssValue("text-align");

            if ("justify".equalsIgnoreCase(textAlign)) {
                System.out.println("Text is justified, which is not compliant: " + element.getText());
            }

            // Check color contrast (foreground and background colors)
            String color = element.getCssValue("color");
            String backgroundColor = element.getCssValue("background-color");

            if (!isColorContrastSufficient(color, backgroundColor)) {
                System.out.println("Insufficient color contrast for element: " + element.getText());
            }
        }
    }

    private static double parsePxValue(String value) {
        return Double.parseDouble(value.replace("px", ""));
    }

    private static boolean isColorContrastSufficient(String color, String backgroundColor) {
        // You can add color contrast logic here. For simplicity, let's just return true.
        // Ideally, you would use a library or implement a method to calculate contrast ratio.
        return true;
    }
}
