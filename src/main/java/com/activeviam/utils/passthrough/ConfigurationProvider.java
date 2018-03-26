package com.activeviam.utils.passthrough;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationProvider {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ConfigurationProvider.class.getName());
	private static final ConfigurationProvider instance = new ConfigurationProvider();

	private Properties configuration = new Properties();

	public ConfigurationProvider() {
		this.loadConfigurationFileIfExists();
	}

	private void loadConfigurationFileIfExists() {
		final InputStream file = ConfigurationProvider.class.getClassLoader().getResourceAsStream("passthrough.properties");
		if (file == null) {
			LOGGER.debug("No configuration file");
			return;
		}
		try {
			configuration.load(file);
		} catch (IOException e) {
			LOGGER.warn("Unable to load a configuration file");
		}
	}

	public String getValue(String property, String defaultValue) {
		String value = configuration.getProperty(property);
		if(value == null){
			value = System.getProperty("passthrough." + property);
		}
		if(value == null){
			value = System.getenv(keyToEnv(property));
		}
		if(value == null){
			value = defaultValue;
		}
		return value;
	}

	private String keyToEnv(String property) {
		return property.toUpperCase().replace('.', '_');
	}

	public static String get(String property) {
		return get(property, null);
	}

	public static String get(String property, String defaultValue) {
		return instance.getValue(property, defaultValue);
	}

}
