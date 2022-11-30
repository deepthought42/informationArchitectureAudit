package com.looksee.audit.informationArchitecture.models;

import org.springframework.data.neo4j.core.schema.Node;

/**
 * 
 */
@Node
public class Screenshot extends LookseeObject {
	
	private String key;
	private String browser_name;
	private String url;
	private String checksum;
	private int width;
	private int height;
	
	public Screenshot(){}
	
	public Screenshot(String viewport, String browser_name, String checksum, int width, int height){
		setScreenshotUrl(viewport);
		setChecksum(checksum);
		setBrowser(browser_name);
		setKey(generateKey());
		setWidth(width);
		setHeight(height);
	}

	public String getScreenshotUrl() {
		return url;
	}

	public void setScreenshotUrl(String url) {
		this.url = url;
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getBrowser() {
		return browser_name;
	}

	public void setBrowser(String browser_name) {
		this.browser_name = browser_name;
	}
	
	public String generateKey() {
		return "screenshot" + this.checksum;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}
}