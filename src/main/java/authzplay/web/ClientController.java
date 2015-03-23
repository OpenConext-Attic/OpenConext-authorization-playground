package authzplay.web;

import authzplay.ClientSettings;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.PartialRequestBuilder;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.header.OutBoundHeaders;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.HashMap;

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

  @Value("${oauth.scopes}")
  private String scopes;

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
  public String start(ModelMap modelMap, HttpServletRequest request, HttpServletResponse response)
    throws IOException {
    modelMap.addAttribute(SETTINGS, createDefaultSettings());
    return "oauth-client";
  }

  @RequestMapping(value = "/", method = RequestMethod.POST, params = "reset")
  public String reset(ModelMap modelMap, HttpServletRequest request, HttpServletResponse response) throws IOException {
    return start(modelMap, request, response);
  }


  @RequestMapping(value = "/", method = RequestMethod.POST, params = "step1")
  public String step1(ModelMap modelMap, @ModelAttribute("settings")
  ClientSettings settings, HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (settings.getGrantType().equals("clientCredentials")) {
      settings.setStep("step3");
      request.getSession().setAttribute(SETTINGS, settings);
      redirect(modelMap, request, response);
    } else {
      settings.setStep("step2");
      String responseType = settings.getGrantType().equals("implicit") ? "token" : "code";
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
  public void step2(ModelMap modelMap, @ModelAttribute("settings")
  ClientSettings settings, HttpServletRequest request, HttpServletResponse response) throws IOException {
    request.getSession().setAttribute(SETTINGS, settings);
    String authorizationURLComplete = settings.getAuthorizationURLComplete();
    response.sendRedirect(authorizationURLComplete);
  }

  @RequestMapping(value = "redirect", method = RequestMethod.GET)
  public String redirect(ModelMap modelMap, HttpServletRequest request, HttpServletResponse response)
    throws IOException {
    ClientSettings settings = (ClientSettings) request.getSession().getAttribute(SETTINGS);
    if (settings.getGrantType().equals("implicit")) {
      modelMap.addAttribute("parseAnchorForAccessToken", Boolean.TRUE);
    } else {

      MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
      boolean isClientCredentials = settings.getGrantType().equals("clientCredentials");
      formData.add("grant_type", isClientCredentials ? "client_credentials" : "authorization_code");
      if (!isClientCredentials) {
        String code = request.getParameter("code");
        formData.add("code", code);
      }

      formData.add("redirect_uri", redirectUri);

      String auth = "Basic ".concat(new String(Base64.encodeBase64(settings.getOauthKey().concat(":")
        .concat(settings.getOauthSecret()).getBytes())));
      Builder builder = client.resource(settings.getAccessTokenEndPoint()).header(AUTHORIZATION, auth)
        .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
      OutBoundHeaders headers = getHeadersCopy(builder);
      ClientResponse clientResponse = builder.post(ClientResponse.class, formData);
      addResponseInfo(modelMap, clientResponse);
      String json = new String(FileCopyUtils.copyToByteArray(clientResponse.getEntityInputStream()));
      modelMap.put("rawResponseInfo", json);
      modelMap.put(
        "requestInfo",
        "Method: POST".concat(BR).concat("URL: ").concat(settings.getAccessTokenEndPoint()).concat(BR)
          .concat("Headers: ").concat(headers.toString()).concat(BR).concat("Body: ").concat(formData.toString()));
      if (clientResponse.getStatus() == 200) {
        HashMap map = mapper.readValue(json, HashMap.class);
        settings.setAccessToken((String) map.get("access_token"));
      }
    }
    modelMap.put(SETTINGS, settings);
    settings.setStep("step3");
    return "oauth-client";
  }

  @RequestMapping(value = "/", method = RequestMethod.POST, params = "step3")
  public String step3(ModelMap modelMap, @ModelAttribute("settings")
  ClientSettings settings, HttpServletRequest request, HttpServletResponse response) throws IOException {
    Builder builder = client.resource(settings.getRequestURL())
      .header(AUTHORIZATION, "bearer ".concat(settings.getAccessToken()))
      .type(MediaType.APPLICATION_JSON_TYPE)
      .accept(MediaType.APPLICATION_JSON_TYPE);
    OutBoundHeaders headers = getHeadersCopy(builder);
    ClientResponse clientResponse = builder.get(ClientResponse.class);
    String json = IOUtils.toString(clientResponse.getEntityInputStream());
    settings.setStep("step3");
    modelMap.put(SETTINGS, settings);
    modelMap.put("requestInfo", "Method: GET".concat(BR).concat("URL: ").concat(settings.getRequestURL()).concat(BR)
      .concat("Headers: ").concat(headers.toString()));
    addResponseInfo(modelMap, clientResponse);
    modelMap.put("rawResponseInfo", json);
    return "oauth-client";
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

  protected ClientSettings createDefaultSettings() {
    ClientSettings settings = new ClientSettings(tokenUri, clientId, clientSecret, authorizeUrl, "step1", resourceServerApiUrl, scopes);
    settings.setGrantType("authCode");
    return settings;

  }

}
