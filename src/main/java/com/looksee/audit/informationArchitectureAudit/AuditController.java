package com.looksee.audit.informationArchitectureAudit;

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
import com.looksee.audit.informationArchitecture.mapper.Body;
import com.looksee.audit.informationArchitecture.models.Audit;
import com.looksee.audit.informationArchitecture.models.AuditProgressUpdate;
import com.looksee.audit.informationArchitecture.models.AuditRecord;
import com.looksee.audit.informationArchitecture.models.PageState;
import com.looksee.audit.informationArchitecture.models.enums.AuditCategory;
import com.looksee.audit.informationArchitecture.models.enums.AuditLevel;
import com.looksee.audit.informationArchitecture.services.AuditRecordService;
import com.looksee.audit.informationArchitecture.services.PageStateService;
import com.looksee.audit.informationArchitectureAudit.gcp.PubSubAuditRecordPublisherImpl;
import com.looksee.audit.informationArchitectureAudit.gcp.PubSubErrorPublisherImpl;
import com.looksee.audit.informationArchitectureAudit.models.message.AuditError;
import com.looksee.audit.informationArchitectureAudit.models.message.PageAuditMessage;
import com.looksee.models.audit.informationarchitecture.LinksAudit;
import com.looksee.models.audit.informationarchitecture.MetadataAudit;
import com.looksee.models.audit.informationarchitecture.SecurityAudit;
import com.looksee.models.audit.informationarchitecture.TitleAndHeaderAudit;
// PubsubController consumes a Pub/Sub message.
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private PubSubAuditRecordPublisherImpl pubSubPageAuditPublisherImpl;
	
	@Autowired
	private PubSubErrorPublisherImpl pubSubErrorPublisherImpl;

	@Autowired
	private LinksAudit links_auditor;
	
	@Autowired
	private MetadataAudit metadata_auditor;

	@Autowired
	private TitleAndHeaderAudit title_and_header_auditor;

	@Autowired
	private SecurityAudit security_audit;
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity receiveMessage(@RequestBody Body body) 
			throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException 
	{
	  log.warn("body :: "+body);
	  // Get PubSub message from request body.
	  Body.Message message = body.getMessage();
	  log.warn("message " + message);
	    /*
	    if (message == null) {
	      String msg = "Bad Request: invalid Pub/Sub message format";
	      System.out.println(msg);
	      return new ResponseEntity(msg, HttpStatus.BAD_REQUEST);
	    }
	*/
	  String data = message.getData();
	  log.warn("data :: "+data);
	  //retrieve audit record and determine type of audit record
    
	  byte[] decodedBytes = Base64.getUrlDecoder().decode(data);
	  String decoded_json = new String(decodedBytes);

	  //create ObjectMapper instance
	  ObjectMapper objectMapper = new ObjectMapper();
    
	  //convert json string to object
	  PageAuditMessage audit_record_msg = objectMapper.readValue(decoded_json, PageAuditMessage.class);
	    
	  JsonMapper mapper = new JsonMapper().builder().addModule(new JavaTimeModule()).build();;

	  try {

		  AuditRecord audit_record = audit_record_service.findById(audit_record_msg.getPageAuditId()).get();
		  PageState page = page_state_service.findById(audit_record.getId()).get();
		  //AuditRecord audit_record = audit_record_service.findById(audit_record_msg.getPageAuditId()).get();
		  //PageState page = audit_record_msg.getPageState(); //audit_record_service.getPageStateForAuditRecord(audit_record.getId());
		  //generate audit report
			
		  AuditProgressUpdate audit_update = new AuditProgressUpdate(
													audit_record_msg.getAccountId(),
													audit_record.getId(),
													(1.0/5.0),
													"Reviewing links",
													AuditCategory.INFORMATION_ARCHITECTURE,
													AuditLevel.PAGE, 
													audit_record_msg.getDomainId());

		  	
		  String audit_record_json = mapper.writeValueAsString(audit_update);
		  pubSubPageAuditPublisherImpl.publish(audit_record_json);
		  
		  try {
			  Audit link_audit = links_auditor.execute(page, audit_record, null);
		   		
			  AuditProgressUpdate audit_update2 = new AuditProgressUpdate(
														audit_record_msg.getAccountId(),
														audit_record.getId(),
														(2.0/5.0),
														"Reviewing title and header page title and header",
														AuditCategory.INFORMATION_ARCHITECTURE,
														AuditLevel.PAGE, 
														audit_record_msg.getDomainId());
		
			  audit_record_service.addAudit(audit_record_msg.getPageAuditId(), link_audit.getId());
			  audit_record_json = mapper.writeValueAsString(audit_update2);
				
			  pubSubPageAuditPublisherImpl.publish(audit_record_json);	
		  } 
		  catch(Exception e) {
			  AuditError audit_err = new AuditError(audit_record_msg.getAccountId(), 
					  								audit_record_msg.getPageAuditId(), 
					  								"An error occurred while reviewing title and header", 
					  								AuditCategory.INFORMATION_ARCHITECTURE, 
					  								(2.0/5.0),
					  								audit_record_msg.getDomainId());
			
			//getContext().getParent().tell(audit_err, getSelf());
			e.printStackTrace();
			audit_record_json = mapper.writeValueAsString(audit_err);
			pubSubErrorPublisherImpl.publish(audit_record_json);
		}
		
		try {
			Audit title_and_headers = title_and_header_auditor.execute(page, audit_record, null);
			
			AuditProgressUpdate audit_update3 = new AuditProgressUpdate(
														audit_record_msg.getAccountId(),
														audit_record.getId(),
														(3.0/5.0),
														"Checking that page is secure",
														AuditCategory.INFORMATION_ARCHITECTURE,
														AuditLevel.PAGE, 
														audit_record_msg.getDomainId());

			  audit_record_service.addAudit(audit_record_msg.getPageAuditId(), title_and_headers.getId());
			  audit_record_json = mapper.writeValueAsString(audit_update3);
				
			  pubSubPageAuditPublisherImpl.publish(audit_record_json);
			
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
			Audit security = security_audit.execute(page, audit_record, null);
			
			AuditProgressUpdate audit_update4 = new AuditProgressUpdate(
														audit_record_msg.getAccountId(),
														audit_record.getId(),
														(4.0/5.0),
														"Reviewing SEO",
														AuditCategory.INFORMATION_ARCHITECTURE,
														AuditLevel.PAGE, 
														audit_record_msg.getDomainId());
			
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), security.getId());
			audit_record_json = mapper.writeValueAsString(audit_update4);
			
			pubSubPageAuditPublisherImpl.publish(audit_record_json);
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
														audit_record.getId(),
														1.0,
														"Completed information architecture audit",
														AuditCategory.INFORMATION_ARCHITECTURE,
														AuditLevel.PAGE, 
														audit_record_msg.getDomainId());
			
			//getSender().tell(audit_update5, getSelf());
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), metadata.getId());
			audit_record_json = mapper.writeValueAsString(audit_update5);
				
			pubSubPageAuditPublisherImpl.publish(audit_record_json);
		}
		catch(Exception e) {
			AuditError audit_err = new AuditError(audit_record_msg.getAccountId(), 
												  audit_record_msg.getPageAuditId(), 
												  "An error occurred while reviewing metadata", 
												  AuditCategory.INFORMATION_ARCHITECTURE, 
												  1.0,
												  audit_record_msg.getDomainId());
			
			audit_record_json = mapper.writeValueAsString(audit_err);
			
			pubSubErrorPublisherImpl.publish(audit_record_json);
			e.printStackTrace();
		}
	} catch(Exception e) {
		log.warn("exception caught during Information Architecture audit");
		e.printStackTrace();
		log.warn("-------------------------------------------------------------");
		log.warn("-------------------------------------------------------------");
		log.warn("THERE WAS AN ISSUE DURING INFO ARCHITECTURE AUDIT");
		log.warn("-------------------------------------------------------------");
		log.warn("-------------------------------------------------------------");
		
	}
	finally {

		AuditProgressUpdate audit_update5 = new AuditProgressUpdate(
														audit_record_msg.getAccountId(),
														audit_record_msg.getPageAuditId(),
														1.0,
														"Completed information architecture audit",
														AuditCategory.INFORMATION_ARCHITECTURE,
														AuditLevel.PAGE,
														audit_record_msg.getDomainId());
		
		String audit_record_json = mapper.writeValueAsString(audit_update5);
		pubSubPageAuditPublisherImpl.publish(audit_record_json);
	}
	  
    return new ResponseEntity("Successfully sent message to audit manager", HttpStatus.OK);
    
    /*
    String target =
        !StringUtils.isEmpty(data) ? new String(Base64.getDecoder().decode(data)) : "World";
    String msg = "Hello " + target + "!";

    System.out.println(msg);
    return new ResponseEntity(msg, HttpStatus.OK);
    */
  }
  /*
  public void publishMessage(String messageId, Map<String, String> attributeMap, String message) throws ExecutionException, InterruptedException {
      log.info("Sending Message to the topic:::");
      PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
              .putAllAttributes(attributeMap)
              .setData(ByteString.copyFromUtf8(message))
              .setMessageId(messageId)
              .build();

      pubSubPublisherImpl.publish(pubsubMessage);
  }
  */
}
// [END run_pubsub_handler]
// [END cloudrun_pubsub_handler]