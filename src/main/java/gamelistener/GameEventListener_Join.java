package gamelistener;

import com.falyrion.discordbridge.DiscordBridgeMain;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class GameEventListener_Join implements Listener {

    /**
     * Event Listener to register player join events in game and send it as a message to discord
     * @param event: PlayerJoinEvent
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        String playerName = event.getPlayer().getDisplayName();

        DiscordBridgeMain.getInstance().sendMessageToDiscord(null, playerName, 3);

    }

}
