package xyz.olivermartin.multichat.proxy.bungee;

import java.io.File;

import net.md_5.bungee.api.plugin.Plugin;
import xyz.olivermartin.multichat.common.MultiChatInfo;
import xyz.olivermartin.multichat.proxy.common.MultiChatProxy;
import xyz.olivermartin.multichat.proxy.common.MultiChatProxyPlatform;
import xyz.olivermartin.multichat.proxy.common.config.ProxyConfigManager;

public class MultiChatProxyBungeePlugin extends Plugin {

	@Override
	public void onEnable() {

		// BSTATS METRICS
		@SuppressWarnings("unused")
		Metrics metrics = new Metrics(this);

		// GET API
		MultiChatProxy api = MultiChatProxy.getInstance();

		// REGISTER PLATFORM
		MultiChatProxyPlatform platform = MultiChatProxyPlatform.BUNGEE;
		api.registerPlatform(platform);

		// REGISTER NAME
		String pluginName = "MultiChat";
		api.registerPluginName(pluginName);

		// REGISTER VERSION
		String pluginVersion = MultiChatInfo.LATEST_VERSION;
		api.registerPluginVersion(pluginVersion);

		// REGISTER CONFIG DIRECTORY
		File configDir = getDataFolder();
		if (!getDataFolder().exists()) {
			getLogger().info("Creating plugin directory for MultiChat!");
			getDataFolder().mkdirs();
		}
		api.registerConfigDirectory(configDir);

		// REGISTER TRANSLATIONS DIRECTORY
		File translationsDir = new File(configDir.toString() + File.separator + "translations");
		if (!translationsDir.exists()) {
			getLogger().info("Creating translations directory for MultiChat!");
			translationsDir.mkdirs();
		}

		// REGISTER CONFIG MANAGER
		ProxyConfigManager configManager = new ProxyConfigManager(platform);
		MultiChatProxy.getInstance().registerConfigManager(configManager);

		// REGISTER CONFIG FILES
		configManager.registerProxyMainConfig("config.yml", configDir);
		configManager.registerProxyJoinMessagesConfig("joinmessages.yml", configDir);
		configManager.registerProxyChatControlConfig("chatcontrol.yml", configDir);
		configManager.registerProxyMessagesConfig("messages.yml", configDir);

		// TODO Check config version

		// TODO Copy the translations files using a file system manager

		// TODO A datastore? Is this needed??

		// TODO load file based storage into the managers...

		// TODO register listeners

		// TODO register communication channels

		// TODO register commands

		// TODO run "start up routines"???!!!

		// TODO "setup chat control stuff?!"

		// TODO "Set default channel"

		// TODO "Set up global chat"

		// TODO "Add all appropriate servers to hard-coded global chat stream..."

		// TODO initiate backup routine

		// TODO fetch display names of all players...

	}

}
