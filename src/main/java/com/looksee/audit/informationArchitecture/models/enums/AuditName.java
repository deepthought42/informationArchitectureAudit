package com.looksee.audit.informationArchitecture.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.looksee.audit.informationArchitecture.models.Audit;

/**
 * Defines all types of {@link Audit audits} that exist in the system
 */
public enum AuditName {
	//color management
	COLOR_PALETTE("COLOR_PALETTE"),
	TEXT_BACKGROUND_CONTRAST("TEXT_BACKGROUND_CONTRAST"),
	NON_TEXT_BACKGROUND_CONTRAST("NON_TEXT_BACKGROUND_CONTRAST"),
	LINKS("LINKS"),
	TYPEFACES("TYPEFACES"),
	FONT("FONT"),
	PADDING("PADDING"),
	MARGIN("MARGIN"),
	MEASURE_UNITS("MEASURE_UNITS"),
	TITLES("TITLES"),
	ALT_TEXT("ALT_TEXT"),
	PARAGRAPHING("PARAGRAPHING"),
	METADATA("METADATA"),
	UNKNOWN("UNKNOWN"),
	IMAGE_COPYRIGHT("IMAGE_COPYRIGHT"),
	IMAGE_POLICY("IMAGE_POLICY"),
	READING_COMPLEXITY("READING_COMPLEXITY"),
	ENCRYPTED("ENCRYPTED"), //
    HEADER_STRUCTURE("HEADER_STRUCTURE"),
	TABLE_STRUCTURE("TABLE_STRUCTURE"),
    FORM_STRUCTURE("FORM_STRUCTURE"),
    ORIENTATION("ORIENTATION"),
    INPUT_PURPOSE("INPUT_PURPOSE"),
    IDENTIFY_PURPOSE("IDENTIFY_PURPOSE"),
    USE_OF_COLOR("USE_OF_COLOR"),
    AUDIO_CONTROL("AUDIO_CONTROL"),
    REFLOW("REFLOW"),
    VISUAL_PRESENTATION("VISUAL_PRESENTATION"), 
    TEXT_SPACING("TEXT_SPACING"), 
    PAGE_LANGUAGE("PAGE_LANGUAGE");
    
	private String shortName;

    AuditName(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static AuditName create(String value) {
        if(value == null) {
            return UNKNOWN;
        }
        for(AuditName v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    public String getShortName() {
        return shortName;
    }
}
