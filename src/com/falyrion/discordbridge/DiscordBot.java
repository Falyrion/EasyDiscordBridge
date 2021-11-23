package com.falyrion.discordbridge;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

import javax.security.auth.login.LoginException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DiscordBot extends ListenerAdapter {

    private static JDA discordBot;
    private final Logger log = Bukkit.getLogger();

    /**
     * Creates a discord bot client from a given token
     *
     * @param token: String, Token of the discord application (Client ID)
     * @throws LoginException: Exception
     */
    public void createBot(String token) throws LoginException {
        discordBot = JDABuilder.createDefault(token).build();
        discordBot.addEventListener(new DiscordBot());
    }

    /**
     * Sends a message in a given discord channel
     *
     * @param channelID: String, The channel ID to send the message to
     * @param msg: String, The message to send
     */
    public void sendMessageOnChannel(String channelID, String msg) {
        discordBot.getTextChannelById(channelID).sendMessage(msg).queue();
    }

    /**
     * Event Listener for onReady-Event of Discord Bot
     * @param event: ReadyEvent
     */
    @Override
    public void onReady(ReadyEvent event) {
        log.info("Bot ready and logged in!");
        DiscordBridgeMain.getInstance().sendMessageToDiscord(null, null, 1);
    }

    /**
     * Event Listener for onMessageReceived-Events
     * @param event: onMessageReceived
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        if (!event.getChannel().getId().equals(DiscordBridgeMain.getInstance().readChannelID)) {
            return;
        }
        
        if (event.getMessage().getAuthor().isBot()) {
            return;
        }

        // Get msg contents
        Message msg = event.getMessage();
        String msgContentRaw = msg.getContentRaw();

        // Get msg author
        String author = event.getAuthor().getName();

        // Call method in main class to send message in game
        try {
            DiscordBridgeMain.getInstance().sendMessageToGame(msgContentRaw, author);
        } catch (Exception e) {
            log.info("Could not handle a Discord message");
            log.info("Author is \"" + author + "\", message is \"" + msgContentRaw + "\"");
            log.log(Level.SEVERE, "An exception has occurred while handling a Discord message", e);
        }
    }

}

