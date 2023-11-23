package com.looksee.audit.informationArchitecture;

/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// [START cloudrun_pubsub_handler]
// [START run_pubsub_handler]
import java.util.Base64;
import java.util.HashSet;
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
import com.looksee.audit.informationArchitecture.mapper.Body;
import com.looksee.audit.informationArchitecture.models.Audit;
import com.looksee.audit.informationArchitecture.models.AuditRecord;
import com.looksee.audit.informationArchitecture.models.LinksAudit;
import com.looksee.audit.informationArchitecture.models.MetadataAudit;
import com.looksee.audit.informationArchitecture.models.PageAuditRecord;
import com.looksee.audit.informationArchitecture.models.PageState;
import com.looksee.audit.informationArchitecture.models.SecurityAudit;
import com.looksee.audit.informationArchitecture.models.TitleAndHeaderAudit;
import com.looksee.audit.informationArchitecture.models.dto.PageAuditDto;
import com.looksee.audit.informationArchitecture.models.enums.AuditCategory;
import com.looksee.audit.informationArchitecture.models.enums.AuditName;
import com.looksee.audit.informationArchitecture.models.enums.ExecutionStatus;
import com.looksee.audit.informationArchitecture.models.message.PageAuditMessage;
import com.looksee.audit.informationArchitecture.services.AuditRecordService;
import com.looksee.audit.informationArchitecture.services.MessageBroadcaster;
import com.looksee.audit.informationArchitecture.services.PageStateService;
import com.looksee.utils.AuditUtils;

// PubsubController consumes a Pub/Sub message.
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private LinksAudit links_auditor;
	
	@Autowired
	private MetadataAudit metadata_auditor;

	@Autowired
	private TitleAndHeaderAudit title_and_header_auditor;

	@Autowired
	private SecurityAudit security_auditor;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private MessageBroadcaster pusher;
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) 
			throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException 
	{
		Body.Message message = body.getMessage();
		String data = message.getData();
	    String target = !data.isEmpty() ? new String(Base64.getDecoder().decode(data)) : "";
        log.warn("page audit msg received = "+target);

	    ObjectMapper input_mapper = new ObjectMapper();
	    PageAuditMessage audit_record_msg = input_mapper.readValue(target, PageAuditMessage.class);
	    
    	AuditRecord audit_record = audit_record_service.findById(audit_record_msg.getPageAuditId()).get();
    	PageState page = page_state_service.getPageStateForAuditRecord(audit_record.getId());
    	
    	Set<Audit> audits = audit_record_service.getAllAudits(audit_record.getId());
    	
    	/*
    	AuditProgressUpdate audit_update = new AuditProgressUpdate(
												audit_record_msg.getAccountId(),
												audit_record_msg.getDomainAuditRecordId(),
												0.05,
												"Reviewing links",
												AuditCategory.INFORMATION_ARCHITECTURE,
												AuditLevel.PAGE, 
												audit_record_msg.getDomainId(),
												audit_record_msg.getPageAuditId());

	  	
    	String audit_record_json = mapper.writeValueAsString(audit_update);
    	audit_update_topic.publish(audit_record_json);
	    */
    	
    	//try {
    		if(!auditAlreadyExists(audits, AuditName.LINKS)) {    			
    			Audit link_audit = links_auditor.execute(page, audit_record, null);
    			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), link_audit.getId());
    		}

    		/*
    		AuditProgressUpdate audit_update2 = new AuditProgressUpdate(
													audit_record_msg.getAccountId(),
													audit_record_msg.getDomainAuditRecordId(),
													(2.0/5.0),
													"Reviewing title and header page title and header",
													AuditCategory.INFORMATION_ARCHITECTURE,
													AuditLevel.PAGE, 
													audit_record_msg.getDomainId(),
													audit_record_msg.getPageAuditId());
	
    		
    		audit_record_json = mapper.writeValueAsString(audit_update2);
			
    		audit_update_topic.publish(audit_record_json);
    	} 
    	catch(Exception e) {
    		 */
    		/*
    		AuditError audit_err = new AuditError(audit_record_msg.getAccountId(), 
												audit_record_msg.getDomainAuditRecordId(),
				  								"An error occurred while reviewing title and header", 
				  								AuditCategory.INFORMATION_ARCHITECTURE, 
				  								(2.0/5.0),
				  								audit_record_msg.getDomainId());
		
    		audit_record_json = mapper.writeValueAsString(audit_err);
    		pubSubErrorPublisherImpl.publish(audit_record_json);
    		
    		e.printStackTrace();
    	}
    		 */
	
//    	try {
    		if(!auditAlreadyExists(audits, AuditName.TITLES)) {    				
	    		Audit title_and_headers = title_and_header_auditor.execute(page, audit_record, null);
	    		audit_record_service.addAudit(audit_record_msg.getPageAuditId(), title_and_headers.getId());
    		}
    		/*
    		AuditProgressUpdate audit_update3 = new AuditProgressUpdate(
													audit_record_msg.getAccountId(),
													audit_record_msg.getDomainAuditRecordId(),
													(3.0/5.0),
													"Checking that page is secure",
													AuditCategory.INFORMATION_ARCHITECTURE,
													AuditLevel.PAGE, 
													audit_record_msg.getDomainId(),
													audit_record_msg.getPageAuditId());

    		audit_record_json = mapper.writeValueAsString(audit_update3);
			
    		audit_update_topic.publish(audit_record_json);
    	} 
    	catch(Exception e) {
    		 */
    		/*
    		AuditError audit_err = new AuditError(audit_record_msg.getAccountId(), 
											  audit_record_msg.getPageAuditId(), 
											  "An error occurred while reviewing page security", 
											  AuditCategory.INFORMATION_ARCHITECTURE, 
											  (3.0/5.0),
											  audit_record_msg.getDomainId());
		
    		audit_record_json = mapper.writeValueAsString(audit_err);
    		pubSubErrorPublisherImpl.publish(audit_record_json);
    		
    		e.printStackTrace();
    	}
    		 */
	
    	//try {
    		if(!auditAlreadyExists(audits, AuditName.ENCRYPTED)) {    			
	    		Audit security_audit = security_auditor.execute(page, audit_record, null);
	    		audit_record_service.addAudit(audit_record_msg.getPageAuditId(), security_audit.getId());
    		}
    		
    		/*
    		AuditProgressUpdate audit_update4 = new AuditProgressUpdate(
													audit_record_msg.getAccountId(),
													audit_record_msg.getDomainAuditRecordId(),
													(4.0/5.0),
													"Reviewing SEO",
													AuditCategory.INFORMATION_ARCHITECTURE,
													AuditLevel.PAGE, 
													audit_record_msg.getDomainId(),
													audit_record_msg.getPageAuditId());
		
			audit_record_json = mapper.writeValueAsString(audit_update4);
			audit_update_topic.publish(audit_record_json);
    	} 
    	catch(Exception e) {
    		 */
    		/*
			AuditError audit_err = new AuditError(audit_record_msg.getAccountId(), 
												  audit_record_msg.getPageAuditId(), 
												  "An error occurred while reviewing SEO", 
												  AuditCategory.INFORMATION_ARCHITECTURE, 
												  (4.0/5.0),
												  audit_record_msg.getDomainId());
			
			//getContext().getParent().tell(audit_err, getSelf());
			audit_record_json = mapper.writeValueAsString(audit_err);
			pubSubErrorPublisherImpl.publish(audit_record_json);
			e.printStackTrace();
    	}
    		 */
	
	//	try {
    		if(!auditAlreadyExists(audits, AuditName.METADATA)) {    			
				Audit metadata = metadata_auditor.execute(page, audit_record, null);
				audit_record_service.addAudit(audit_record_msg.getPageAuditId(), metadata.getId());
    		}
			/*
			AuditProgressUpdate audit_update5 = new AuditProgressUpdate(
														audit_record_msg.getAccountId(),
														audit_record_msg.getDomainAuditRecordId(),
														1.0,
														"Completed information architecture audit",
														AuditCategory.INFORMATION_ARCHITECTURE,
														AuditLevel.PAGE, 
														audit_record_msg.getDomainId(),
														audit_record_msg.getPageAuditId());
			
			//getSender().tell(audit_update5, getSelf());
			audit_record_json = mapper.writeValueAsString(audit_update5);
				
			audit_update_topic.publish(audit_record_json);
		}
		catch(Exception e) {
			 */
			/*
			AuditError audit_err = new AuditError(audit_record_msg.getAccountId(), 
												  audit_record_msg.getDomainAuditRecordId(),
												  "An error occurred while reviewing metadata", 
												  AuditCategory.INFORMATION_ARCHITECTURE, 
												  1.0,
												  audit_record_msg.getDomainId());
			
			audit_record_json = mapper.writeValueAsString(audit_err);
			
			pubSubErrorPublisherImpl.publish(audit_record_json);
			e.printStackTrace();
		}
			 */

		/*
		AuditProgressUpdate audit_update5 = new AuditProgressUpdate(
														audit_record_msg.getAccountId(),
														audit_record_msg.getDomainAuditRecordId(),
														1.0,
														"Completed information architecture audit",
														AuditCategory.INFORMATION_ARCHITECTURE,
														AuditLevel.PAGE,
														audit_record_msg.getDomainId(),
														audit_record_msg.getPageAuditId());
		
		audit_record_json = mapper.writeValueAsString(audit_update5);
		audit_update_topic.publish(audit_record_json);
	  */
 //   PageAuditRecord audit_record = (PageAuditRecord) audit_record_service.findById(audit_record_msg.getPageAuditId()).get();
	PageAuditDto audit_dto = builPagedAuditdDto(audit_record_msg.getPageAuditId(), page.getUrl());
	pusher.sendAuditUpdate(Long.toString( audit_record_msg.getAccountId() ), audit_dto);
    return new ResponseEntity<String>("Successfully audited information architecture", HttpStatus.OK);
  }

	/**
	 * Creates an {@linkplain PageAuditDto} using page audit ID and the provided page_url
	 * @param pageAuditId
	 * @param page_url
	 * @return
	 */
	private PageAuditDto builPagedAuditdDto(long pageAuditId, String page_url) {
		//get all audits
		Set<Audit> audits = audit_record_service.getAllAudits(pageAuditId);
		Set<AuditName> audit_labels = new HashSet<AuditName>();
		audit_labels.add(AuditName.TEXT_BACKGROUND_CONTRAST);
		audit_labels.add(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
		audit_labels.add(AuditName.TITLES);
		audit_labels.add(AuditName.IMAGE_COPYRIGHT);
		audit_labels.add(AuditName.IMAGE_POLICY);
		audit_labels.add(AuditName.LINKS);
		audit_labels.add(AuditName.ALT_TEXT);
		audit_labels.add(AuditName.METADATA);
		audit_labels.add(AuditName.READING_COMPLEXITY);
		audit_labels.add(AuditName.PARAGRAPHING);
		audit_labels.add(AuditName.ENCRYPTED);
		//count audits for each category
		//calculate content score
		//calculate aesthetics score
		//calculate information architecture score
		double visual_design_progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS, 
																 1, 
																 audits, 
																 AuditUtils.getAuditLabels(AuditCategory.AESTHETICS, audit_labels));
		double content_progress = AuditUtils.calculateProgress(AuditCategory.CONTENT, 
																1, 
																audits, 
																audit_labels);
		double info_architecture_progress = AuditUtils.calculateProgress(AuditCategory.INFORMATION_ARCHITECTURE, 
																		1, 
																		audits, 
																		audit_labels);

		double content_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.CONTENT);
		double info_architecture_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.INFORMATION_ARCHITECTURE);
		double visual_design_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.AESTHETICS);
		double a11y_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.ACCESSIBILITY);

		double data_extraction_progress = 1;
		String message = "";
		ExecutionStatus execution_status = ExecutionStatus.UNKNOWN;
		if(visual_design_progress < 1 || content_progress < 1 || visual_design_progress < 1) {
			execution_status = ExecutionStatus.IN_PROGRESS;
		}
		else {
			execution_status = ExecutionStatus.COMPLETE;
		}
		
		return new PageAuditDto(pageAuditId, 
								page_url, 
								content_score, 
								content_progress, 
								info_architecture_score, 
								info_architecture_progress, 
								a11y_score,
								visual_design_score,
								visual_design_progress,
								data_extraction_progress, 
								message, 
								execution_status);
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