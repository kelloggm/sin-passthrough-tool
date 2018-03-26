package com.activeviam.utils.passthrough;

/**
 * Handle the Cloud configuration for the current application.
 */
public class CloudProvider {

	private String region;

	public boolean isAWS() {
		return provider.equals(SupportedProviders.AWS);
	}

	public class SupportedProviders {
		static final String AWS = "AWS";
	}

	private static final CloudProvider instance = new CloudProvider();

	public static CloudProvider getInstance() {
		return instance;
	}

	private String provider;

	private CloudProvider() {
		this.provider = ConfigurationProvider.get("cloud.provider");
		this.setup();
	}

	private void setup() {
		switch (this.provider) {
			case SupportedProviders.AWS:
				break;
			default:
				throw new UnsupportedOperationException("Cloud provider " + this.provider + " is not supported");
		}
	}

	public String getRegion(){
		return region;
	}

}
