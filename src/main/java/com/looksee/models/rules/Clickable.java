package com.looksee.models.rules;

import com.looksee.audit.informationArchitecture.models.Element;

public class Clickable extends Rule {	
	public Clickable(){
		setType(RuleType.CLICKABLE);
		setValue("");
		setKey(generateKey());
	}

	@Override
	public Boolean evaluate(Element val) {
		assert false;
		// TODO Auto-generated method stub
		return null;
	}
}