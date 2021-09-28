package gamelistener;

import com.falyrion.discordbridge.DiscordBridgeMain;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameEventListener_Quit implements Listener {

    /**
     * Event Listener to register player quit events in game and send it as a message to discord
     * @param event: PlayerQuitEvent
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        String playerName = event.getPlayer().getDisplayName();

        DiscordBridgeMain.getInstance().sendMessageToDiscord(null, playerName, 4);

    }
}
