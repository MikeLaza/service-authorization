package com.epam.reportportal.auth.integration.parameter;

import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationParams;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.epam.ta.reportportal.commons.validation.Suppliers.formattedSupplier;
import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public enum LdapParameter {
	NAME("name", false, false),
	URL("url", true, false),
	BASE_DN("baseDn", true, false),
	EMAIL_ATTRIBUTE("email", true, true),
	FULL_NAME_ATTRIBUTE("fullName", false, true),
	PHOTO_ATTRIBUTE("photo", false, true),
	USER_DN_PATTERN("userDnPattern", false, false),
	USER_SEARCH_FILTER("userSearchFilter", false, false),
	GROUP_SEARCH_BASE("groupSearchBase", false, false),
	GROUP_SEARCH_FILTER("groupSearchFilter", false, false),
	PASSWORD_ENCODER_TYPE("passwordEncoderType", false, false),
	PASSWORD_ATTRIBUTE("passwordAttribute", false, false),
	MANAGER_DN("managerDn", false, false),
	MANAGER_PASSWORD("managerPassword", false, false),
	DOMAIN("domain", false, false);

	private String parameterName;

	private boolean required;

	private boolean syncAttribute;

	LdapParameter(String parameterName, boolean required, boolean syncAttribute) {
		this.parameterName = parameterName;
		this.required = required;
		this.syncAttribute = syncAttribute;
	}

	public String getParameterName() {
		return parameterName;
	}

	public boolean isRequired() {
		return required;
	}

	public boolean isSyncAttribute() {
		return syncAttribute;
	}

	public Optional<String> getParameter(Integration integration) {
		return ofNullable((String) integration.getParams().getParams().get(parameterName));
	}

	public void setParameter(Integration integration, String value) {
		if (Objects.isNull(integration.getParams())) {
			integration.setParams(new IntegrationParams(new HashMap<>()));
		}
		if (Objects.isNull(integration.getParams().getParams())) {
			integration.getParams().setParams(new HashMap<>());
		}
		integration.getParams().getParams().put(parameterName, value);
	}

	public boolean exist(Integration integration) {
		return getParameter(integration).isPresent();
	}

	public String getRequiredParameter(Integration integration) {
		Optional<String> property = getParameter(integration);
		if (required) {
			if (property.isPresent()) {
				return property.get();
			} else {
				throw new ReportPortalException(ErrorType.INCORRECT_REQUEST, formattedSupplier("'{}' should be present.", parameterName));
			}
		} else {
			throw new ReportPortalException(ErrorType.INCORRECT_REQUEST, formattedSupplier("'{}' is not required."));
		}
	}

	public Optional<String> getParameter(Map<String, Object> parametersMap) {
		return ofNullable(parametersMap.get(parameterName)).map(it -> (String) it).filter(StringUtils::isNotBlank);
	}

	public Optional<String> getParameter(UpdateAuthRQ request) {
		return ofNullable(request.getAuthParams()).flatMap(this::getParameter);
	}

	public void setParameter(UpdateAuthRQ request, Integration integration) {
		getParameter(request).ifPresent(it -> setParameter(integration, it));
	}

}