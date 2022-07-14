package gamelistener;

import com.falyrion.discordbridge.DiscordBridgeMain;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;


public class GameEventListener_Chat implements Listener {

    /**
     * Event Listener to register chat events in game and send it as a message to discord
     * @param event: AsyncPlayerChatEvent
     */
    @EventHandler
    public void onChatMessageReceived(AsyncPlayerChatEvent event) {

        String msg = event.getMessage();
        String author = event.getPlayer().getDisplayName();

        DiscordBridgeMain.getInstance().sendMessageToDiscord(msg, author, 0);

    }

}

