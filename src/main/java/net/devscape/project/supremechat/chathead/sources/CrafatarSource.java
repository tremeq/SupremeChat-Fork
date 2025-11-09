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
 * SkinSource implementation to retrieve heads from Crafatar.
 * Note: Crafatar ONLY supports UUID retrieval, not suitable for offline mode.
 */
public class CrafatarSource extends SkinSource {

    public CrafatarSource(boolean useUUIDWhenRetrieve) {
        super(SkinSourceEnum.CRAFATAR, false, useUUIDWhenRetrieve);
    }

    public CrafatarSource() {
        super(SkinSourceEnum.CRAFATAR, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseComponent[] getHead(OfflinePlayer player, boolean overlay) {
        if (!hasUsernameSupport() && !useUUIDWhenRetrieve()) {
            throw new UnsupportedOperationException("CrafatarSource does not support username to retrieve player heads");
        }

        String[] colors = new String[64];
        try {
            String url = "https://crafatar.com/avatars/" + player.getUniqueId() + "?size=8";
            if (overlay) url += "&overlay";
            BufferedImage skinImage = ImageIO.read(new URL(url));
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
     * Crafatar does NOT support username-based retrieval.
     * This method will throw an exception.
     */
    @Override
    public BaseComponent[] getHeadByName(String playerName, boolean overlay) {
        throw new UnsupportedOperationException(
            "CrafatarSource does not support username-based retrieval. " +
            "Use MinotarSource or McHeadsSource for offline mode servers."
        );
    }
}
