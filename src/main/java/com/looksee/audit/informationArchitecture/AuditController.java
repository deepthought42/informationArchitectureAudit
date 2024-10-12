package com.looksee.audit.informationArchitecture;

import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.audit.informationArchitecture.gcp.PubSubAuditUpdatePublisherImpl;
import com.looksee.audit.informationArchitecture.mapper.Body;
import com.looksee.audit.informationArchitecture.models.AudioControlAudit;
import com.looksee.audit.informationArchitecture.models.Audit;
import com.looksee.audit.informationArchitecture.models.AuditRecord;
import com.looksee.audit.informationArchitecture.models.FormStructureAudit;
import com.looksee.audit.informationArchitecture.models.HeaderStructureAudit;
import com.looksee.audit.informationArchitecture.models.IdentifyPurposeAudit;
import com.looksee.audit.informationArchitecture.models.InputPurposeAudit;
import com.looksee.audit.informationArchitecture.models.LinksAudit;
import com.looksee.audit.informationArchitecture.models.MetadataAudit;
import com.looksee.audit.informationArchitecture.models.OrientationAudit;
import com.looksee.audit.informationArchitecture.models.PageLanguageAudit;
import com.looksee.audit.informationArchitecture.models.PageState;
import com.looksee.audit.informationArchitecture.models.ReflowAudit;
import com.looksee.audit.informationArchitecture.models.SecurityAudit;
import com.looksee.audit.informationArchitecture.models.TableStructureAudit;
import com.looksee.audit.informationArchitecture.models.TextSpacingAudit;
import com.looksee.audit.informationArchitecture.models.TitleAndHeaderAudit;
import com.looksee.audit.informationArchitecture.models.UseOfColorAudit;
import com.looksee.audit.informationArchitecture.models.VisualPresentationAudit;
import com.looksee.audit.informationArchitecture.models.enums.AuditCategory;
import com.looksee.audit.informationArchitecture.models.enums.AuditLevel;
import com.looksee.audit.informationArchitecture.models.enums.AuditName;
import com.looksee.audit.informationArchitecture.models.message.AuditProgressUpdate;
import com.looksee.audit.informationArchitecture.models.message.PageAuditMessage;
import com.looksee.audit.informationArchitecture.services.AuditRecordService;
import com.looksee.audit.informationArchitecture.services.PageStateService;

@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private HeaderStructureAudit header_structure_auditor;

	@Autowired
	private TableStructureAudit table_structure_auditor;
	
	@Autowired
	private FormStructureAudit form_structure_auditor;

	@Autowired
	private OrientationAudit orientationAudit;

	@Autowired
	private InputPurposeAudit inputPurposeAudit;

	@Autowired
	private IdentifyPurposeAudit identifyPurposeAudit;

	@Autowired
	private UseOfColorAudit useOfColorAudit;

	@Autowired
	private ReflowAudit reflowAudit;

	@Autowired
	private LinksAudit links_auditor;
	
	@Autowired
	private AudioControlAudit audioControlAudit;

	@Autowired
	private VisualPresentationAudit visualPresentationAudit;

	@Autowired
	private PageLanguageAudit pageLanguageAudit;

	@Autowired
	private MetadataAudit metadata_auditor;

	@Autowired
	private TitleAndHeaderAudit title_and_header_auditor;

	@Autowired
	private TextSpacingAudit textSpacingAudit;

	@Autowired
	private SecurityAudit security_auditor;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private PubSubAuditUpdatePublisherImpl audit_update_topic;
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body)
			throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException 
	{
		Body.Message message = body.getMessage();
		String data = message.getData();
	    String target = !data.isEmpty() ? new String(Base64.getDecoder().decode(data)) : "";
        log.warn("page audit msg received = "+target);

	    ObjectMapper mapper = new ObjectMapper();
	    PageAuditMessage audit_record_msg = mapper.readValue(target, PageAuditMessage.class);
	    
    	AuditRecord audit_record = audit_record_service.findById(audit_record_msg.getPageAuditId()).get();
    	PageState page = page_state_service.getPageStateForAuditRecord(audit_record.getId());
    	
    	Set<Audit> audits = audit_record_service.getAllAudits(audit_record.getId());
    	
		//WCAG 2.1 Section 1.3.1 - Structure (headers)
		if(!auditAlreadyExists(audits, AuditName.HEADER_STRUCTURE)) {
			Audit header_structure_audit = header_structure_auditor.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), header_structure_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.TABLE_STRUCTURE)) {
			Audit table_structure_audit = table_structure_auditor.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), table_structure_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.FORM_STRUCTURE)) {
			Audit form_structure_audit = form_structure_auditor.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), form_structure_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.ORIENTATION)) {
			Audit orientation_audit = orientationAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), orientation_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.INPUT_PURPOSE)) {
			Audit input_purpose_audit = inputPurposeAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), input_purpose_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.IDENTIFY_PURPOSE)) {
			Audit identify_purpose_audit = identifyPurposeAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), identify_purpose_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.USE_OF_COLOR)) {
			Audit use_of_color_audit = useOfColorAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), use_of_color_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.AUDIO_CONTROL)) {
			Audit audio_control_audit = audioControlAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), audio_control_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.VISUAL_PRESENTATION)) {
			Audit visual_presentation_audit = visualPresentationAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), visual_presentation_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.REFLOW)) {
			Audit reflow_audit = reflowAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), reflow_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.TEXT_SPACING)) {
			Audit text_spacing_audit = textSpacingAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), text_spacing_audit.getId());
		}

		/*********************************
			WCAG 2.1 SECTION 3
		**********************************/
		if(!auditAlreadyExists(audits, AuditName.PAGE_LANGUAGE)) {
			Audit page_language_audit = pageLanguageAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), page_language_audit.getId());
		}


		//************************************************
		//Original UX audits section
		//*************************************************

		if(!auditAlreadyExists(audits, AuditName.LINKS)) {
			Audit link_audit = links_auditor.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), link_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.TITLES)) {
    		Audit title_and_headers = title_and_header_auditor.execute(page, audit_record, null);
    		audit_record_service.addAudit(audit_record_msg.getPageAuditId(), title_and_headers.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.ENCRYPTED)) {
			Audit security_audit = security_auditor.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), security_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.METADATA)) {
			Audit metadata = metadata_auditor.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), metadata.getId());
		}
		
		AuditProgressUpdate audit_update = new AuditProgressUpdate(audit_record_msg.getAccountId(),
															1.0,
															"Completed information architecture audit",
																	AuditCategory.INFORMATION_ARCHITECTURE, 
																	AuditLevel.PAGE,
																	audit_record_msg.getPageAuditId());

		String audit_record_json = mapper.writeValueAsString(audit_update);
		audit_update_topic.publish(audit_record_json);
		
		return new ResponseEntity<String>("Successfully audited information architecture", HttpStatus.OK);
	}

	/**
	 * Checks if the any of the provided {@link Audit audits} have a name that matches 
	 * 		the provided {@linkplain AuditName}
	 * 
	 * @param audits
	 * @param audit_name
	 * 
	 * @return
	 * 
	 * @pre audits != null
	 * @pre audit_name != null
	 */
	private boolean auditAlreadyExists(Set<Audit> audits, AuditName audit_name) {
		assert audits != null;
		assert audit_name != null;
		
		for(Audit audit : audits) {
			if(audit_name.equals(audit.getName())) {
				return true;
			}
		}
		return false;
	}
  
}