package com.looksee.informationArchitectureAudit.models.recommend;

import java.util.UUID;

import com.looksee.audit.informationArchitecture.models.LookseeObject;


public class Recommendation extends LookseeObject{
	private String description;

	public Recommendation() { }
	
	public Recommendation(String description) {
		setDescription(description);
	}

	@Override
	public String generateKey() {
		return "recommendation::"+UUID.randomUUID();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}