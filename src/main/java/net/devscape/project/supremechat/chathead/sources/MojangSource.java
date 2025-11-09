package net.devscape.project.supremechat.chathead.sources;

import net.devscape.project.supremechat.chathead.SkinSource;
import net.devscape.project.supremechat.chathead.SkinSourceEnum;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.OfflinePlayer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

/**
 * SkinSource implementation to retrieve heads from Mojang.
 * Works for online mode servers, can convert username to UUID, but NOT suitable for offline mode.
 */
public class MojangSource extends SkinSource {

    public MojangSource(boolean useUUIDWhenRetrieve) {
        super(SkinSourceEnum.MOJANG, true, useUUIDWhenRetrieve);
    }

    public MojangSource() {
        super(SkinSourceEnum.MOJANG, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseComponent[] getHead(OfflinePlayer player, boolean overlay) {
        if (useUUIDWhenRetrieve()) {
            return toBaseComponent(getPixelColorsFromSkin(getPlayerSkinFromMojang(player.getUniqueId().toString()), overlay));
        } else {
            return toBaseComponent(getPixelColorsFromSkin(getPlayerSkinFromMojang(getUUIDFromName(player)), overlay));
        }
    }

    /**
     * NEW: Get head by player name.
     * Note: This converts name to UUID via Mojang API, so it will FAIL in offline mode.
     * Use MinotarSource for offline mode instead.
     */
    @Override
    public BaseComponent[] getHeadByName(String playerName, boolean overlay) {
        String uuid = getUUIDFromName(playerName);
        if (uuid.isEmpty()) {
            throw new IllegalArgumentException(
                "Failed to retrieve UUID for player: " + playerName + ". " +
                "This is expected in offline mode. Use MinotarSource instead."
            );
        }
        return toBaseComponent(getPixelColorsFromSkin(getPlayerSkinFromMojang(uuid), overlay));
    }

    /**
     * Get the UUID by knowing the player's name from Mojang API.
     *
     * @param offlinePlayer The player.
     * @return the UUID string.
     */
    public String getUUIDFromName(OfflinePlayer offlinePlayer) {
        return getUUIDFromName(offlinePlayer.getName());
    }

    /**
     * Get the UUID by knowing the player's name from Mojang API.
     *
     * @param playerName The player's name.
     * @return the UUID string.
     */
    public String getUUIDFromName(String playerName) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                String jsonResponse = response.toString();
                JSONObject jsonObject = new JSONObject(jsonResponse);
                return jsonObject.getString("id");
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Retrieves the URL of the players skin hosted on Mojangs session server.
     *
     * @param uuid The UUID of the player whose skin URL is to be retrieved.
     * @return A string representing the URL of the player's skin.
     */
    private String getPlayerSkinFromMojang(String uuid) {
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                String jsonResponse = response.toString();
                JSONObject jsonObject = new JSONObject(jsonResponse);
                JSONArray propertiesArray = jsonObject.getJSONArray("properties");

                for (int i = 0; i < propertiesArray.length(); i++) {
                    JSONObject property = propertiesArray.getJSONObject(i);
                    if (property.getString("name").equals("textures")) {
                        String value = property.getString("value");
                        byte[] decodedBytes = Base64.getDecoder().decode(value);
                        String decodedValue = new String(decodedBytes);
                        JSONObject textureJson = new JSONObject(decodedValue);
                        return textureJson.getJSONObject("textures").getJSONObject("SKIN").getString("url");
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return "Unable to retrieve player skin URL.";
    }
}
