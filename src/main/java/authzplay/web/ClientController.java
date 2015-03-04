package authzplay.web;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.PartialRequestBuilder;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.header.OutBoundHeaders;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import authzplay.ClientSettings;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

@Controller
@PropertySource("classpath:playground.properties")
@Configuration
public class ClientController {

  @Autowired
  protected MessageSource messageSource;

  @Autowired
  private Environment env;

  protected void notice(RedirectAttributes redirectAttributes, String key, Object... args) {
    redirectAttributes.addFlashAttribute("flash.notice", messageSource.getMessage(key, args, Locale.ENGLISH));
  }

  private static final String AUTHORIZATION = "Authorization";
  private static final String SETTINGS = "settings";
  private static final String BR = System.getProperty("line.separator");

  private static final ObjectMapper mapper = new ObjectMapper();

  private Client client;

  /**
   * @param env
   */
  public ClientController() {
    ClientConfig config = new DefaultClientConfig();
    config.getClasses().add(JacksonJsonProvider.class);
    this.client = Client.create(config);
  }

  @RequestMapping(value = {"test"}, method = RequestMethod.GET)
  public String start(ModelMap modelMap, HttpServletRequest request, HttpServletResponse response)
    throws IOException {
    modelMap.addAttribute(SETTINGS, createDefaultSettings(false));
    return "oauth-client";
  }

  @RequestMapping(value = "/test", method = RequestMethod.POST, params = "reset")
  public String reset(ModelMap modelMap, HttpServletRequest request, HttpServletResponse response) throws IOException {
    return start(modelMap, request, response);
  }


  @RequestMapping(value = "test", method = RequestMethod.POST, params = "step1")
  public String step1(ModelMap modelMap, @ModelAttribute("settings")
  ClientSettings settings, HttpServletRequest request, HttpServletResponse response) throws IOException {
    settings.setStep("step2");
    modelMap.addAttribute(SETTINGS, settings);
    return "oauth-client";
  }

  @RequestMapping(value = "test", method = RequestMethod.POST, params = "step2")
  public void step2(ModelMap modelMap, @ModelAttribute("settings")
  ClientSettings settings, HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.sendRedirect(settings.getAuthorizationURLComplete());
  }

  @RequestMapping(value = "redirect", method = RequestMethod.GET)
  public String redirect(ModelMap modelMap, HttpServletRequest request, HttpServletResponse response)
    throws JsonParseException, JsonMappingException, IOException {
    String code = request.getParameter("code");
    ClientSettings settings = createDefaultSettings(false);

    MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
    formData.add("grant_type", "authorization_code");
    formData.add("code", code);
    formData.add("redirect_uri", env.getProperty("redirect_uri"));

    String auth = "Basic ".concat(new String(Base64.encodeBase64(settings.getOauthKey().concat(":")
      .concat(settings.getOauthSecret()).getBytes())));
    Builder builder = client.resource(settings.getAccessTokenEndPoint()).header(AUTHORIZATION, auth)
      .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
    OutBoundHeaders headers = getHeadersCopy(builder);
    ClientResponse clientResponse = builder.post(ClientResponse.class, formData);
    String json = new String(FileCopyUtils.copyToByteArray(clientResponse.getEntityInputStream()));
    HashMap map = mapper.readValue(json, HashMap.class);
    settings.setStep("step3");
    settings.setAccessToken((String) map.get("access_token"));
    modelMap.put(SETTINGS, settings);
    modelMap.put(
      "requestInfo",
      "Method: POST".concat(BR).concat("URL: ").concat(settings.getAccessTokenEndPoint()).concat(BR)
        .concat("Headers: ").concat(headers.toString()).concat(BR).concat("Body: ").concat(formData.toString()));
    addResponseInfo(modelMap, clientResponse);
    modelMap.put("rawResponseInfo", json);
    return "oauth-client";
  }

  @RequestMapping(value = "test", method = RequestMethod.POST, params = "step3")
  public String step3(ModelMap modelMap, @ModelAttribute("settings")
  ClientSettings settings, HttpServletRequest request, HttpServletResponse response) throws IOException {
    Builder builder = client.resource(settings.getRequestURL())
      .header(AUTHORIZATION, "bearer ".concat(settings.getAccessToken()))
      .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
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

  /**
   * See /apis-authorization-server/src/main/resources/db/migration/hsqldb/V1__auth-server-admin.sql
   */
  protected ClientSettings createDefaultSettings(boolean implicitGrant) {
    String responseType = implicitGrant ? "token" : "code";
    String redirectUri = env.getProperty("redirect_uri");
    String tokenUri = env.getProperty("token_uri");
    String clientId = env.getProperty("client_id");
    String clientSecret = env.getProperty("client_secret");
    String authorizeUrl = env.getProperty("authorize_url");
    String resourceServerApiUrl = env.getProperty("resource_server_api_url");
    ClientSettings settings = new ClientSettings(tokenUri, clientId, clientSecret, authorizeUrl, "step1", resourceServerApiUrl);
    settings.setGrantType("authCode");
    settings.setOauthScope("read");
    settings.setAuthorizationURLComplete(String.format(
      settings.getAuthorizationURL()
        .concat("?response_type=%s&client_id=%s&redirect_uri=%s&scope=read&state=example"), responseType, settings
        .getOauthKey(), redirectUri));
    return settings;

  }

}
