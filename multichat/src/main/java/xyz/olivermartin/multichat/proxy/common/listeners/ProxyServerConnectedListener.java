package xyz.olivermartin.multichat.proxy.common.listeners;

import java.util.UUID;

import de.myzelyam.api.vanish.BungeeVanishAPI;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import xyz.olivermartin.multichat.bungee.ChatManipulation;
import xyz.olivermartin.multichat.bungee.ChatModeManager;
import xyz.olivermartin.multichat.bungee.ConfigManager;
import xyz.olivermartin.multichat.bungee.ConsoleManager;
import xyz.olivermartin.multichat.bungee.MultiChat;
import xyz.olivermartin.multichat.bungee.UUIDNameManager;
import xyz.olivermartin.multichat.common.MultiChatUtil;
import xyz.olivermartin.multichat.proxy.common.MultiChatProxy;
import xyz.olivermartin.multichat.proxy.common.ProxyJsonUtils;
import xyz.olivermartin.multichat.proxy.common.channels.ChannelManager;
import xyz.olivermartin.multichat.proxy.common.config.ConfigFile;
import xyz.olivermartin.multichat.proxy.common.storage.ProxyDataStore;

public class ProxyServerConnectedListener implements Listener {

	private void displayMessage(ProxiedPlayer player, ProxiedPlayer sender, String senderServer, String message) {

		message = MultiChatUtil.translateColourCodes(message);

		if (player.getUniqueId().equals(sender.getUniqueId())) {
			if (MultiChat.legacyServers.contains(senderServer)) message = MultiChatUtil.approximateRGBColourCodes(message);
		} else {
			if (player.getServer() == null) return;
			if (MultiChat.legacyServers.contains(player.getServer().getInfo().getName())) message = MultiChatUtil.approximateRGBColourCodes(message);
		}

		player.sendMessage(ProxyJsonUtils.parseMessage(message));

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onServerConnected(ServerConnectedEvent event) {

		ProxyDataStore ds = MultiChatProxy.getInstance().getDataStore();
		ChannelManager channelManager = MultiChatProxy.getInstance().getChannelManager();

		ProxiedPlayer player = event.getPlayer();
		UUID uuid = player.getUniqueId();

		String defaultChannel = MultiChatProxy.getInstance().getContextManager().getGlobalContext().getDefaultChannel();
		boolean forceChannel = MultiChatProxy.getInstance().getContextManager().getGlobalContext().isForceChannel();
		boolean firstJoin = false;

		// Set up chat info
		if (!ChatModeManager.getInstance().existsPlayer(uuid)) {

			boolean globalMode;

			if (!defaultChannel.equalsIgnoreCase("local")) {
				globalMode = true;
			} else {
				globalMode = false;
			}
			ChatModeManager.getInstance().registerPlayer(uuid, globalMode);
			firstJoin = true;

		}

		// If we are forcing the channels, then force it
		if (forceChannel) {

			boolean globalMode;
			if (!defaultChannel.equalsIgnoreCase("local")) {
				globalMode = true;
			} else {
				globalMode = false;
			}
			ChatModeManager.getInstance().registerPlayer(uuid, globalMode);

		}

		// Set player to appropriate channels in NEW CHANNELS system
		if (ChatModeManager.getInstance().isGlobal(uuid)) {
			channelManager.select(uuid, "global");
		} else {
			channelManager.select(uuid, "local");
		}

		// Remove any old UUID - Name pairings
		if (UUIDNameManager.existsUUID(uuid)) {
			UUIDNameManager.removeUUID(uuid);
		}

		// Register updated entry in UUID - Name map
		UUIDNameManager.addNew(uuid, player.getName());
		ConsoleManager.log("Refreshed UUID-Name lookup: " + uuid.toString());

		// If player is only switching server (not joining for first time) then leave now
		if (ds.getJoinedNetwork().contains(player.getUniqueId())) return;
		ds.getJoinedNetwork().add(player.getUniqueId());

		// If MultiChat is handling join messages...
		if (ConfigManager.getInstance().getHandler(ConfigFile.JOIN_MESSAGES).getConfig().getBoolean("showjoin")
				|| ConfigManager.getInstance().getHandler(ConfigFile.JOIN_MESSAGES).getConfig().getBoolean("welcome")
				|| ConfigManager.getInstance().getHandler(ConfigFile.JOIN_MESSAGES).getConfig().getBoolean("private_welcome")) {

			// PremiumVanish support, return as early as possible to avoid loading unnecessary resources
			if (MultiChat.premiumVanish && MultiChat.hideVanishedStaffInJoin && BungeeVanishAPI.isInvisible(player)) {
				return;
			}

			// Load join message formats from config
			String joinformat = ConfigManager.getInstance().getHandler(ConfigFile.JOIN_MESSAGES).getConfig().getString("serverjoin");
			String silentformat = ConfigManager.getInstance().getHandler(ConfigFile.JOIN_MESSAGES).getConfig().getString("silentjoin");
			String welcomeMessage = ConfigManager.getInstance().getHandler(ConfigFile.JOIN_MESSAGES).getConfig().getString("welcome_message");
			String privateWelcomeMessage = ConfigManager.getInstance().getHandler(ConfigFile.JOIN_MESSAGES).getConfig().getString("private_welcome_message");

			// Replace the placeholders
			ChatManipulation chatman = new ChatManipulation(); // TODO Legacy
			joinformat = chatman.replaceJoinMsgVars(joinformat, player.getName(), event.getServer().getInfo().getName());
			silentformat = chatman.replaceJoinMsgVars(silentformat, player.getName(), event.getServer().getInfo().getName());
			welcomeMessage = chatman.replaceJoinMsgVars(welcomeMessage, player.getName(), event.getServer().getInfo().getName());
			privateWelcomeMessage = chatman.replaceJoinMsgVars(privateWelcomeMessage, player.getName(), event.getServer().getInfo().getName());

			// Check which messages should be broadcast
			boolean broadcastWelcome = ConfigManager.getInstance().getHandler(ConfigFile.JOIN_MESSAGES).getConfig().getBoolean("welcome", true);
			boolean privateWelcome = ConfigManager.getInstance().getHandler(ConfigFile.JOIN_MESSAGES).getConfig().getBoolean("private_welcome", false);
			boolean broadcastJoin = !player.hasPermission("multichat.staff.silentjoin");

			// Broadcast
			for (ProxiedPlayer onlineplayer : ProxyServer.getInstance().getPlayers()) {

				if (broadcastJoin) {

					if (firstJoin && broadcastWelcome) {
						displayMessage(onlineplayer, event.getPlayer(), event.getServer().getInfo().getName(), welcomeMessage);
					}

					if (firstJoin && privateWelcome
							&& onlineplayer.getName().equals(player.getName())) {

						displayMessage(onlineplayer, event.getPlayer(), event.getServer().getInfo().getName(), privateWelcomeMessage);

					}

					if (ConfigManager.getInstance().getHandler(ConfigFile.JOIN_MESSAGES).getConfig().getBoolean("showjoin")) {
						displayMessage(onlineplayer, event.getPlayer(), event.getServer().getInfo().getName(), joinformat);
					}

				} else {

					ds.getHiddenStaff().add(player.getUniqueId());

					if (ConfigManager.getInstance().getHandler(ConfigFile.JOIN_MESSAGES).getConfig().getBoolean("showjoin")) {
						if (onlineplayer.hasPermission("multichat.staff.silentjoin") ) {
							displayMessage(onlineplayer, event.getPlayer(), event.getServer().getInfo().getName(), silentformat);
						}
					}

				}

			}

		}

	}

}