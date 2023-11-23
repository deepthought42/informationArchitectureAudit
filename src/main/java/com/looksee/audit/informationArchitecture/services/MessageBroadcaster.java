package com.looksee.audit.informationArchitecture.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.audit.informationArchitecture.models.dto.DomainDto;
import com.looksee.audit.informationArchitecture.models.dto.PageAuditDto;
import com.pusher.rest.Pusher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Defines methods for emitting data to subscribed clients
 */
@Component
public class MessageBroadcaster {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(MessageBroadcaster.class);
	
	private Pusher pusher;
	
	public MessageBroadcaster(@Value( "${pusher.appId}" ) String app_id,
			@Value( "${pusher.key}" ) String key,
			@Value( "${pusher.secret}" ) String secret,
			@Value("${pusher.cluster}") String cluster) {
		pusher = new Pusher(app_id, key, secret);
		pusher.setCluster(cluster);
		pusher.setEncrypted(true);
	}
	
	/**
	 * send {@link AuditRecord} to the users pusher channel
	 * @param account_id
	 * @param audit
	 */
	public void sendAuditRecord(String user_id, DomainDto domain_dto) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

		String domain_dto_json = mapper.writeValueAsString(domain_dto);
		pusher.trigger(user_id, "audit-record", domain_dto_json);
	}
	
	/**
	 * Sends {@linkplain PageAuditRecord} to user via Pusher
	 * @param account_id
	 * @param audit_record
	 * @throws JsonProcessingException
	 */
	public void sendAuditUpdate(String account_id, PageAuditDto audit_record) throws JsonProcessingException {
		assert account_id != null;
		assert !account_id.isEmpty();
		assert audit_record != null;
		
		log.warn("Sending page audit record to user");
		ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String audit_record_json = mapper.writeValueAsString(audit_record);
		pusher.trigger(account_id, "audit-record", audit_record_json);
	}
}
