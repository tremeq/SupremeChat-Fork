package net.devscape.project.supremechat.chathead.sources;

import net.devscape.project.supremechat.chathead.SkinSource;
import net.devscape.project.supremechat.chathead.SkinSourceEnum;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.OfflinePlayer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * SkinSource implementation to retrieve heads from Minotar.
 * BEST CHOICE for offline mode servers - supports both UUID and username retrieval.
 */
public class MinotarSource extends SkinSource {

    public MinotarSource(boolean useUUIDWhenRetrieve) {
        super(SkinSourceEnum.MINOTAR, true, useUUIDWhenRetrieve);
    }

    public MinotarSource() {
        super(SkinSourceEnum.MINOTAR, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseComponent[] getHead(OfflinePlayer player, boolean overlay) {
        String[] colors = new String[64];
        try {
            String baseUrl = "https://minotar.net/";
            String endpoint = overlay ? "helm" : "avatar";
            String uuidOrUsername = useUUIDWhenRetrieve() ?
                player.getUniqueId().toString().replace("-", "").trim() :
                player.getName();
            String imageUrl = baseUrl + endpoint + "/" + uuidOrUsername + "/8.png";

            BufferedImage skinImage = ImageIO.read(new URL(imageUrl));
            int faceWidth = 8, faceHeight = 8;

            int index = 0;
            for (int x = 0; x < faceHeight; x++) {
                for (int y = 0; y < faceWidth; y++) {
                    colors[index++] = String.format("#%06X", (skinImage.getRGB(x, y) & 0xFFFFFF));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return toBaseComponent(colors);
    }

    /**
     * NEW: Get head by player name - perfect for offline mode.
     * This method bypasses UUID issues by using the player's name directly.
     *
     * @param playerName The player's name
     * @param overlay Whether to include the second skin layer (helmet)
     * @return BaseComponent array representing the 8x8 pixel head
     */
    @Override
    public BaseComponent[] getHeadByName(String playerName, boolean overlay) {
        String[] colors = new String[64];
        try {
            String baseUrl = "https://minotar.net/";
            String endpoint = overlay ? "helm" : "avatar";
            String imageUrl = baseUrl + endpoint + "/" + playerName + "/8.png";

            BufferedImage skinImage = ImageIO.read(new URL(imageUrl));
            int faceWidth = 8, faceHeight = 8;

            int index = 0;
            for (int x = 0; x < faceHeight; x++) {
                for (int y = 0; y < faceWidth; y++) {
                    colors[index++] = String.format("#%06X", (skinImage.getRGB(x, y) & 0xFFFFFF));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return toBaseComponent(colors);
    }
}
