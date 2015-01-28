package com.v3rticle.oss.discobit.client;

import com.eclipsesource.json.JsonObject;
import com.v3rticle.oss.discobit.client.bootstrap.DiscobitSettings;
import com.v3rticle.oss.discobit.client.dto.ApplicationDTO;
import com.v3rticle.oss.discobit.client.dto.ConfigPropertyDTO;
import com.v3rticle.oss.discobit.client.dto.ConfigurationDTO;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST connector to discoBit repository API
 * See http://discobit.com/api for more information
 * 
 * @author jens@v3rticle.com
 *
 */
public class DiscobitConnector {

	Logger log = Logger.getLogger(DiscobitConnector.class.getName());
	
	private DiscobitSettings settings;
	private boolean authenticated;

	private String sessionCookie;
	
	protected DiscobitConnector(DiscobitSettings settings){
		this.settings = settings;
		authenticate();
	}
	
	protected DiscobitConnector(){
		settings = new DiscobitSettings();
		authenticate();
	}
	
	protected DiscobitSettings getSettings() {
		return settings;
	}

	/**
	 * do form authentication against rest interface, will get proper session cookie set in return
	 */
	private boolean processAuthentication(){
		
		boolean authenticationAttemptSuccess = false;
		
		String serverAddress  = settings.getServerURL() + "/rest/j_spring_security_check";

		StringBuilder response = new StringBuilder();
		int responseCode = -1;
		try {
			StringBuilder params = new StringBuilder();
			params.append("j_username=").append(URLEncoder.encode(settings.getRepositoryUsername(), "UTF-8"));
			params.append("&j_password=").append(URLEncoder.encode(settings.getRepositoryPassword(), "UTF-8"));

			URL loginUrl = new URL(serverAddress);

			HttpURLConnection conn = (HttpURLConnection) loginUrl.openConnection();
			conn.setRequestMethod("POST");

			//add request header
			conn.setRequestProperty("User-Agent", "java client");

			conn.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			wr.writeBytes(params.toString());
			wr.flush();
			wr.close();

			responseCode = conn.getResponseCode();

			BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

			in.close();

			String cookie = conn.getHeaderField("Set-Cookie");

			if (cookie != null) {
				String[] split = cookie.split(";");
				sessionCookie = split[0];
			}


		} catch (IOException e) {
			log.log(Level.SEVERE, "[discobit] connector failed to authenticate:", e);
		}

		log.info("[discobit] authentication response: " + responseCode);
		authenticationAttemptSuccess = responseCode == 200;

		if (!authenticationAttemptSuccess) {
			log.log(Level.SEVERE, "[discobit] connector failed to authenticate:");
			log.log(Level.SEVERE, response.toString());
		}


//		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.BEST_MATCH).build();
//
//	    HttpClientContext context = HttpClientContext.create();
//	    context.setCookieStore(cookieStore);
//
//	    CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(globalConfig).setDefaultCookieStore(cookieStore).build();
//	    HttpPost httpGet = new HttpPost(loginUrl);
//	    try {
//			CloseableHttpResponse loginResponse = httpClient.execute(httpGet,context);
//			log.info("[discobit] authentication response: " + loginResponse.getStatusLine());
//
//			if (loginResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
//				log.info("[discobit] authentication cookie: " + context.getCookieStore().getCookies());
//				authenticationAttemptSuccess = true;
//			} else {
//				log.warning("[discobit] authentication failed");
//			}
//
//		} catch (IOException e) {
//			log.log(Level.SEVERE, "[discobit] connector failed to authenticate:" + e.getMessage());
//		}
//
//		Unirest.setHttpClient(httpClient);
		
		return authenticationAttemptSuccess;

	}
	
	

	/**
	 * sets authentication status after attempt
	 * @return
	 */
	private boolean authenticate(){
		
		this.authenticated = processAuthentication();
		return authenticated;
		
	}
	
	/**
	 * Does not set authentication status after attempt - just for testing connectivity
	 * @return
	 */
	protected boolean testAuthentication(){
		return processAuthentication();
	}
	
	public boolean pushConfiguration(String cUUID, File config){
		// check cookie auth
//		if (!authenticated){
//			log.log(Level.SEVERE, "[discobit] connector not authenticated, returning");
//			return false;
//		}
//
//		HttpEntity entity = MultipartEntityBuilder.create()
//				.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
//				.setStrictMode()
//				.addBinaryBody("file", config)
//				.build();
//		HttpUriRequest post = RequestBuilder.post()
//				.setUri(settings.getServerURL() + "/rest/" + settings.getApiVersion() + "/configuration/propertyfile/")
//				.addParameter("cUUID", cUUID)
//				.setEntity(entity)
//				.build();
//
//		HttpClient client = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
//
//		int responseStatus = 0;
//		org.apache.http.HttpResponse response = null;
//	    try {
//	        response = client.execute(post);
//	        responseStatus = response.getStatusLine().getStatusCode();
//	    } catch (ClientProtocolException e) {
//	    	log.severe(e.getMessage());
//	    } catch (IOException e) {
//	    	log.severe(e.getMessage());
//	    }
//	    return responseStatus == HttpStatus.SC_OK;

		return false;
	}
	
	/**
	 * check if a given configuration exists in repository
	 * @param cUUID
	 * @return
	 */
	public boolean checkConfiguration(String cUUID){
		// check cookie auth
//		if (!authenticated){
//			log.log(Level.SEVERE, "[discobit] connector not authenticated, returning");
//			return false;
//		}
//
//		boolean configExists = false;
//		try {
//			String op = settings.getServerURL() + "/rest/" + settings.getApiVersion() + "/configuration/" + cUUID;
//
//			GetRequest getReq = Unirest.get(op);
//			if (getReq.asString().getBody() != null){
//				try {
//					new JSONObject(getReq.asString().getBody());
//					HttpResponse<JsonNode> jsonResponse = getReq.asJson();
//					configExists = jsonResponse.getBody().getObject().getString("uuid") != null;
//				} catch (Exception e){
//					log.log(Level.WARNING, "[discobit] connector failed to fetch configuration: " + cUUID + ", reason: " + e.getMessage());
//				}
//
//			} else {
//				log.log(Level.WARNING, "[discobit] connector failed to fetch configuration: " + cUUID);
//			}
//		} catch (UnirestException e) {
//			log.log(Level.SEVERE, "[discobit] connector failed to fetch configuration: " + e.getMessage());
//		}
//
//		return configExists;

		return false;
	}
	
	
	/**
	 * creates a new configuration space for an application in the repository
	 * @param aDTO
	 * @return
	 */
	protected int createApplicationSpace(ApplicationDTO aDTO){
		// check cookie auth
//		if (!authenticated){
//			log.log(Level.SEVERE, "[discobit] connector not authenticated, returning");
//			return -1;
//		}
//
//		int applicationSpaceId = -1;
//		HttpResponse<JsonNode> response = null;
//		try {
//			String op = settings.getServerURL() + "/rest/" + settings.getApiVersion() + "/application";
//			Object ob = JSONObject.wrap(aDTO);
//			JsonNode node = new JsonNode(ob.toString());
//
//			response = Unirest.post(op)
//			.header("accept", "application/json")
//			.header("Content-Type", "application/json")
//			.body(node).asJson();
//
//			applicationSpaceId = response.getBody().getObject().getInt("id");
//		} catch (UnirestException e) {
//			log.log(Level.SEVERE, "[discobit] connector failed to create application space: " + e.getMessage() + "\nResponse: " + response);
//		}
//
//		return applicationSpaceId;

		return -1;
	}
	
	
	/**
	 * creates a new configuration in the repository
	 * @param cDTO
	 * @return
	 */
	protected UUID createConfiguration(ConfigurationDTO cDTO, ApplicationDTO aDTO){
		// check cookie auth
//		if (!authenticated){
//			log.log(Level.SEVERE, "[discobit] connector not authenticated, returning");
//			return null;
//		}
//
//		String configUUID = null;
//		HttpResponse<JsonNode> response = null;
//		try {
//			String op = settings.getServerURL() + "/rest/" + settings.getApiVersion() + "/configuration/" + cDTO.getName();
//			Object ob = JSONObject.wrap(aDTO);
//			JsonNode node = new JsonNode(ob.toString());
//
//			response = Unirest.post(op)
//			.header("accept", "application/json")
//			.header("Content-Type", "application/json")
//			.body(node).asJson();
//			configUUID = response.getBody().getObject().getString("uuid");
//		} catch (UnirestException e) {
//			log.log(Level.SEVERE, "[discobit] connector failed to fetch configuration: " + e.getMessage() + "\nResponse: " + response);
//		}
//
//		return UUID.fromString(configUUID);

		return null;
	}
	
	/**
	 * creates a new key-value pair in repository
	 * @param cpDTO
	 * @param cDTO
	 * @return
	 */
	protected boolean createConfigProperty(ConfigPropertyDTO cpDTO, ConfigurationDTO cDTO){
		// check cookie auth
//		if (!authenticated){
//			log.log(Level.SEVERE, "[discobit] connector not authenticated, returning");
//			return false;
//		}
//
//		boolean success = false;
//		HttpResponse<JsonNode> response = null;
//		try {
//			String op = settings.getServerURL() + "/rest/" + settings.getApiVersion() + "/property/" + cDTO.getUuid() + "/" + cpDTO.getKey() + "/";
//
//			Object ob = JSONObject.wrap(cpDTO);
//			JsonNode node = new JsonNode(ob.toString());
//
//			response = Unirest.post(op)
//			.header("accept", "application/json")
//			.header("Content-Type", "application/json")
//			.body(node).asJson();
//
//			success = true;
//		} catch (UnirestException e) {
//			log.log(Level.SEVERE, "[discobit] connector failed to create new config property: " + e.getMessage() + "\nResponse: " + response);
//		}
//
//		return success;

		return false;
	}
	
	/**
	 * read a key-value pair from repository
	 * @param cUUID
	 * @param configParamKey
	 * @return
	 */
	protected String getConfigParamValue(String cUUID, String configParamKey){
		// check cookie auth
//		if (!authenticated){
//			log.log(Level.SEVERE, "[discobit] connector not authenticated, returning");
//			return null;
//		}
//
//		String responseValue = null;
//		try {
//			String op = settings.getServerURL() + "/rest/" + settings.getApiVersion() + "/property/" + cUUID + "/" + configParamKey;
//
//			GetRequest getReq = Unirest.get(op);
//			if (getReq.asString().getBody() != null){
//				try {
//					new JSONObject(getReq.asString().getBody());
//					HttpResponse<JsonNode> jsonResponse = getReq.asJson();
//					responseValue = jsonResponse.getBody().getObject().getString("value");
//				} catch (Exception e){
//					log.log(Level.WARNING, "[discobit] connector failed to fetch configuration value: " + cUUID + "/" + configParamKey + ", reason: " + e.getMessage());
//				}
//
//			} else {
//				log.log(Level.WARNING, "[discobit] connector failed to fetch configuration value: " + cUUID + "/" + configParamKey);
//			}
//		} catch (UnirestException e) {
//			log.log(Level.SEVERE, "[discobit] connector failed to fetch configuration value: " + e.getMessage());
//		}
//
//		return responseValue;

		return null;
	}
	
	protected Properties getConfiguration(String cUUID){
		// check cookie auth
		if (!authenticated){
			log.log(Level.SEVERE, "[discobit] connector not authenticated, returning");
			return null;
		}

		StringBuilder response = new StringBuilder();
		int responseCode = -1;
		try {

			URL url = new URL(settings.getServerURL() + "/rest/" + settings.getApiVersion() + "/configuration/" + cUUID);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setUseCaches(false);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Cookie", sessionCookie);
			conn.setRequestProperty("User-Agent", "java client");

			responseCode = conn.getResponseCode();

			BufferedReader in = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}

			in.close();

		} catch (IOException e) {
			log.log(Level.SEVERE, "[discobit] connector failed to get configuration:", e);
		}

		if (responseCode == 200) {

			Properties confProps = new Properties();

			JsonObject jsonObject = JsonObject.readFrom(response.toString());
			JsonObject jsonProperties = jsonObject.get("properties").asObject();

			for (String name : jsonProperties.names()) {
				JsonObject obj = jsonProperties.get(name).asObject();
				confProps.put(name, obj.get("value").asString());
			}

			return confProps;
		}
		else {
			log.log(Level.SEVERE, "[discobit] connector failed to get configuration! Response: " + response);
			return null;
		}
	}
	
}
