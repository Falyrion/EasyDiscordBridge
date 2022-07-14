package gamelistener;

import com.falyrion.discordbridge.DiscordBridgeMain;

import java.util.Locale;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;


public class GameEventListener_SayCommand implements Listener {

    /**
     * Event Listener to register commands issued by non-players. When a /say command is issued, send its message to discord
     * @param event: ServerCommandEvent
     */
    @EventHandler
    public void onServerIssuedCommand(ServerCommandEvent event) {

        handleCommandIfApplicable(event.getSender().getName(), event.getCommand());

    }
    
    /*
     *  TODO: SAFELY handle /say when issued by a player.
     *  If using PlayerCommandPreprocessEvent and PlayerChatEvent, some permissions check needs to be performed before broadcasting the message to Discord.
     */

    private void handleCommandIfApplicable(String sender, String commandWithArgs) {
        
        String prefixableCommandWithArgs = commandWithArgs.toLowerCase(Locale.ENGLISH);
        final String expectedPrefix = "say ";
        
        if (!prefixableCommandWithArgs.startsWith(expectedPrefix)) {
            return;
        }
        
        String message = commandWithArgs.substring(expectedPrefix.length());
        
        DiscordBridgeMain.getInstance().sendMessageToDiscord(message, "[" + sender + "]", 0);
    }

}

