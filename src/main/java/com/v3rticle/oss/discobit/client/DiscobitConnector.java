package com.v3rticle.oss.discobit.client;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;
import com.v3rticle.oss.discobit.client.bootstrap.DiscobitSettings;
import com.v3rticle.oss.discobit.client.dto.ApplicationDTO;
import com.v3rticle.oss.discobit.client.dto.ConfigPropertyDTO;
import com.v3rticle.oss.discobit.client.dto.ConfigurationDTO;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST connector to discoBit repository API
 * See http://discobit.com/api for more information
 *
 * @author jens@v3rticle.com, Nathanael Schwalbe
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

		try {
			StringBuilder params = new StringBuilder();
			params.append("j_username=").append(URLEncoder.encode(settings.getRepositoryUsername(), "UTF-8"));
			params.append("&j_password=").append(URLEncoder.encode(settings.getRepositoryPassword(), "UTF-8"));

			URL loginUrl = new URL(serverAddress);

			HttpURLConnection conn = (HttpURLConnection) loginUrl.openConnection();

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(params.toString());
            }

            int responseCode = conn.getResponseCode();
            String response = readResponse(conn);

            log.info("[discobit] authentication response: " + responseCode);

            if (responseCode == 200) {
                String cookie = conn.getHeaderField("Set-Cookie");

                if (cookie != null) {
                    String[] split = cookie.split(";");
                    sessionCookie = split[0];
                }

                authenticationAttemptSuccess = true;
            } else {
                log.log(Level.SEVERE, "[discobit] connector failed to authenticate. Returned " + responseCode);
                log.log(Level.SEVERE, response);
            }

		} catch (IOException e) {
			log.log(Level.SEVERE, "[discobit] connector failed to authenticate:", e);
		}

		return authenticationAttemptSuccess;

	}

    private String readResponse(HttpURLConnection conn) throws IOException {

        StringBuilder response = new StringBuilder();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {

            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }

        return response.toString();
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
        if (!authenticated) {
            log.log(Level.SEVERE, "[discobit] connector not authenticated, returning");
            return false;
        }

        String charset = "UTF-8";
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(settings.getServerURL() + "/rest/" + settings.getApiVersion() + "/configuration/propertyfile/").openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/mixed; boundary=" + boundary);
            conn.setRequestProperty("Cookie", sessionCookie);
            conn.setRequestProperty("Accept", "application/json");

            try (
                    OutputStream output = conn.getOutputStream();
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true)
            ) {

                // Send normal param.
                writer.append("--").append(boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"cUUID\"").append(CRLF);
                writer.append("Content-Type: text/plain; charset=").append(charset).append(CRLF);
                writer.append(CRLF).append(cUUID).append(CRLF).flush();

                // Send binary file.
                writer.append("--").append(boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(config.getName()).append("\"").append(CRLF);
                writer.append("Content-Type: ").append(URLConnection.guessContentTypeFromName(config.getName())).append(CRLF);
                writer.append("Content-Transfer-Encoding: binary").append(CRLF);
                writer.append(CRLF).flush();
                Files.copy(config.toPath(), output);
                output.flush(); // Important before continuing with writer!
                writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

                // Send text file.
//                writer.append("--" + boundary).append(CRLF);
//                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + config.getName() + "\"").append(CRLF);
//                writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
//                writer.append(CRLF).flush();
//                Files.copy(config.toPath(), output);
//                output.flush(); // Important before continuing with writer!
//                writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

                // End of multipart/form-data.
                writer.append("--").append(boundary).append("--").append(CRLF).flush();
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                return true;
            }

            String response = readResponse(conn);

            log.severe(response);


        } catch (IOException e) {
            log.log(Level.SEVERE, "[discobit] failed to push configuration: ", e);
        }

        return false;
    }
	
	/**
	 * check if a given configuration exists in repository
	 * @param cUUID
	 * @return
     */
    public boolean checkConfiguration(String cUUID){
		// check cookie auth
        if (!authenticated) {
            log.log(Level.SEVERE, "[discobit] connector not authenticated, returning");
            return false;
        }

        boolean configExists = false;
        try {
            URL url = new URL(settings.getServerURL() + "/rest/" + settings.getApiVersion() + "/configuration/" + cUUID);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Cookie", sessionCookie);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            String response = readResponse(conn);

            if (responseCode == 200) {
                JsonObject jsonObject = JsonObject.readFrom(response);
                configExists = jsonObject.get("uuid") != null;
            } else {
                log.log(Level.SEVERE, "[discobit] connector check configuration existence: " + cUUID + ". Returned " + responseCode);
                log.log(Level.SEVERE, response);
            }

        } catch (IOException | ParseException e) {
            log.log(Level.SEVERE, "[discobit] connector failed to fetch configuration: ", e);
        }

        return configExists;
    }
	
	
	/**
	 * creates a new configuration space for an application in the repository
     * @param dto
     * @return
	 */
    protected int createApplicationSpace(ApplicationDTO dto) {

        if (!authenticated) {
            log.log(Level.SEVERE, "[discobit] connector not authenticated, returning");
            return -1;
        }

        int applicationSpaceId = -1;
        try {
            URL url = new URL(settings.getServerURL() + "/rest/" + settings.getApiVersion() + "/application");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Cookie", sessionCookie);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");

            JsonObject json = new JsonObject();
            json.add("name", dto.getName())
                    .add("owner", dto.getOwner())
                    .add("email", dto.getEmail())
                    .add("url", dto.getUrl());

            try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), "UTF-8")) {
                json.writeTo(wr);
            }

            int responseCode = conn.getResponseCode();
            String response = readResponse(conn);

            if (responseCode == 200) {
                JsonObject jsonObject = JsonObject.readFrom(response);
                applicationSpaceId = jsonObject.getInt("id", -1);
            } else {
                log.log(Level.SEVERE, "[discobit] connector failed to create application space! \nResponse: " + responseCode);
                log.log(Level.SEVERE, response);
            }

        } catch (IOException | ParseException e) {
            log.log(Level.SEVERE, "[discobit] connector failed to create application space!", e);
        }

        return applicationSpaceId;
    }
	
	
	/**
	 * creates a new configuration in the repository
     * @param dto
     * @return
	 */
    protected UUID createConfiguration(ConfigurationDTO dto, ApplicationDTO aDTO) {
        // check cookie auth
        if (!authenticated) {
            log.log(Level.SEVERE, "[discobit] connector not authenticated, returning");
            return null;
        }

        String configUUID = null;
        try {
            URL url = new URL(settings.getServerURL() + "/rest/" + settings.getApiVersion() + "/configuration/" + dto.getName());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Cookie", sessionCookie);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");

            JsonObject json = new JsonObject();
//            json.add("name", aDTO.getName())
//                    .add("owner", aDTO.getOwner())
//                    .add("email", aDTO.getEmail())
//                    .add("url", aDTO.getUrl())
            json.add("id", aDTO.getId());

            try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), "UTF-8")) {
                json.writeTo(wr);
            }

            int responseCode = conn.getResponseCode();
            String response = readResponse(conn);

            if (responseCode == 200) {
                JsonObject jsonObject = JsonObject.readFrom(response);
                configUUID = jsonObject.getString("uuid", null);
            } else {
                log.log(Level.SEVERE, "[discobit] connector failed to create configuration! \nResponse: " + responseCode);
                log.log(Level.SEVERE, response);
            }

        } catch (IOException e) {
            log.log(Level.SEVERE, "[discobit] connector failed to create configuration!", e);
        }

        return configUUID != null ? UUID.fromString(configUUID) : null;
    }
	
	/**
	 * creates a new key-value pair in repository
	 * @param cpDTO
	 * @param cDTO
	 * @return
	 */
	protected boolean createConfigProperty(ConfigPropertyDTO cpDTO, ConfigurationDTO cDTO){
		// check cookie auth
        if (!authenticated) {
            log.log(Level.SEVERE, "[discobit] connector not authenticated, returning");
            return false;
        }

        boolean success = false;
        try {
            URL url = new URL(settings.getServerURL() + "/rest/" + settings.getApiVersion() + "/property/" + cDTO.getUuid() + "/" + cpDTO.getKey() + "/");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Cookie", sessionCookie);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");

            JsonObject json = new JsonObject();
            json.add("key", cpDTO.getKey())
                    .add("name", cpDTO.getValue())
                    .add("comment", cpDTO.getComment())
                    .add("encrypted", cpDTO.isEncrypted())
                    .add("locked", cpDTO.isLocked())
                    .add("overrideAllowed", cpDTO.isOverrideAllowed());

            try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), "UTF-8")) {
                json.writeTo(wr);
            }

            int responseCode = conn.getResponseCode();
            String response = readResponse(conn);

            if (responseCode == 200) {
                success = true;
            } else {
                log.log(Level.SEVERE, "[discobit] connector failed to create new config property: \nResponse: " + responseCode);
                log.log(Level.SEVERE, response);
            }

        } catch (IOException e) {
            log.log(Level.SEVERE, "[discobit] connector failed to create new config property!", e);
        }

        return success;
    }
	
	/**
	 * read a key-value pair from repository
	 * @param cUUID
	 * @param configParamKey
	 * @return
	 */
	protected String getConfigParamValue(String cUUID, String configParamKey){
		// check cookie auth
        if (!authenticated) {
            log.log(Level.SEVERE, "[discobit] connector not authenticated, returning");
            return null;
        }

        String value = null;

        try {
            URL url = new URL(settings.getServerURL() + "/rest/" + settings.getApiVersion() + "/property/" + cUUID + "/" + configParamKey);

            log.log(Level.SEVERE, url.toString());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Cookie", sessionCookie);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            String response = readResponse(conn);

            if (responseCode == 200) {
                JsonObject jsonObject = JsonObject.readFrom(response);
                value = jsonObject.getString("value", null);
            } else {
                log.log(Level.SEVERE, "[discobit] connector failed to fetch configuration value: " + cUUID + "/" + configParamKey + " Returned " + responseCode);
                log.log(Level.SEVERE, response);
            }

        } catch (IOException | ParseException e) {
            log.log(Level.SEVERE, "[discobit] connector failed to fetch configuration value: ", e);
        }

        return value;
    }
	
	protected Properties getConfiguration(String cUUID){
		// check cookie auth
		if (!authenticated){
			log.log(Level.SEVERE, "[discobit] connector not authenticated, returning");
			return null;
		}

        Properties confProps = null;

		try {

			URL url = new URL(settings.getServerURL() + "/rest/" + settings.getApiVersion() + "/configuration/" + cUUID);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setUseCaches(false);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Cookie", sessionCookie);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            String response = readResponse(conn);

            if (responseCode == 200) {

                confProps = new Properties();

                JsonObject jsonObject = JsonObject.readFrom(response);
                JsonObject jsonProperties = jsonObject.get("properties").asObject();

                for (String name : jsonProperties.names()) {
                    JsonObject obj = jsonProperties.get(name).asObject();
                    confProps.put(name, obj.get("value").asString());
                }
            } else {
                log.log(Level.SEVERE, "[discobit] connector failed to get configuration! Returned: " + responseCode);
                log.log(Level.SEVERE, response);
            }

        } catch (IOException | ParseException e) {
            log.log(Level.SEVERE, "[discobit] connector failed to get configuration:", e);
		}

        return confProps;
    }
	
}
