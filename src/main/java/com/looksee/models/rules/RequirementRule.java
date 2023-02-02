package com.looksee.models.rules;

import com.looksee.audit.informationArchitecture.models.Element;

public class RequirementRule extends Rule{
	/**
	 * Constructs Rule
	 */
	public RequirementRule(){
		setValue("");
		setType(RuleType.REQUIRED);
		this.setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(Element elem) {
		return elem.getAttributes().containsKey("required");
	}
}
