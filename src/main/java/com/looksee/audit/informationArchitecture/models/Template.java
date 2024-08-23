package com.looksee.audit.informationArchitecture.models;

import org.springframework.data.neo4j.core.schema.Node;

import com.looksee.audit.informationArchitecture.models.enums.TemplateType;



/**
 * 		A Template is defined as a semi-generic string that matches a set of Elements
 */
@Node
public class Template extends LookseeObject {

	private String type;
	private String template;
	
	
	public Template(){
		setType(TemplateType.UNKNOWN);
		setTemplate("");
		setKey(generateKey());
	}
	
	public Template(TemplateType type, String template){
		setType(type);
		setTemplate(template);
		setKey(generateKey());
	}
	
	@Override
	public String generateKey() {
		return type+org.apache.commons.codec.digest.DigestUtils.sha256Hex(template);
	}

	public TemplateType getType() {
		return TemplateType.create(type);
	}

	public void setType(TemplateType type) {
		this.type = type.toString();
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}
}
