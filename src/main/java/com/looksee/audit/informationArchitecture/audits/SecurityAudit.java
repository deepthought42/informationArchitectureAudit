package com.looksee.audit.informationArchitecture.audits;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.looksee.services.UXIssueMessageService;

/**
 * Responsible for executing an audit on the security on a page for the information architecture audit category
 */
@Component
public class SecurityAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(SecurityAudit.class);
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	public SecurityAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.TEXT_BACKGROUND_CONTRAST);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Checks if 
	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		assert page_state != null;
		Set<UXIssueMessage> issue_messages = new HashSet<>();

		String why_it_matters = "Sites that don't use HTTPS are highly insecure and are more likley to leak personal identifiable information(PII). Modern users are keenly aware of this fact and are less likely to trust sites that aren't secured.";
		Set<String> labels = new HashSet<>();
		labels.add("information_architecture");
		labels.add("security");
		
		boolean is_secure = page_state.isSecured();
		if(!is_secure) {
			String title = "Page isn't secure";
			String description = page_state.getUrl() + " doesn't use https";
			String wcag_compliance = "";
			String recommendation = "Enable encryption(SSL) for your site by getting a signed certificate from a certificate authority and enabling ssl on the server that hosts your website.";
			//Set<Recommendation> recommendations = new HashSet<>();
			//recommendations.add(new Recommendation(recommendation));
			
			UXIssueMessage ux_issue = new UXIssueMessage(Priority.HIGH,
														description,
														ObservationType.SECURITY,
														AuditCategory.INFORMATION_ARCHITECTURE,
														wcag_compliance,
														labels,
														why_it_matters,
														title,
														0, 
														1, 
														recommendation);
			
			issue_messages.add(issue_message_service.save(ux_issue));
		}
		else {
			String title = "Page is secure";
			String description = page_state.getUrl() + " uses https protocol to provide a secure connection";
			String wcag_compliance = "";
			String recommendation = "";
			//Set<Recommendation> recommendations = new HashSet<>();
			//recommendations.add(new Recommendation(recommendation));
			
			UXIssueMessage ux_issue = new UXIssueMessage(Priority.NONE,
														description,
														ObservationType.SECURITY,
														AuditCategory.INFORMATION_ARCHITECTURE,
														wcag_compliance,
														labels,
														why_it_matters,
														title,
														1, 
														1, 
														recommendation);

			issue_messages.add(issue_message_service.save(ux_issue));
		}
		
		String description = "";
		
		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			points_earned += issue_msg.getPoints();
			max_points += issue_msg.getMaxPoints();
		}
		
		Audit audit = new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
								AuditSubcategory.SECURITY,
								AuditName.ENCRYPTED,
								points_earned,
								issue_messages,
								AuditLevel.PAGE,
								max_points,
								page_state.getUrl(),
								why_it_matters,
								description,
								false);
		
		return audit_service.save(audit);
	}
	

	public static List<String> makeDistinct(List<String> from){
		return from.stream().distinct().sorted().collect(Collectors.toList());
	}
}