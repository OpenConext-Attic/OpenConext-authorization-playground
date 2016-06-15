package authzplay.web;

import authzplay.ClientSettings;
import authzplay.JWKVerifier;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.PartialRequestBuilder;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.representation.Form;
import com.sun.jersey.core.header.OutBoundHeaders;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Configuration
public class ClientController {

  @Value("${oauth.redirect_uri}")
  private String redirectUri;

  @Value("${oauth.token_uri}")
  private String tokenUri;

  @Value("${oauth.client_id}")
  private String clientId;

  @Value("${oauth.client_secret}")
  private String clientSecret;

  @Value("${oauth.authorize_url}")
  private String authorizeUrl;

  @Value("${oauth.resource_server_api_url}")
  private String resourceServerApiUrl;

  @Value("${oauth.check_token_url}")
  private String checkTokenUrl;

  @Value("${oauth.scopes}")
  private String scopes;

  @Value("${oidc.redirect_uri}")
  private String oidcRedirectUri;

  @Value("${oidc.token_uri}")
  private String oidcTokenUri;

  @Value("${oidc.client_id}")
  private String oidcClientId;

  @Value("${oidc.client_secret}")
  private String oidcClientSecret;

  @Value("${oidc.scopes}")
  private String oidcScopes;

  @Value("${oidc.authorize_url}")
  private String oidcAuthorizeUrl;

  @Value("${oidc.resource_server_api_url}")
  private String oidcResourceServerApiUrl;

  @Value("${oidc.introspect_url}")
  private String oidcIntrospectUrl;

  @Value("${oidc.user_info_url}")
  private String oidcUserInfoUrl;

  @Value("${oidc.jwk_url}")
  private String oidcJwkUrl;

  @Value("${oidc.well_known_configuration_url}")
  private String oidcWellKnownConfigurationUrl;

  private static final String AUTHORIZATION = "Authorization";
  private static final String SETTINGS = "settings";
  private static final String BR = System.getProperty("line.separator");

  private static final ObjectMapper mapper = new ObjectMapper();

  private Client client;

  public ClientController() {
    ClientConfig config = new DefaultClientConfig();
    config.getClasses().add(JacksonJsonProvider.class);
    this.client = Client.create(config);
  }

  @RequestMapping(value = {"/"}, method = RequestMethod.GET)
  public String start(ModelMap modelMap, @RequestParam(value = "modus", defaultValue = "oauth2", required = false) String modus)
    throws IOException {
    ClientSettings settings = createDefaultSettings(modus);
    modelMap.addAttribute(SETTINGS, settings);
    return "oauth-client";
  }

  @RequestMapping(value = "/", method = RequestMethod.POST, params = "reset")
  public String reset(ModelMap modelMap) throws IOException {
    return start(modelMap, "oauth2");
  }


  @RequestMapping(value = "/", method = RequestMethod.POST, params = "step1")
  public String step1(ModelMap modelMap, @ModelAttribute("settings")
  ClientSettings settings, HttpServletRequest request, HttpServletResponse response) throws IOException, ParseException {
    if (settings.getGrantType().equals("clientCredentials")) {
      settings.setStep("step3");
      request.getSession().setAttribute(SETTINGS, settings);
      redirect(modelMap, request, response);
    } else {
      settings.setStep("step2");
      String responseType;
      String responseTypeFromClient = settings.getResponseType();
      boolean implicit = settings.getGrantType().equals("implicit");
      if (!implicit && StringUtils.hasText(responseTypeFromClient)) {
        responseType = responseTypeFromClient;
      } else {
        responseType = implicit ? "token" : "code";
      }

      String encodedScopes = URLEncoder.encode(settings.getOauthScopes(), "UTF-8");
      String authorizationUrlComplete = String.format(
        settings.getAuthorizationURL()
          .concat("?response_type=%s&client_id=%s&scope=%s&state=example"), responseType, settings.getOauthKey(), encodedScopes);
      if (!settings.isNoRedirectUri()) {
        authorizationUrlComplete = authorizationUrlComplete + "&redirect_uri=" + redirectUri;
      }
      settings.setAuthorizationURLComplete(authorizationUrlComplete);
    }
    modelMap.addAttribute(SETTINGS, settings);
    return "oauth-client";
  }

  @RequestMapping(value = "/", method = RequestMethod.POST, params = "step2")
  public void step2(@ModelAttribute("settings")
                    ClientSettings settings, HttpServletRequest request, HttpServletResponse response) throws IOException {
    request.getSession().setAttribute(SETTINGS, settings);
    String authorizationURLComplete = settings.getAuthorizationURLComplete();
    response.sendRedirect(authorizationURLComplete);
  }

  @RequestMapping(value = "redirect", method = RequestMethod.GET)
  public String redirect(ModelMap modelMap, HttpServletRequest request, HttpServletResponse response)
    throws IOException, ParseException {
    ClientSettings settings = (ClientSettings) request.getSession().getAttribute(SETTINGS);
    String responseType = settings.getResponseType();
    if (settings.getGrantType().equals("implicit")) {
      modelMap.addAttribute("parseAnchorForAccessToken", Boolean.TRUE);
      if (settings.isOpenIdConnect()) {
        modelMap.addAttribute("parseAnchorForIdToken", Boolean.TRUE);
      }
    } else if (StringUtils.hasText(responseType) && responseType.equals("id_token")) {
      modelMap.addAttribute("parseAnchorForIdToken", Boolean.TRUE);
    } else {

      MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
      boolean isClientCredentials = settings.getGrantType().equals("clientCredentials");
      formData.add("grant_type", isClientCredentials ? "client_credentials" : "authorization_code");
      if (!isClientCredentials) {
        String code = request.getParameter("code");
        formData.add("code", code);
      }

      formData.add("redirect_uri", redirectUri);

      Builder builder = getBasicAuthBuilder(settings, settings.getAccessTokenEndPoint());
      OutBoundHeaders headers = getHeadersCopy(builder);
      ClientResponse clientResponse = builder.post(ClientResponse.class, formData);
      addResponseInfo(modelMap, clientResponse);
      String json = new String(FileCopyUtils.copyToByteArray(clientResponse.getEntityInputStream()));
      modelMap.put("rawResponseInfo", getRawResponseInfo(json));
      modelMap.put(
        "requestInfo",
        "Method: POST".concat(BR).concat("URL: ").concat(settings.getAccessTokenEndPoint()).concat(BR)
          .concat("Headers: ").concat(headers.toString()).concat(BR).concat("Body: ").concat(formData.toString()));
      if (clientResponse.getStatus() == 200) {
        HashMap map = mapper.readValue(json, HashMap.class);
        settings.setAccessToken((String) map.get("access_token"));
        if (settings.isOpenIdConnect()) {
          settings.setAccessTokenJson(parseJWT(settings.getAccessToken()));
          if (map.containsKey("id_token")) {
            settings.setIdToken((String) map.get("id_token"));
            settings.setIdTokenJson(parseJWT(settings.getIdToken()));
          }
        }

      }
    }
    modelMap.put(SETTINGS, settings);
    settings.setStep("step3");
    return "oauth-client";
  }

  private String getRawResponseInfo(String json) {
    //looks silly but easiest way
    try {
      JsonNode jsonNode = mapper.readTree(json);
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
    } catch (IOException e) {
      return json;
    }
  }

  @RequestMapping(value = "/", method = RequestMethod.POST, params = "step3")
  public String step3(ModelMap modelMap, @ModelAttribute("settings")
  ClientSettings settings) throws IOException {
    String requestURL = settings.getRequestURL();
    String accessToken = settings.getAccessToken();
    return performResourceCall(modelMap, settings, requestURL, accessToken);
  }

  private String performResourceCall(ModelMap modelMap, @ModelAttribute("settings") ClientSettings settings, String requestURL, String accessToken) throws IOException {
    //https://developer.salesforce.com/docs/atlas.en-us.198.0.api_rest.meta/api_rest/intro_curl.htm
    accessToken = accessToken.replace("!", "\\!");
    Builder builder = client.resource(requestURL)
      .header(AUTHORIZATION, "bearer ".concat(accessToken))
      .type(MediaType.APPLICATION_JSON_TYPE)
      .accept(MediaType.APPLICATION_JSON_TYPE);
    return doPerformCall(modelMap, settings, requestURL, builder, HttpMethod.GET, null);
  }

  private String doPerformCall(ModelMap modelMap, @ModelAttribute("settings") ClientSettings settings, String requestURL,
                               Builder builder, HttpMethod method, Object requestEntity) throws IOException {
    OutBoundHeaders headers = getHeadersCopy(builder);
    long start = System.currentTimeMillis();
    ClientResponse clientResponse;
    if (method.equals(HttpMethod.GET)) {
      clientResponse = builder.get(ClientResponse.class);
    } else if (method.equals(HttpMethod.POST)) {
      clientResponse = builder.post(ClientResponse.class, requestEntity);
    } else {
      throw new RuntimeException("Not supported method: " + method);
    }
    String json = IOUtils.toString(clientResponse.getEntityInputStream());
    settings.setStep("step3");
    modelMap.put(SETTINGS, settings);
    modelMap.put("requestInfo", "Method: ".concat(method.name()).concat(BR).concat("URL: ").concat(requestURL).concat(BR)
      .concat("Headers: ").concat(headers.toString()));
    addResponseInfo(modelMap, clientResponse);
    modelMap.put("responseTime", String.format("Took %s ms", System.currentTimeMillis() - start));
    modelMap.put("rawResponseInfo", getRawResponseInfo(json));
    return "oauth-client";
  }

  private String doPerformGet(ModelMap modelMap, @ModelAttribute("settings") ClientSettings settings, String requestURL,
                              Builder builder) throws IOException {
    return doPerformCall(modelMap, settings, requestURL, builder, HttpMethod.GET, null);
  }

  @RequestMapping(value = "/", method = RequestMethod.POST, params = "userInfo")
  public String userInfo(ModelMap modelMap, @ModelAttribute("settings")
  ClientSettings settings) throws IOException {
    String accessToken = settings.getAccessToken();
    return performResourceCall(modelMap, settings, oidcUserInfoUrl, accessToken);
  }

  @RequestMapping(value = "/", method = RequestMethod.POST, params = "introspect")
  public String introspect(ModelMap modelMap, @ModelAttribute("settings")
  ClientSettings settings) throws IOException {
    String accessToken = settings.getAccessToken();
    String requestURL = oidcIntrospectUrl + "?token=" + accessToken;
    Builder builder = getBasicAuthBuilder(settings, requestURL);

    return doPerformGet(modelMap, settings, requestURL, builder);
  }

  @RequestMapping(value = "/", method = RequestMethod.POST, params = "checkToken")
  public String checkToken(ModelMap modelMap, @ModelAttribute("settings")
  ClientSettings settings) throws IOException {
    String accessToken = settings.getAccessToken();
    Builder builder = getBasicAuthBuilder(settings, checkTokenUrl);
    Form formData = new Form();
    formData.put("token", Collections.singletonList(accessToken));
    return doPerformCall(modelMap, settings, checkTokenUrl, builder, HttpMethod.POST, formData);
  }

  @RequestMapping(value = "/decodeJwtToken", method = RequestMethod.GET)
  @ResponseBody
  public String decodeJwtToken(@RequestParam String jwtToken) throws IOException, ParseException {
    return parseJWT(jwtToken);
  }

  private Builder getBasicAuthBuilder(ClientSettings settings, String requestURL) {
    String auth = "Basic ".concat(new String(Base64.encodeBase64(settings.getOauthKey().concat(":")
      .concat(settings.getOauthSecret()).getBytes())));
    return client.resource(requestURL).header(AUTHORIZATION, auth)
      .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
  }

  private void addResponseInfo(ModelMap modelMap, ClientResponse clientResponse) {
    modelMap.put(
      "responseInfo",
      "Status: ".concat(String.valueOf(clientResponse.getStatus()).concat(BR).concat("Headers:")
        .concat(clientResponse.getHeaders().toString())));
  }

  /*
   * Nasty trick to be able to print out the headers after the POST is done
   */
  private OutBoundHeaders getHeadersCopy(Builder builder) {
    Field metaData;
    try {
      metaData = PartialRequestBuilder.class.getDeclaredField("metadata");
      metaData.setAccessible(true);
      return new OutBoundHeaders((OutBoundHeaders) metaData.get(builder));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String parseJWT(String jwtToken) throws IOException, ParseException {
    JWKVerifier verifier = new JWKVerifier(jwtToken);
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(verifier.toMap());
  }

  protected ClientSettings createDefaultSettings(String modus) {
    ClientSettings settings;

    if ("oauth2".equalsIgnoreCase(modus)) {
      settings = new ClientSettings(tokenUri, clientId, clientSecret, authorizeUrl, "step1", resourceServerApiUrl, scopes);
    } else {
      settings = new ClientSettings(oidcTokenUri, oidcClientId, oidcClientSecret, oidcAuthorizeUrl, "step1", oidcResourceServerApiUrl, oidcScopes);
      settings.setOpenIdConnect(true);
      settings.setOidcIntrospectUrl(oidcIntrospectUrl);
      settings.setOidcJwkUrl(oidcJwkUrl);
      settings.setOidcUserInfoUrl(oidcUserInfoUrl);
      settings.setOidcWellKnownConfigurationUrl(oidcWellKnownConfigurationUrl);
    }
    settings.setGrantType("authCode");
    settings.setResponseType("code");
    return settings;

  }

}
