package com.looksee.audit.informationArchitecture.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
public class GenericIssue {
    @Getter
    @Setter
    private String description;

    @Getter
    @Setter
    private String title;

    @Getter
    @Setter
    private String cssSelector;

    @Getter
    @Setter
    private String recommendation;
}
