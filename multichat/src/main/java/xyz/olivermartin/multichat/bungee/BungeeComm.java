package xyz.olivermartin.multichat.bungee;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

/**
 * Bungee Communication Manager
 * <p>Manages all plug-in messaging channels on the BungeeCord side</p>
 * 
 * @author Oliver Martin (Revilo410)
 *
 */
public class BungeeComm implements Listener {

	public static void sendMessage(String message, ServerInfo server) {

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);

		try {
			// Players name
			out.writeUTF(message);

			// Should display name be set?
			Configuration configYML = ConfigManager.getInstance().getHandler("config.yml").getConfig();
			if (configYML.contains("set_display_name")) {
				if (configYML.getBoolean("set_display_name")) {
					out.writeUTF("T");
				} else {
					out.writeUTF("F");
				}
			} else {
				out.writeUTF("T");
			}

			// Display name format
			if (configYML.contains("display_name_format")) {
				out.writeUTF(configYML.getString("display_name_format"));
			} else {
				out.writeUTF("%PREFIX%%NICK%%SUFFIX%");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		server.sendData("multichat:comm", stream.toByteArray());

	}

	public static void sendCommandMessage(String command, ServerInfo server) {

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);

		try {

			// Command
			out.writeUTF(command);

		} catch (IOException e) {
			e.printStackTrace();
		}

		server.sendData("multichat:action", stream.toByteArray());

	}

	public static void sendPlayerCommandMessage(String command, String playerRegex, ServerInfo server) {

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);

		try {

			// Command
			out.writeUTF(playerRegex);
			out.writeUTF(command);

		} catch (IOException e) {
			e.printStackTrace();
		}

		server.sendData("multichat:paction", stream.toByteArray());

	}

	public static void sendChatMessage(String playerName, String message, ServerInfo server) {

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);

		try {
			// Players name
			out.writeUTF(playerName);
			// Message
			out.writeUTF(message);

		} catch (IOException e) {
			e.printStackTrace();
		}

		server.sendData("multichat:chat", stream.toByteArray());

	}

	@EventHandler
	public static void onPluginMessage(PluginMessageEvent ev) {

		if (! (ev.getTag().equals("multichat:comm") || ev.getTag().equals("multichat:prefix") || ev.getTag().equals("multichat:suffix") || ev.getTag().equals("multichat:world") || ev.getTag().equals("multichat:nick")) ) {
			return;
		}

		if (!(ev.getSender() instanceof Server)) {
			return;
		}

		if (ev.getTag().equals("multichat:comm")) {

			// TODO Remove - legacy
			return;

		}

		if (ev.getTag().equals("multichat:nick")) {

			ByteArrayInputStream stream = new ByteArrayInputStream(ev.getData());
			DataInputStream in = new DataInputStream(stream);

			try {

				UUID uuid = UUID.fromString(in.readUTF());
				String nick = in.readUTF();
				ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);

				if (player == null) return;

				synchronized (player) {

					/*
					 * Update the nickname stored somewhere and call for an update of the player
					 * display name in that location. (Pending the "true" value of fetch display names)
					 * and a new config option to decide if the display name should be set.
					 */

					Optional<PlayerMeta> opm = PlayerMetaManager.getInstance().getPlayer(uuid);

					if (opm.isPresent()) {

						opm.get().nick = nick;
						PlayerMetaManager.getInstance().updateDisplayName(uuid);

					}

				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		if (ev.getTag().equals("multichat:prefix")) {

			ByteArrayInputStream stream = new ByteArrayInputStream(ev.getData());
			DataInputStream in = new DataInputStream(stream);

			try {

				UUID uuid = UUID.fromString(in.readUTF());
				String prefix = in.readUTF();
				ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);

				if (player == null) return;

				synchronized (player) {

					/*
					 * Update the prefix stored somewhere and call for an update of the player
					 * display name in that location. (Pending the "true" value of fetch display names)
					 * and a new config option to decide if the display name should be set.
					 */

					Optional<PlayerMeta> opm = PlayerMetaManager.getInstance().getPlayer(uuid);

					if (opm.isPresent()) {

						opm.get().prefix = prefix;
						PlayerMetaManager.getInstance().updateDisplayName(uuid);

					}

				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		if (ev.getTag().equals("multichat:suffix")) {

			ByteArrayInputStream stream = new ByteArrayInputStream(ev.getData());
			DataInputStream in = new DataInputStream(stream);

			try {

				UUID uuid = UUID.fromString(in.readUTF());
				String suffix = in.readUTF();
				ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);

				if (player == null) return;

				synchronized (player) {

					/*
					 * Update the suffix stored somewhere and call for an update of the player
					 * display name in that location. (Pending the "true" value of fetch display names)
					 * and a new config option to decide if the display name should be set.
					 */

					Optional<PlayerMeta> opm = PlayerMetaManager.getInstance().getPlayer(uuid);

					if (opm.isPresent()) {

						opm.get().suffix = suffix;
						PlayerMetaManager.getInstance().updateDisplayName(uuid);

					}

				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		if (ev.getTag().equals("multichat:world")) {

			ByteArrayInputStream stream = new ByteArrayInputStream(ev.getData());
			DataInputStream in = new DataInputStream(stream);

			DebugManager.log("[multichat:world] Got an incoming channel message!");

			try {

				UUID uuid = UUID.fromString(in.readUTF());
				String world = in.readUTF();
				ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);

				if (player == null) return;

				DebugManager.log("[multichat:world] Player is online!");

				synchronized (player) {

					/*
					 * Update the world stored somewhere
					 */

					Optional<PlayerMeta> opm = PlayerMetaManager.getInstance().getPlayer(uuid);

					if (opm.isPresent()) {

						DebugManager.log("[multichat:world] Got their meta data correctly");

						opm.get().world = world;

						DebugManager.log("[multichat:world] Set their world to: " + world);

					}

				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
}
