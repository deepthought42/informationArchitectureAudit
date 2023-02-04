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
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.audit.informationArchitecture.gcp.PubSubAuditUpdatePublisherImpl;
import com.looksee.audit.informationArchitecture.gcp.PubSubErrorPublisherImpl;
import com.looksee.audit.informationArchitecture.mapper.Body;
import com.looksee.audit.informationArchitecture.models.Audit;
import com.looksee.audit.informationArchitecture.models.AuditRecord;
import com.looksee.audit.informationArchitecture.models.LinksAudit;
import com.looksee.audit.informationArchitecture.models.MetadataAudit;
import com.looksee.audit.informationArchitecture.models.PageState;
import com.looksee.audit.informationArchitecture.models.SecurityAudit;
import com.looksee.audit.informationArchitecture.models.TitleAndHeaderAudit;
import com.looksee.audit.informationArchitecture.models.enums.AuditCategory;
import com.looksee.audit.informationArchitecture.models.enums.AuditLevel;
import com.looksee.audit.informationArchitecture.models.message.AuditError;
import com.looksee.audit.informationArchitecture.models.message.AuditProgressUpdate;
import com.looksee.audit.informationArchitecture.models.message.PageAuditMessage;
import com.looksee.audit.informationArchitecture.services.AuditRecordService;

// PubsubController consumes a Pub/Sub message.
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private PubSubAuditUpdatePublisherImpl audit_update_topic;
	
	@Autowired
	private PubSubErrorPublisherImpl pubSubErrorPublisherImpl;

	@Autowired
	private LinksAudit links_auditor;
	
	@Autowired
	private MetadataAudit metadata_auditor;

	@Autowired
	private TitleAndHeaderAudit title_and_header_auditor;

	@Autowired
	private SecurityAudit security_auditor;
	
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
	    
	    JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
	    
    	AuditRecord audit_record = audit_record_service.findById(audit_record_msg.getPageAuditId()).get();
	  
    	log.warn("audit record id : " + audit_record.getId());
    	PageState page = audit_record_service.getPageStateForAuditRecord(audit_record.getId());
    	//generate audit report
		
    	AuditProgressUpdate audit_update = new AuditProgressUpdate(
												audit_record_msg.getAccountId(),
												audit_record_msg.getDomainAuditRecordId(),
												(1.0/5.0),
												"Reviewing links",
												AuditCategory.INFORMATION_ARCHITECTURE,
												AuditLevel.PAGE, 
												audit_record_msg.getDomainId(),
												audit_record_msg.getPageAuditId());

	  	
    	String audit_record_json = mapper.writeValueAsString(audit_update);
    	audit_update_topic.publish(audit_record_json);
	  
    	try {
    		Audit link_audit = links_auditor.execute(page, audit_record, null);
	   		
    		AuditProgressUpdate audit_update2 = new AuditProgressUpdate(
													audit_record_msg.getAccountId(),
													audit_record_msg.getDomainAuditRecordId(),
													(2.0/5.0),
													"Reviewing title and header page title and header",
													AuditCategory.INFORMATION_ARCHITECTURE,
													AuditLevel.PAGE, 
													audit_record_msg.getDomainId(),
													audit_record_msg.getPageAuditId());
	
    		audit_record_service.addAudit(audit_record_msg.getPageAuditId(), link_audit.getId());
    		audit_record_json = mapper.writeValueAsString(audit_update2);
			
    		audit_update_topic.publish(audit_record_json);	
    	} 
    	catch(Exception e) {
    		AuditError audit_err = new AuditError(audit_record_msg.getAccountId(), 
												audit_record_msg.getDomainAuditRecordId(),
				  								"An error occurred while reviewing title and header", 
				  								AuditCategory.INFORMATION_ARCHITECTURE, 
				  								(2.0/5.0),
				  								audit_record_msg.getDomainId());
		
    		e.printStackTrace();
    		audit_record_json = mapper.writeValueAsString(audit_err);
    		pubSubErrorPublisherImpl.publish(audit_record_json);
    	}
	
    	try {
    		Audit title_and_headers = title_and_header_auditor.execute(page, audit_record, null);
		
    		AuditProgressUpdate audit_update3 = new AuditProgressUpdate(
													audit_record_msg.getAccountId(),
													audit_record_msg.getDomainAuditRecordId(),
													(3.0/5.0),
													"Checking that page is secure",
													AuditCategory.INFORMATION_ARCHITECTURE,
													AuditLevel.PAGE, 
													audit_record_msg.getDomainId(),
													audit_record_msg.getPageAuditId());

    		audit_record_service.addAudit(audit_record_msg.getPageAuditId(), title_and_headers.getId());
    		audit_record_json = mapper.writeValueAsString(audit_update3);
			
    		audit_update_topic.publish(audit_record_json);
    	} 
    	catch(Exception e) {
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
	
    	try {
    		Audit security_audit = security_auditor.execute(page, audit_record, null);
		
    		AuditProgressUpdate audit_update4 = new AuditProgressUpdate(
													audit_record_msg.getAccountId(),
													audit_record_msg.getDomainAuditRecordId(),
													(4.0/5.0),
													"Reviewing SEO",
													AuditCategory.INFORMATION_ARCHITECTURE,
													AuditLevel.PAGE, 
													audit_record_msg.getDomainId(),
													audit_record_msg.getPageAuditId());
		
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), security_audit.getId());
			audit_record_json = mapper.writeValueAsString(audit_update4);
			
			audit_update_topic.publish(audit_record_json);
    	} 
    	catch(Exception e) {
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
	
		try {
			Audit metadata = metadata_auditor.execute(page, audit_record, null);
			
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
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), metadata.getId());
			audit_record_json = mapper.writeValueAsString(audit_update5);
				
			audit_update_topic.publish(audit_record_json);
		}
		catch(Exception e) {
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
	  
    return new ResponseEntity<String>("Successfully audited information architecture", HttpStatus.OK);
  }
  
}