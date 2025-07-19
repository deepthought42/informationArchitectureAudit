package com.looksee.audit.informationArchitecture.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
public class PageLanguageAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(LinksAudit.class);

	@Autowired
	private AuditService auditService;

    @Autowired
    private ElementStateService elementStateService;

    @Autowired
    private PageStateService pageStateService;

	List<String> bad_link_text_list;
	
	public PageLanguageAudit() {
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
		
        Document jsoup_doc = Jsoup.parse(page_state.getSrc());
        issue_messages.addAll(checkLanguageCompliance(jsoup_doc));

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

    // Set of all valid ISO 639-1 language codes
    private static final Set<String> VALID_LANGUAGE_CODES = new HashSet<>(Arrays.asList(
        "aa", "ab", "ae", "af", "ak", "am", "an", "ar", "as", "av",
        "ay", "az", "ba", "be", "bg", "bh", "bi", "bm", "bn", "bo",
        "br", "bs", "ca", "ce", "ch", "co", "cr", "cs", "cu", "cv",
        "cy", "da", "de", "dv", "dz", "ee", "el", "en", "eo", "es",
        "et", "eu", "fa", "ff", "fi", "fj", "fo", "fr", "fy", "ga",
        "gd", "gl", "gn", "gu", "gv", "ha", "he", "hi", "ho", "hr",
        "ht", "hu", "hy", "hz", "ia", "id", "ie", "ig", "ii", "ik",
        "io", "is", "it", "iu", "ja", "jv", "ka", "kg", "ki", "kj",
        "kk", "kl", "km", "kn", "ko", "kr", "ks", "ku", "kv", "kw",
        "ky", "la", "lb", "lg", "li", "ln", "lo", "lt", "lu", "lv",
        "mg", "mh", "mi", "mk", "ml", "mn", "mr", "ms", "mt", "my",
        "na", "nb", "nd", "ne", "ng", "nl", "nn", "no", "nr", "nv",
        "ny", "oc", "oj", "om", "or", "os", "pa", "pi", "pl", "ps",
        "pt", "qu", "rm", "rn", "ro", "ru", "rw", "sa", "sc", "sd",
        "se", "sg", "si", "sk", "sl", "sm", "sn", "so", "sq", "sr",
        "ss", "st", "su", "sv", "sw", "ta", "te", "tg", "th", "ti",
        "tk", "tl", "tn", "to", "tr", "ts", "tt", "tw", "ty", "ug",
        "uk", "ur", "uz", "ve", "vi", "vo", "wa", "wo", "xh", "yi",
        "yo", "za", "zh", "zu"
    ));

    /**
     * Validates whether the provided language code is a valid ISO 639-1 language code.
     *
     * @param lang The language code to validate.
     * @return true if the language code is valid, false otherwise.
     */
    public static boolean isValidLanguageCode(String lang) {
        if(lang == null){
            return false;
        }
        // Ensure the language code is lowercase and check against the set
        return VALID_LANGUAGE_CODES.contains(lang.toLowerCase().trim());
    }

    /**
     * Checks the HTML document for compliance with WCAG 2.1 Section 3.1.1 regarding the language of the page.
     * This method verifies that the <html> element has a valid "lang" attribute that specifies the primary language of the document.
     *
     * @param doc The Jsoup Document object representing the HTML content to be evaluated. Must not be null.
     * @return A list of GenericIssue objects representing compliance issues found. The list is never null but may be empty.
     */
    public static List<UXIssueMessage> checkLanguageCompliance(Document doc) {
        // Preconditions
        assert doc != null : "Document must not be null";

        List<UXIssueMessage> issues = new ArrayList<>();

        // Get the <html> element
        Element htmlElement = doc.select("html").first();

        // Check if the <html> tag has a "lang" attribute
        if (htmlElement != null) {
            String langAttribute = htmlElement.attr("lang").trim();

            // Validate that the lang attribute is present and not empty
            if (langAttribute.isEmpty()) {
                String title = "Missing Language Attribute";
                String description = "The <html> element does not have a 'lang' attribute, or it is empty.";
                String recommendation = "Add a valid 'lang' attribute to the <html> element to specify the primary language of the document.";
                String why_it_matters = "";
                String wcag_compliance = "";
                
                issues.add(new UXIssueMessage(Priority.HIGH,
                                    description,
                                    ObservationType.PAGE_LANGUAGE,
                                    AuditCategory.ACCESSIBILITY,
                                    wcag_compliance,
                                    new HashSet<>(),
                                    why_it_matters,
                                    title,
                                    0,
                                    0,
                                    recommendation));
            } else {
                String title = "Invalid Language Code";
                String description = "The 'lang' attribute on the <html> element contains an invalid or unrecognized language code.";
                String recommendation = "Use a valid ISO 639-1 language code (e.g., 'en', 'fr', 'es') in the 'lang' attribute.";
                String why_it_matters = "";
                String wcag_compliance = "";
                // Optionally, validate the language code (e.g., "en", "fr", "es")
                if (!isValidLanguageCode(langAttribute)) {
                    issues.add( new UXIssueMessage(Priority.HIGH,
                                    description,
                                    ObservationType.PAGE_LANGUAGE,
                                    AuditCategory.ACCESSIBILITY,
                                    wcag_compliance,
                                    new HashSet<>(),
                                    why_it_matters,
                                    title,
                                    0,
                                    0,
                                    recommendation));
                }
            }
        } else {
            String title = "Missing <html> Element";
            String description =  "The <html> element is missing from the document.";
            String recommendation = "Ensure the document has an <html> element with a valid 'lang' attribute specifying the primary language.";
            String why_it_matters = "";
            String wcag_compliance = "";

            issues.add(new UXIssueMessage(Priority.HIGH,
                            description,
                            ObservationType.PAGE_LANGUAGE,
                            AuditCategory.ACCESSIBILITY,
                            wcag_compliance,
                            new HashSet<>(),
                            why_it_matters,
                            title,
                            0,
                            0,
                            recommendation));
        }

        // Postconditions
        assert issues != null : "Issues list must not be null";

        // Return list of issues found
        return issues;
    }
}
