package com.looksee.audit.informationArchitecture.models;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.audit.informationArchitecture.gcp.CloudVisionUtils;
import com.looksee.audit.informationArchitecture.models.enums.AuditCategory;
import com.looksee.audit.informationArchitecture.models.enums.AuditLevel;
import com.looksee.audit.informationArchitecture.models.enums.AuditName;
import com.looksee.audit.informationArchitecture.models.enums.AuditSubcategory;
import com.looksee.audit.informationArchitecture.models.enums.Priority;
import com.looksee.audit.informationArchitecture.services.AuditService;
import com.looksee.audit.informationArchitecture.services.PageStateService;
import com.looksee.audit.informationArchitecture.services.UXIssueMessageService;
import com.looksee.utils.BrowserUtils;

/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class LinksAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(LinksAudit.class);
		
	@Autowired
	private PageStateService page_state_service;

	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	List<String> bad_link_text_list;
	
	public LinksAudit() {
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
		List<ElementState> link_elements = page_state_service.getLinkElementStates(page_state.getId());
		String ada_compliance = "There is no ADA guideline for dead links";

		Set<String> labels = new HashSet<>();
		labels.add("information architecture");
		labels.add("accessibility");
		labels.add("navigation");
		labels.add("links");
		labels.add("wcag");
		
		//score each link element
		for(ElementState link : link_elements) {
			Document jsoup_doc = Jsoup.parseBodyFragment(link.getOuterHtml(), page_state.getUrl());
			Element element = jsoup_doc.getElementsByTag("a").first();

			log.warn("Evaluating link..."+link.getId());
			if( element.hasAttr("href") ) {
				log.warn("link has href value present ");
				String recommendation = "Make sure links have a url set for the href value.";
				String description = "Link has href attribute";
				String title = "Link has href attribute";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.NONE,
																description,
																recommendation, 
																null,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																1,
																1);

				
				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				log.warn("adding link = "+link.getId() + " to issue message = "+issue_message.getId());
				issue_message_service.addElement(issue_message.getId(), link.getId());
				issue_messages.add(issue_message);
			}
			else {
				String recommendation = "Make sure links have a url set for the href value.";
				String description = "Link is missing href attribute";
				String title = "Link is missing href attribute";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH,
																description,
																recommendation, 
																null,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																0,
																1);

				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), link.getId());
				issue_messages.add(issue_message);
				continue;
			}
			
			String href = element.attr("href");
			log.warn("evaluating href value = "+href);

			//if href is a mailto link then give score full remaining value and continue
			if(href.startsWith("mailto:")) {
				String recommendation = "";
				String description = "Link uses mailto: protocol to allow users to send email";
				String title = "Link uses mailto: protocol";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.NONE,
																description,
																recommendation, 
																null,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																1,
																1);
				
				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), link.getId());
				issue_messages.add(issue_message);
				continue;
			}
			//if href is a telephone link then give score full remaining value and continue
			else if(href.startsWith("tel:")) {
				String recommendation = "";
				String description = "Link uses tel: protocol to allow users to call";
				String title = "Link uses mailto: protocol";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.NONE,
																description,
																recommendation, 
																null,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																1,
																1);

				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), link.getId());
				issue_messages.add(issue_message);
				continue;
			}
			else if(element.hasAttr("role") 
					&& ("presentation".contentEquals(element.attr("role")) 
							|| "none".contentEquals(element.attr("role")))){
				//Skip this element because the prensentation/none role removes all semantic meaning from element
				//continue;
			}

			//does element have an href value?
			if(href != null && !href.isEmpty()) {
				String recommendation = "";
				String description = "Links have a url set for the href value";
				String title = "Link has url set for href value";

				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.NONE, 
																description, 
																recommendation, 
																null,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																1,
																1);

				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), link.getId());
				issue_messages.add(issue_message);
			}
			else {
				String recommendation = "Make sure links have a url set for the href value";
				String description = "Make sure links have a url set for the href value";
				String title = "Link url is missing";

				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH, 
																description, 
																recommendation, 
																null,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																0,
																1);

				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), link.getId());
				issue_messages.add(issue_message);
				continue;
			}
			
			// Check if element link a valid url
			String sanitized_href = "";
			try {
				String host = new URL(BrowserUtils.sanitizeUrl(page_state.getUrl(), page_state.isSecure())).getHost();
				sanitized_href = BrowserUtils.formatUrl("http", host, href, page_state.isSecure());
				if( BrowserUtils.isJavascript(href)
					|| href.startsWith("itms-apps:")
					|| href.startsWith("snap:")
					|| href.startsWith("tel:")
					|| href.startsWith("mailto:")
					|| href.startsWith("applenews:") //both apple news spellings are here because its' not clear which is the proper protocol
					|| href.startsWith("applenewss:")//both apple news spellings are here because its' not clear which is the proper protocol
				) {
					//do something here
					
				}else {
					
					//if href is external then try creating URL object, else if it's not external then check for page state
					URL href_url = new URL(sanitized_href);
				}
				
				
				//if starts with / then append host
				String recommendation = "";
				String description = "Link URL is properly formatted : "+sanitized_href;
				String title = "Link URL is properly formatted";

				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.NONE, 
																description, 
																recommendation, 
																null,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																1,
																1);
				
				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), link.getId());
				issue_messages.add(issue_message);
			} catch (MalformedURLException e) {
				String recommendation = "Make sure link url format is valid. For example \"https://www.google.com\"";
				String description = "link url is not a valid format "+href;
				String title = "Invalid link url format";

				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH, 
																description, 
																recommendation, 
																null,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																0,
																1);

				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), link.getId());
				issue_messages.add(issue_message);
				e.printStackTrace();
				continue;
			}

			//Does link have a valid URL? yes(1) / No(0)
			try {				
				if(BrowserUtils.isJavascript(href)) {
					String recommendation = "Links should have a valid URL in them. We suggest avoiding the use of the javascript protocol, expecially if you are going to use it to crete a non working link";
					String description = "This link has the href value set to 'javascript:void(0)', which causes the link to appear to users as if it doesn't work.";
					String title = "Invalid link url";

					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.HIGH,
																	description,
																	recommendation, 
																	null,
																	AuditCategory.INFORMATION_ARCHITECTURE,
																	labels,
																	ada_compliance,
																	title,
																	0,
																	1);

					issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
					issue_message_service.addElement(issue_message.getId(), link.getId());
					issue_messages.add(issue_message);
				}
				else {
					if(BrowserUtils.doesUrlExist(sanitized_href)) {
						String recommendation = "";
						String description = "Link points to valid location - "+href;
						String title = "Link points to valid location";
	
						ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																		Priority.NONE,
																		description,
																		recommendation, 
																		null,
																		AuditCategory.INFORMATION_ARCHITECTURE,
																		labels,
																		ada_compliance,
																		title,
																		1,
																		1);

						issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
						issue_message_service.addElement(issue_message.getId(), link.getId());
						issue_messages.add(issue_message);
					}
					else {
						String recommendation = "Make sure links point to valid locations";
						String description = "Link destination could not be found - "+href;
						String title = "Invalid link url";
	
						ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																		Priority.HIGH,
																		description,
																		recommendation, 
																		null,
																		AuditCategory.INFORMATION_ARCHITECTURE,
																		labels,
																		ada_compliance,
																		title,
																		0,
																		1);

						issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
						issue_message_service.addElement(issue_message.getId(), link.getId());
						issue_messages.add(issue_message);
					}
				}
			} catch (IOException e) {
				
				log.warn("IO error occurred while auditing links ...."+e.getMessage());
				log.warn("href value :: "+sanitized_href);
				String recommendation = "Make sure links point to a valid url";
				String description = "Invalid link url (IOException) - "+sanitized_href;
				String title = "Invalid link url";
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH,
																description,
																recommendation, 
																null, 
																AuditCategory.INFORMATION_ARCHITECTURE, 
																labels,
																ada_compliance,
																title, 
																3,
																4);

				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), link.getId());
				issue_messages.add(issue_message);
				e.printStackTrace();
			} catch (Exception e) {
				String recommendation = "Make sure links point to a valid url";
				String description = "Invalid link url (IOException) - "+href;
				String title = "Invalid link url";
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																			Priority.HIGH,
																			description,
																			recommendation, 
																			null, 
																			AuditCategory.INFORMATION_ARCHITECTURE, 
																			labels,
																			ada_compliance,
																			title, 
																			3,
																			4);

				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), link.getId());
				issue_messages.add(issue_message);
				log.warn("Exception thrown during links audit :: "+e.getMessage());
				e.printStackTrace();
			}
			
			//Does link contain a text label inside it
			if(!link.getAllText().isEmpty()) {
				
				//Does text contain any of the common poor link text
				String link_text = link.getAllText();
				
				if(bad_link_text_list.contains(link_text.toLowerCase().trim())) {
					String recommendation = "Replace link text with more informative text that provides proper context of what the user will find on the page that the link points to";
					String description = "Links should contain informative text. "+link_text.trim()+" does not provide enough context to be considered accessible";
					String title = "Link text is not considered accessible";
					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.MEDIUM,
																	description, 
																	recommendation, 
																	null,
																	AuditCategory.INFORMATION_ARCHITECTURE,
																	labels,
																	ada_compliance,
																	title,
																	3,
																	4);

					issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
					issue_message_service.addElement(issue_message.getId(), link.getId());
					issue_messages.add(issue_message);
				}
				else {
					String recommendation = "";
					String description = "Link contains text and is setup correctly. Well done!";
					String title = "Link is setup correctly and considered accessible";
					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.NONE,
																	description, 
																	recommendation, 
																	null,
																	AuditCategory.INFORMATION_ARCHITECTURE,
																	labels,
																	ada_compliance,
																	title, 
																	4,
																	4);
	
					issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
					issue_message_service.addElement(issue_message.getId(), link.getId());
					issue_messages.add(issue_message);
				}
			}
			else {
				//NOTE :: evaluating links with images has proven to be an issue. Commenting out for now until this can be
				//made more robust. The issue is that often the link is not the same size as the image. 
				//We should also be able to identify when all links within a parent tag have the same destination,
				// and in these scenarios recommend making the parent tag a link instead of including multiple link tags
				// NOTE 2: This is an issue for blind people and others that rely on screen readers
				// NOTE 3: Links with image tags within then should have the alt-text extracted and reviewed.
				 
				boolean element_includes_text = false;
	
				//send img src to google for text extraction
				try {
					//check if link contains image, if so then extract image source
					if(link.getOuterHtml().contains("<img")) {
						//link contains image
					}
	
					URL url = new URL( link.getScreenshotUrl() );
					BufferedImage img_src = ImageIO.read( url );
					List<String> image_text_list = CloudVisionUtils.extractImageText(img_src);
					
					for(String text : image_text_list) {
						if(text != null && !text.isEmpty()) {
							element_includes_text = true;
						}
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				 
				if(!element_includes_text) {
					String recommendation = "For best usability make sure links include text. You can assign text to a link by entering text within the link tag or by using an image with text";
					String description = "Link doesn't contain any text";
					String title = "Link is missing text";
					ada_compliance = "WCAG Criterion 2.4.4 requires that links have text that can be used to determine the purpose of a link";
	
					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.HIGH,
																	description, 
																	recommendation, 
																	null,
																	AuditCategory.INFORMATION_ARCHITECTURE,
																	labels,
																	ada_compliance,
																	title, 
																	3,
																	4);
					 //does element use image as links?
					issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
					issue_message_service.addElement(issue_message.getId(), link.getId());
					issue_messages.add(issue_message);				 
				 }
				 else {
					 String recommendation = "";
					 String description = "Link contains text and is setup correctly. Well done!";
					 String title = "Link is setup correctly and considered accessible";
	
					 ElementStateIssueMessage issue_message = new ElementStateIssueMessage(Priority.NONE,
																							description, 
																							recommendation, 
																							null,
																							AuditCategory.INFORMATION_ARCHITECTURE,
																							labels,
																							ada_compliance,
																							title, 
																							4,
																							4);
	
					issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
					issue_message_service.addElement(issue_message.getId(), link.getId());
					issue_messages.add(issue_message);
				 }
			}
			 
			//TODO : Does link have a hover styling? yes(1) / No(0)
			
			//TODO : Is link label relevant to destination url or content? yes(1) / No(0)
				//TODO :does link text exist in url?
			//	if(href.contains(element.ownText())) {
				//	score++;
				//}
				
				//TODO :does target content relate to link?
			
		}
		
		
		/*
		String why_it_matters = "Dead links are links whose source can't be found. When users encounter dead links"
				+ " they perceive the validity of what you have to say as less valuable. Often, after experiencing a"
				+ " dead link, users bounce in search of a more reputable source.";
		*/
		String why_it_matters = "Links without text are less accessible as well as generally impacting usability. "
				+ "When links don't have text, users that rely on screen readers are unable to understand what links without text are meant to accomplish."
				+ "Links without text also affect how usable your site seems, because users may not be familiar with any images or icons used as links.";		
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.INFORMATION_ARCHITECTURE.getShortName());
		
		//log.warn("LINKS AUDIT SCORE ::  "+score + " / " + (link_elements.size()*5));
		
		String description = "Making sure your links are setup correctly is incredibly important";
		//Review link audit issues
		//separate issues into 2 buckets. "good examples"(aka perfect scores) and everything else
		
		//randomly associate the elementState from the "good examples" set with the UXIssues that have less than a 100% score
		
		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			points_earned += issue_msg.getPoints();
			max_points += issue_msg.getMaxPoints();		   
/*
			if(issue_msg.getScore() < 90 && issue_msg instanceof ElementStateIssueMessage) {
				ElementStateIssueMessage element_issue_msg = (ElementStateIssueMessage)issue_msg;
				List<ElementState> good_examples = audit_service.findGoodExample(AuditName.LINKS, 100);
				if(good_examples.isEmpty()) {
					log.warn("Could not find element for good example...");
					continue;
				}
				
				if(good_examples.size() > 1) {					
					Random random = new Random();
					ElementState good_example = good_examples.get(random.nextInt(good_examples.size()-1));
					element_issue_msg.setGoodExample(good_example);
				}
				else {
					ElementState good_example = good_examples.get(0);
					element_issue_msg.setGoodExample(good_example);
				}
				issue_message_service.save(element_issue_msg);
			}
			*/
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
}
