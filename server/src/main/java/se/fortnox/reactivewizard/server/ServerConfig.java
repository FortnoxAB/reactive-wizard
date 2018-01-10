package se.fortnox.reactivewizard.server;

import se.fortnox.reactivewizard.config.Config;

@Config("server")
public class ServerConfig {

	private int		port	= 8080;
	private boolean	enabled	= true;
	private int     blockingThreadPoolSize = 20;
	private int maxHeaderSize = 20*1024;
	private int maxInitialLineLengthDefault = 4096;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getBlockingThreadPoolSize() {
		return blockingThreadPoolSize;
	}

	public void setBlockingThreadPoolSize(int blockingThreadPoolSize) {
		this.blockingThreadPoolSize = blockingThreadPoolSize;
	}

	public int getMaxHeaderSize() {
		return maxHeaderSize;
	}

	public void setMaxHeaderSize(int maxHeaderSize) {
		this.maxHeaderSize = maxHeaderSize;
	}

	public int getMaxInitialLineLengthDefault() {
		return maxInitialLineLengthDefault;
	}

	public void setMaxInitialLineLengthDefault(int maxInitialLineLengthDefault) {
		this.maxInitialLineLengthDefault = maxInitialLineLengthDefault;
	}
}
