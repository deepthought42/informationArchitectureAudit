package com.looksee.audit.informationArchitecture.services;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.looksee.audit.informationArchitecture.models.Browser;
import com.looksee.audit.informationArchitecture.models.BrowserConnectionHelper;
import com.looksee.audit.informationArchitecture.models.enums.BrowserEnvironment;
import com.looksee.audit.informationArchitecture.models.enums.BrowserType;

/**
 * A collection of methods for interacting with the {@link Browser} session object
 *
 */
@Component
public class BrowserService {
	private static Logger log = LoggerFactory.getLogger(BrowserService.class);
	
	/**
	 * retrieves a new browser connection
	 *
	 * @param browser_name name of the browser (ie. firefox, chrome)
	 *
	 * @return new {@link Browser} instance
	 * @throws MalformedURLException
	 *
	 * @pre browser_name != null;
	 * @pre !browser_name.isEmpty();
	 */
	public Browser getConnection(BrowserType browser, BrowserEnvironment browser_env) throws MalformedURLException {
		assert browser != null;

		return BrowserConnectionHelper.getConnection(browser, browser_env);
	}
	
	public static String generalizeSrc(String src) {
		assert src != null;
		Document html_doc = Jsoup.parse(src);
		html_doc.select("script").remove();
		html_doc.select("link").remove();
		html_doc.select("style").remove();
		html_doc.select("iframe").remove();
		
		//html_doc.attr("id","");
		for(Element element : html_doc.getAllElements()) {
			/*
			element.removeAttr("id")
				   .removeAttr("name")
				   .removeAttr("style")
				   .removeAttr("data-id");
			*/
		    List<String>  attToRemove = new ArrayList<>();
			for (Attribute a : element.attributes()) {
				if(element.tagName().contentEquals("img") && a.getKey().contentEquals("src")) {
					continue;
				}
		        // transfer it into a list -
		        // to be sure ALL data-attributes will be removed!!!
		        attToRemove.add(a.getKey());
		    }

		    for(String att : attToRemove) {
		        element.removeAttr(att);
		   }
		}
		
		return removeComments(html_doc.html());
	}
	
	/**
	 * Removes HTML comments from html string
	 * 
	 * @param html
	 * 
	 * @return html string without comments
	 */
	public static String removeComments(String html) {
		return Pattern.compile("<!--.*?-->").matcher(html).replaceAll("");
    }


	/** MESSAGE GENERATION METHODS **/
	static String[] data_extraction_messages = {
		"Locating elements",
		"Create an account to get results faster",
		"Looking for content",
		"Having a look-see",
		"Extracting colors",
		"Checking fonts",
		"Pssst. Get results faster by logging in",
		"Mapping page structure",
		"Locating links",
		"Extracting navigation",
		"Pssst. Get results faster by logging in",
		"Create an account to get results faster",
		"Mapping CSS styles",
		"Generating unique CSS selector",
		"Mapping forms",
		"Measuring whitespace",
		"Pssst. Get results faster by logging in",
		"Create an account to get results faster",
		"Mapping attributes",
		"Mapping attributes",
		"Mapping attributes",
		"Mapping attributes",
		"Mapping attributes",
		"Mapping attributes",
		"Extracting color palette",
		"Looking for headers",
		"Mapping content structure",
		"Create an account to get results faster",
		"Wow! There's a lot of elements here",
		"Wow! There's a lot of elements here",
		"Wow! There's a lot of elements here",
		"Wow! There's a lot of elements here",
		"Wow! There's a lot of elements here",
		"Wow! There's a lot of elements here",
		"Wow! There's a lot of elements here",
		"Wow! There's a lot of elements here",
		"Wow! There's a lot of elements here",
		"Crunching the numbers",
		"Pssst. Get results faster by logging in",
		"Create an account to get results faster",
		"Searching for areas of interest",
		"Evaluating purpose of webpage",
		"Just a single page audit? Login to audit a domain",
		"Labeling icons",
		"Labeling images",
		"Labeling logos",
		"Applying customizations",
		"Checking for overfancification",
		"Grouping by proximity",
		"Almost there!",
		"Create an account to get results faster",
		"Labeling text elements",
		"Labeling links",
		"Pssst. Get results faster by logging in",
		"Labeling images",
		"Mapping form fields",
		"Extracting templates",
		"Contemplating the meaning of the universe",
		"Checking template structure"
	};
	
	/**
     * Generates an XPath for the given element using indexes for each tag to specify its location
     * relative to other tags with the same name.
     *
     * @param element The JSoup Element for which to generate the XPath.
     * @return A string representing the XPath of the element.
     */
    public static String getXPath(Element element) {
        StringBuilder xpath = new StringBuilder();

        // Traverse up the DOM tree to construct the XPath
        while (element != null) {
            int index = getElementIndex(element);
            String tagName = element.tagName();

            // Construct the XPath part for this element
            xpath.insert(0, "/" + tagName + "[" + index + "]");

            // Move up to the parent element
            Node parent = element.parent();
            if (parent instanceof Element && !"body".equals(((Element)parent).tagName())) {
                element = (Element) parent;
            } else {
				xpath.insert(0, "//body");
                break;
            }
        }

        // Return the full XPath
        return xpath.toString();
    }

    /**
     * Returns the 1-based index of the element among its siblings with the same tag name.
     *
     * @param element The element whose index to determine.
     * @return The 1-based index of the element.
     */
    private static int getElementIndex(Element element) {
        int index = 1; // XPath indices are 1-based
        Element previousSibling = element.previousElementSibling();

        // Count the number of preceding siblings with the same tag name
        while (previousSibling != null) {
            if (previousSibling.tagName().equals(element.tagName())) {
                index++;
            }
            previousSibling = previousSibling.previousElementSibling();
        }

        return index;
    }
	
	/**
	 * generates a unique xpath for this element.
	 *
	 * @return an xpath that identifies this element uniquely
	 */
	public static String generateCssSelectorFromXpath(String xpath){
		List<String> selectors = new ArrayList<>();
		
		//split xpath on '/' character
		String[] xpath_selectors = xpath.split("/");
		for(String xpath_selector : xpath_selectors) {
			//transform selector to css selector
			String css_select = transformXpathSelectorToCss(xpath_selector);
			selectors.add(css_select);
		}
		
		return buildCssSelector(selectors);
	}

	/**
	 * combines list of sub selectors into cohesive css_selector
	 * @param selectors
	 * @return
	 */
	private static String buildCssSelector(List<String> selectors) {
		String css_selector = "";
		
		for(String selector : selectors) {
			if(css_selector.isEmpty() && !selector.isEmpty()) {
				css_selector = selector;
			}
			else if(!css_selector.isEmpty() && !selector.isEmpty()){
				css_selector += " " + selector;
			}
		}
		
		return css_selector;
	}

	public static String transformXpathSelectorToCss(String xpath_selector) {
		String selector = "";
		
		//convert index value with format '[integer]' to css format
		String pattern_string = "(\\[([0-9]+)\\])";
        Pattern pattern_index = Pattern.compile(pattern_string);
        Matcher matcher = pattern_index.matcher(xpath_selector);
        if(matcher.find()) {
        	String match = matcher.group(1);
        	match = match.replace("[", "");
        	match = match.replace("]", "");
        	int element_index = Integer.parseInt(match);
        	selector = xpath_selector.replaceAll(pattern_string, "");

			selector += ":nth-child(" + element_index + ")";
        }
        else {
        	selector = xpath_selector;
        }
        
		return selector.trim();
	}
	
	/**
	 * Filters out html, body, link, title, script, meta, head, iframe, or noscript tags
	 *
	 * @param tag_name
	 *
	 * @pre tag_name != null
	 *
	 * @return true if tag name is html, body, link, title, script, meta, head, iframe, or noscript
	 */
	public static boolean isStructureTag(String tag_name) {
		assert tag_name != null;

		return "head".contentEquals(tag_name) || "link".contentEquals(tag_name) 
				|| "script".contentEquals(tag_name) || "g".contentEquals(tag_name) 
				|| "path".contentEquals(tag_name) || "svg".contentEquals(tag_name) 
				|| "polygon".contentEquals(tag_name) || "br".contentEquals(tag_name) 
				|| "style".contentEquals(tag_name) || "polyline".contentEquals(tag_name) 
				|| "use".contentEquals(tag_name) || "template".contentEquals(tag_name) 
				|| "audio".contentEquals(tag_name)  || "iframe".contentEquals(tag_name)
				|| "noscript".contentEquals(tag_name) || "meta".contentEquals(tag_name) 
				|| "base".contentEquals(tag_name) || "em".contentEquals(tag_name);
	}
	
	/**
	 * Get immediate parent elements for a given element
	 *
	 * @param elem	{@linkplain WebElement) to get parent of
	 * @return parent {@linkplain WebElement)
	 */
	public WebElement getParentElement(WebElement elem) throws WebDriverException{
		return elem.findElement(By.xpath(".."));
	}

	public static String cleanAttributeValues(String attribute_values_string) {
		String escaped = attribute_values_string.replaceAll("[\\t\\n\\r]+"," ");
		escaped = escaped.trim().replaceAll("\\s+", " ");
		escaped = escaped.replace("\"", "\\\"");
		return escaped.replace("\'", "'");
	}

	/**
	 * Extracts template for element by using outer html and removing inner text
	 * @param element {@link Element}
	 * @return templated version of element html
	 */
	public static String extractTemplate(String outerHtml){
		assert outerHtml != null;
		assert !outerHtml.isEmpty();
		
		Document html_doc = Jsoup.parseBodyFragment(outerHtml);

		Cleaner cleaner = new Cleaner(Whitelist.relaxed());
		html_doc = cleaner.clean(html_doc);
		
		html_doc.select("script").remove()
				.select("link").remove()
				.select("style").remove();

		for(Element element : html_doc.getAllElements()) {
			element.removeAttr("id");
			element.removeAttr("name");
			element.removeAttr("style");
		}
		
		return html_doc.html();
	}

	public String getPageSource(Browser browser, URL sanitized_url) throws MalformedURLException {
		assert browser != null;
		assert sanitized_url != null;
		
		return browser.getSource();
	}
}


@ResponseStatus(HttpStatus.SEE_OTHER)
class ServiceUnavailableException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 794045239226319408L;

	public ServiceUnavailableException(String msg) {
		super(msg);
	}
}

@ResponseStatus(HttpStatus.SEE_OTHER)
class FiveZeroThreeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 452417401491490882L;

	public FiveZeroThreeException(String msg) {
		super(msg);
	}
}
