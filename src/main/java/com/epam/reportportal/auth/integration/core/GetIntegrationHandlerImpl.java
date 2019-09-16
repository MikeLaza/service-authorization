/*
 * Copyright 2019 EPAM Systems
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

package com.epam.reportportal.auth.integration.core;

import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.IntegrationResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.epam.reportportal.auth.integration.converter.IntegrationConverter.TO_INTEGRATION_RESOURCE;

/**
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Service
public class GetIntegrationHandlerImpl implements GetIntegrationHandler {

	private final IntegrationRepository integrationRepository;
	private final IntegrationTypeRepository integrationTypeRepository;

	@Autowired
	public GetIntegrationHandlerImpl(IntegrationRepository integrationRepository, IntegrationTypeRepository integrationTypeRepository) {
		this.integrationRepository = integrationRepository;
		this.integrationTypeRepository = integrationTypeRepository;
	}

	@Override
	public IntegrationResource getGlobalIntegrationById(Long integrationId) {
		Integration integration = integrationRepository.findGlobalById(integrationId)
				.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND, integrationId));
		return TO_INTEGRATION_RESOURCE.apply(integration);
	}

	@Override
	public List<IntegrationResource> getGlobalIntegrations() {
		return integrationRepository.findAllGlobal().stream().map(TO_INTEGRATION_RESOURCE).collect(Collectors.toList());
	}

	@Override
	public List<IntegrationResource> getGlobalIntegrations(String pluginName) {
		IntegrationType integrationType = integrationTypeRepository.findByName(pluginName)
				.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND, pluginName));
		return integrationRepository.findAllGlobalByType(integrationType)
				.stream()
				.map(TO_INTEGRATION_RESOURCE)
				.collect(Collectors.toList());
	}
}
