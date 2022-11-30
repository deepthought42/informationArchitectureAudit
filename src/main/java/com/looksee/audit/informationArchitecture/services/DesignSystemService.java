package com.looksee.audit.informationArchitecture.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.audit.informationArchitecture.models.DesignSystem;
import com.looksee.audit.informationArchitecture.models.repository.DesignSystemRepository;

/**
 * Contains business logic for interacting with and managing accounts
 *
 */
@Service
public class DesignSystemService {

	@Autowired
	private DesignSystemRepository design_system_repo;

	public DesignSystem save(DesignSystem design_system) {
		return design_system_repo.save(design_system);
	}

	public Optional<DesignSystem> findById(long id) {
		return design_system_repo.findById(id);
	}
}
