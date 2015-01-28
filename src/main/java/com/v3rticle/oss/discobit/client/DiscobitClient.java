package com.v3rticle.oss.discobit.client;

import com.v3rticle.oss.discobit.client.bootstrap.DiscobitOptions;
import com.v3rticle.oss.discobit.client.dto.ApplicationDTO;
import com.v3rticle.oss.discobit.client.dto.ConfigPropertyDTO;
import com.v3rticle.oss.discobit.client.dto.ConfigurationDTO;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The discoBit client wraps all calls to the repository REST-API. For detailed documentation see <a href="http://discobit.com/api">http://discobit.com/api</a>
 * <p>Check {@link DiscobitOptions} for potential configuration. Options can be provided as JVM system parameters or set within the system environment.</p>
 * 
 * @author jens@v3rticle.com
 *
 */
public class DiscobitClient {
	
	Logger log = Logger.getLogger(DiscobitClient.class.getName());
	
	/**
	 * the connector including settings
	 */
	private DiscobitConnector connector;
	
	/**
	 * DiscoBit Client with manual configuration
	 * @param connector
	 * @since 0.7
	 */
	protected DiscobitClient(DiscobitConnector connector){
		this.connector = connector;
	}
	
	/**
	 * DiscoBit Client with Autodiscovery
	 */
	protected DiscobitClient(){
		connector = new DiscobitConnector();
	}
	
	public boolean checkConfigExists(String uuid){
		boolean configExists = connector.checkConfiguration(uuid);
		if (!configExists)
			log.log(Level.SEVERE, "Configuration does not exist in repository: " + uuid);
		
		return configExists;
	}
	

	/**
	 * Creates a new application space that will contain the configurations. Spaces are bound to the discoBit user account and cannot be accessed from others.
	 * @param name mandatory - name of the application space
	 * @param email mandatory (notifications)
	 * @param owner mandatory
	 * @param url optional
	 * @return application ID
	 * @throws DiscobitOperationException
	 */
	public int createApplicationSpace(String name, String email, String owner, String url) throws DiscobitOperationException{
		
		// check params
		assert name != null;
		assert email != null;
		assert owner != null;
		
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			throw new DiscobitOperationException(e);
		}
		
		
		ApplicationDTO aDTO = new ApplicationDTO();
		aDTO.setName(name);
		aDTO.setOwner(owner);
		aDTO.setUrl(url);
		aDTO.setEmail(email);
		return connector.createApplicationSpace(aDTO);
	}
	
	/**
	 * Creates a new configuration which will contain the key-value properties. Configurations are linked to an application space and are identifier by a UUID.
	 * @param name mandatory
	 * @param description optional
	 * @return configuration UUID
	 * @throws DiscobitOperationException
	 */
	public UUID createConfiguration(int applicationId, String configurationName, String configurationDescription) throws DiscobitOperationException{
		assert configurationName != null;
		
		ConfigurationDTO cDTO = new ConfigurationDTO();
		cDTO.setName(configurationName);
		cDTO.setDescription(configurationDescription);
		
		ApplicationDTO aDTO = null;
		
		if (applicationId >= 0){
			aDTO = new ApplicationDTO();
			aDTO.setId(applicationId);
		}
		return connector.createConfiguration(cDTO, aDTO);
	}
	
	/**
	 * Key-Value-pair like properties. Tied to a specific configuration.
	 * @param configUUID mandatory
	 * @param propertyName mandatory
	 * @param propertyValue mandatory
	 * @throws DiscobitOperationException
	 * @return succeeded
	 */
	public boolean createConfigProperty(UUID configUUID, String propertyName, String propertyValue) throws DiscobitOperationException{
		assert configUUID != null;
		assert propertyName != null;
		assert propertyValue != null;
		
		assert checkConfigExists(configUUID.toString());
		
		ConfigurationDTO cDTO = new ConfigurationDTO();
		cDTO.setUuid(configUUID.toString());
		
		ConfigPropertyDTO cpDTO = new ConfigPropertyDTO();
		cpDTO.setKey(propertyName);
		cpDTO.setValue(propertyValue);
		
		// TODO
		cpDTO.setLocked(false);
		cpDTO.setOverrideAllowed(false);
		cpDTO.setComment(null);
		
		return connector.createConfigProperty(cpDTO, cDTO);
	}
	
	/**
	 * Reads a configuration property from the repository. Accesses local caches for lookups first.
	 * @param configUUID if null the connector will try to use the default configured UUID from settings
	 * @param key mandatory
	 * @param useCachedProperties default is false
	 * @return property value
	 * @throws DiscobitOperationException
	 */
	public String getConfigProperty(UUID configUUID, String key, boolean useCachedProperties) throws DiscobitOperationException{
		
		String selectedUUID = null;
		if (configUUID != null)
			selectedUUID = configUUID.toString();
		else if (selectedUUID == null)
			selectedUUID = connector.getSettings().getDefaultConfigurationUUID();
		
		assert checkConfigExists(selectedUUID);
		assert key != null;

		return connector.getConfigParamValue(selectedUUID, key);
	}
	
	/**
	 * Read a configuration from the repository
	 * @param configUUID
	 * @return
	 */
	public Properties getConfig(UUID configUUID) throws DiscobitOperationException{
		return connector.getConfiguration(configUUID.toString());
	}
	
	
	/**
	 * Check proper connectivity
	 * @return
	 * @throws DiscobitOperationException
	 */
	public boolean testAuthentication() throws DiscobitOperationException{
		return connector.testAuthentication();
	}

	/**
	 * push a configuration property file to the repository
	 * @param cUUID
	 * @param config
	 * @return success
	 * @since 0.7
	 */
	public boolean pushConfiguration(String cUUID, File config) {
		assert config != null;
		assert config.exists();
		assert cUUID != null;
		
		return connector.pushConfiguration(cUUID, config);
	}
}
