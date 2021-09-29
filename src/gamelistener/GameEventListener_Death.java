package gamelistener;

import com.falyrion.discordbridge.DiscordBridgeMain;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class GameEventListener_Death implements Listener {

    /**
     * Event Listener to register player death events in game and send it as a message to discord
     * @param event: PlayerDeathEvent
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        String deathMessage = event.getDeathMessage();

        DiscordBridgeMain.getInstance().sendMessageToDiscord(deathMessage, null, 5);

    }

}
