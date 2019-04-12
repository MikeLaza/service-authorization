/*
 * Copyright 2018 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.auth.integration.github;

import com.epam.ta.reportportal.dao.OAuthRegistrationRestrictionRepository;
import com.epam.ta.reportportal.entity.oauth.OAuthRegistrationRestriction;
import com.epam.ta.reportportal.exception.ReportPortalException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link OAuth2UserService}. Very similar to {@link DefaultOAuth2UserService},
 * but also checks user for defined restrictions (e.g. belonging to organisations) and replicates
 * user info to the ReportPortal database.
 *
 * @author Anton Machulski
 */
public class GitHubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
	private static final String MISSING_USER_INFO_URI_ERROR_CODE = "missing_user_info_uri";
	private static final String MISSING_USER_NAME_ATTRIBUTE_ERROR_CODE = "missing_user_name_attribute";
	private GitHubUserReplicator userReplicator;
	private OAuthRegistrationRestrictionRepository oAuthRegistrationRestrictionRepository;

	public GitHubOAuth2UserService(GitHubUserReplicator userReplicator,
			OAuthRegistrationRestrictionRepository oAuthRegistrationRestrictionRepository) {
		this.userReplicator = userReplicator;
		this.oAuthRegistrationRestrictionRepository = oAuthRegistrationRestrictionRepository;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		Assert.notNull(userRequest, "userRequest cannot be null");
		if (!"github".equalsIgnoreCase(userRequest.getClientRegistration().getRegistrationId())) {
			return null;
		}
		if (!StringUtils.hasText(userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri())) {
			OAuth2Error oauth2Error = new OAuth2Error(
					MISSING_USER_INFO_URI_ERROR_CODE,
					"Missing required UserInfo Uri in UserInfoEndpoint for Client Registration: " + userRequest.getClientRegistration()
							.getRegistrationId(),
					null
			);
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
		}
		String userNameAttributeName = userRequest.getClientRegistration()
				.getProviderDetails()
				.getUserInfoEndpoint()
				.getUserNameAttributeName();
		if (!StringUtils.hasText(userNameAttributeName)) {
			OAuth2Error oauth2Error = new OAuth2Error(
					MISSING_USER_NAME_ATTRIBUTE_ERROR_CODE,
					"Missing required \"user name\" attribute name in UserInfoEndpoint for Client Registration: "
							+ userRequest.getClientRegistration().getRegistrationId(),
					null
			);
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
		}
		GitHubClient gitHubClient = GitHubClient.withAccessToken(userRequest.getAccessToken().getTokenValue());
		UserResource gitHubUser = gitHubClient.getUser();
		Map<String, Object> userAttributes = gitHubClient.getUserAttributes();
		Set<String> allowedOrganizations = oAuthRegistrationRestrictionRepository.findByRegistrationId(userRequest.getClientRegistration()
				.getRegistrationId())
				.stream()
				.filter(restriction -> "organization".equalsIgnoreCase(restriction.getType()))
				.map(OAuthRegistrationRestriction::getValue)
				.collect(Collectors.toSet());
		if (!allowedOrganizations.isEmpty()) {
			boolean assignedToOrganization = gitHubClient.getUserOrganizations(gitHubUser.login)
					.stream()
					.map(org -> org.login)
					.anyMatch(allowedOrganizations::contains);
			if (!assignedToOrganization) {
				throw new ReportPortalException("User '" + gitHubUser.login + "' does not belong to allowed GitHUB organization");
			}
		}
		GrantedAuthority authority = new OAuth2UserAuthority(userAttributes);
		Set<GrantedAuthority> authorities = new HashSet<>();
		authorities.add(authority);
		userReplicator.replicateUser(userRequest.getAccessToken().getTokenValue());
		return new DefaultOAuth2User(authorities, userAttributes, userNameAttributeName);
	}
}
