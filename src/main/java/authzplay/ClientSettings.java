/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package authzplay;

import org.springframework.util.StringUtils;

/**
 * Form object for the test page
 * 
 */
public class ClientSettings {

  private String accessTokenEndPoint ;
  private String oauthKey;
  private String oauthSecret;
  private String oauthScopes; // space-separated
  private String grantType;
  private boolean noRedirectUri;
  private String authorizationURL;
  private String authorizationURLComplete;
  private String step ;
  private String requestURL;
  private String accessToken;
  private String idToken;
  private String responseType;
  private String responseMode = "query";
  private boolean openIdConnect;

  private String oidcIntrospectUrl;
  private String oidcUserInfoUrl;
  private String oidcJwkUrl;
  private String oidcWellKnownConfigurationUrl;

  private String accessTokenJson;
  private String idTokenJson;

  public ClientSettings() {
    super();
  }

  public ClientSettings(String accessTokenEndPoint, String oauthKey, String oauthSecret, String authorizationURL,
                        String step, String requestURL, String scopes) {
    super();
    this.accessTokenEndPoint = accessTokenEndPoint;
    this.oauthKey = oauthKey;
    this.oauthSecret = oauthSecret;
    this.authorizationURL = authorizationURL;
    this.step = step;
    this.requestURL = requestURL;
    this.oauthScopes = scopes;
  }


  /**
   * @return the accessTokenEndPoint
   */
  public String getAccessTokenEndPoint() {
    return accessTokenEndPoint;
  }

  /**
   * @param accessTokenEndPoint
   *          the accessTokenEndPoint to set
   */
  public void setAccessTokenEndPoint(String accessTokenEndPoint) {
    this.accessTokenEndPoint = accessTokenEndPoint;
  }

  /**
   * @return the oauthKey
   */
  public String getOauthKey() {
    return oauthKey;
  }

  /**
   * @param oauthKey
   *          the oauthKey to set
   */
  public void setOauthKey(String oauthKey) {
    this.oauthKey = oauthKey;
  }

  /**
   * @return the oauthSecret
   */
  public String getOauthSecret() {
    return oauthSecret;
  }

  /**
   * @param oauthSecret
   *          the oauthSecret to set
   */
  public void setOauthSecret(String oauthSecret) {
    this.oauthSecret = oauthSecret;
  }

  /**
   * @return the authorizationURL
   */
  public String getAuthorizationURL() {
    return authorizationURL;
  }

  /**
   * @param authorizationURL
   *          the authorizationURL to set
   */
  public void setAuthorizationURL(String authorizationURL) {
    this.authorizationURL = authorizationURL;
  }

  /**
   * @return the step
   */
  public String getStep() {
    return step;
  }

  /**
   * @param step
   *          the step to set
   */
  public void setStep(String step) {
    this.step = step;
  }

  /**
   * @return the requestURL
   */
  public String getRequestURL() {
    return requestURL;
  }

  /**
   * @param requestURL
   *          the requestURL to set
   */
  public void setRequestURL(String requestURL) {
    this.requestURL = requestURL;
  }

  /**
   * @return the authorizationURLComplete
   */
  public String getAuthorizationURLComplete() {
    return authorizationURLComplete;
  }

  /**
   * @param authorizationURLComplete the authorizationURLComplete to set
   */
  public void setAuthorizationURLComplete(String authorizationURLComplete) {
    this.authorizationURLComplete = authorizationURLComplete;
  }

  /**
   * @return the accessToken
   */
  public String getAccessToken() {
    return accessToken;
  }

  /**
   * @param accessToken the accessToken to set
   */
  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getOauthScopes() {
    return oauthScopes;
  }

  public void setOauthScopes(String oauthScopes) {
    this.oauthScopes = oauthScopes;
  }

  public String getGrantType() {
    return grantType;
  }

  public void setGrantType(String grantType) {
    this.grantType = grantType;
  }

  public boolean isNoRedirectUri() {
    return noRedirectUri;
  }

  public void setNoRedirectUri(boolean noRedirectUri) {
    this.noRedirectUri = noRedirectUri;
  }

  public String getIdToken() {
    return idToken;
  }

  public void setIdToken(String idToken) {
    this.idToken = idToken;
  }

  public String getResponseType() {
    return responseType;
  }

  public void setResponseType(String responseType) {
    this.responseType = responseType;
  }

  public String getResponseMode() {
    return responseMode;
  }

  public void setResponseMode(String responseMode) {
    this.responseMode = responseMode;
  }

  public boolean isOpenIdConnect() {
    return openIdConnect;
  }

  public void setOpenIdConnect(boolean openIdConnect) {
    this.openIdConnect = openIdConnect;
  }

  public String getOidcIntrospectUrl() {
    return oidcIntrospectUrl;
  }

  public void setOidcIntrospectUrl(String oidcIntrospectUrl) {
    this.oidcIntrospectUrl = oidcIntrospectUrl;
  }

  public String getOidcUserInfoUrl() {
    return oidcUserInfoUrl;
  }

  public void setOidcUserInfoUrl(String oidcUserInfoUrl) {
    this.oidcUserInfoUrl = oidcUserInfoUrl;
  }

  public String getOidcJwkUrl() {
    return oidcJwkUrl;
  }

  public void setOidcJwkUrl(String oidcJwkUrl) {
    this.oidcJwkUrl = oidcJwkUrl;
  }

  public String getOidcWellKnownConfigurationUrl() {
    return oidcWellKnownConfigurationUrl;
  }

  public void setOidcWellKnownConfigurationUrl(String oidcWellKnownConfigurationUrl) {
    this.oidcWellKnownConfigurationUrl = oidcWellKnownConfigurationUrl;
  }

  public String getAccessTokenJson() {
    return accessTokenJson;
  }

  public void setAccessTokenJson(String accessTokenJson) {
    this.accessTokenJson = accessTokenJson;
  }

  public String getIdTokenJson() {
    return idTokenJson;
  }

  public void setIdTokenJson(String idTokenJson) {
    this.idTokenJson = idTokenJson;
  }

  public boolean isAccessTokenPresent() {
    //might be that it is implicit, so we can't check for access token not null
    return !"id_token".equals(responseType);
  }

  public boolean isIdTokenPresent() {
    return oauthScopes.contains("openid") && !grantType.equals("clientCredentials");
  }
}
