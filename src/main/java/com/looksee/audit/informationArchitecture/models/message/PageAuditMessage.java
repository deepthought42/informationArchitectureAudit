package com.looksee.audit.informationArchitecture.models.message;

import lombok.Getter;
import lombok.Setter;

public class PageAuditMessage extends Message {
	@Getter
	@Setter
	private long pageAuditId;

	@Getter
	@Setter
	private long pageId;
	
	public PageAuditMessage() {}
	
	public PageAuditMessage(long account_id,
							long page_audit_id,
							long page_id
	) {
		super(account_id);
		setPageAuditId(page_audit_id);
		setPageId(page_id);
	}
}
