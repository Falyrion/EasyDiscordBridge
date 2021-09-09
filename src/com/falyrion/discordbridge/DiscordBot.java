package com.falyrion.discordbridge;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.concurrent.ExecutionException;


public class DiscordBot extends ListenerAdapter {

    private static JDA discordBot;

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
     * Event Listener for onReady-Event
     * @param event: ReadyEvent
     */
    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("[EasyDiscordBridge] Bot ready and logged in!");
        DiscordBridgeMain.getInstance().sendMessageToDiscord("", "", 1);
    }

    /**
     * Event Listener for onMessageReceived-Events
     * @param event: onMessageReceived
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        if(event.getChannel().getId().equals(DiscordBridgeMain.getInstance().readChannelID)) {
            if (!event.getMessage().getAuthor().isBot()) {

                // Get msg contents
                Message msg = event.getMessage();
                String msgContentRaw = msg.getContentRaw();

                // Get msg author
                String author = event.getAuthor().getName();

                // Call method in main class to send message in game
                try {
                    DiscordBridgeMain.getInstance().sendMessageToGame(msgContentRaw, author);
                } catch (ExecutionException e) {
                    System.out.println("[EasyDiscordBridge] ExecutionException. Full error log:");
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    System.out.println("[EasyDiscordBridge] InterruptedException. Full error log:");
                    e.printStackTrace();
                }
            }
        }
    }

}

