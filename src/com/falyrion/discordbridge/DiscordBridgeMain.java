package com.falyrion.discordbridge;

import gamelistener.GameEventListener_Chat;
import gamelistener.GameEventListener_Death;
import gamelistener.GameEventListener_Join;
import gamelistener.GameEventListener_Quit;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.util.concurrent.ExecutionException;


public class DiscordBridgeMain extends JavaPlugin implements Listener {

    private static DiscordBridgeMain instance;

    public static DiscordBridgeMain getInstance() {
        return instance;
    }

    public String readChannelID;
    private String writeChannelID;
    private String serverStartMessage;
    private String serverStopMessage;
    private String playerJoinMessage;
    private String playerQuitMessage;
    private String playerMessageToGame;
    private String playerMessageToDiscord;

    FileConfiguration config = getConfig();

    DiscordBot bot = new DiscordBot();


    @Override
    public void onEnable() {

        System.out.println("[EasyDiscordBridge] starting plugin...");

        instance = this;

        // Create config file if not existing
        this.saveDefaultConfig();

        // Read values from config
        String discordBotToken = config.getString("clientID");
        readChannelID = config.getString("textChannelRead");
        writeChannelID = config.getString("textChannelWrite");

        if (discordBotToken == null || readChannelID == null || writeChannelID == null) {
            System.out.println("[EasyDiscordBridge] Config file not available or invalid.");
        } else if (discordBotToken.equals("0") || readChannelID.equals("0") || writeChannelID.equals("0")) {
            System.out.println("[EasyDiscordBridge] Bot data not set up in config file yet. Can not start bot.");
        } else {
            System.out.println("[EasyDiscordBridge] Loaded config:");
            System.out.println("[EasyDiscordBridge] The bot will read from channel ID: " + readChannelID);
            System.out.println("[EasyDiscordBridge] The bot will write to channel ID: " + writeChannelID);

            // Create and load discord bot
            boolean botSuccessful = true;
            try {
                System.out.println("[EasyDiscordBridge] Trying to start bot...");
                bot.createBot(discordBotToken);
            } catch (LoginException e) {
                botSuccessful = false;
                System.out.println("[EasyDiscordBridge] Failed to start bot. Check if your token is correct.");
                e.printStackTrace();
            }

            // If bot successfully started, register in game chat events
            if (botSuccessful) {

                // Register chat events
                Bukkit.getServer().getPluginManager().registerEvents(new GameEventListener_Chat(), this);

                // Register other events if enabled in config
                if (config.getBoolean("callPlayerJoin")) {
                    Bukkit.getServer().getPluginManager().registerEvents(new GameEventListener_Join(), this);
                }
                if (config.getBoolean("callPlayerQuit")) {
                    Bukkit.getServer().getPluginManager().registerEvents(new GameEventListener_Quit(), this);
                }
                if (config.getBoolean("callPlayerDeath")) {
                    Bukkit.getServer().getPluginManager().registerEvents(new GameEventListener_Death(), this);
                }

                // Read other config values
                playerMessageToGame = config.getString("playerMessageToGame");
                playerMessageToDiscord = config.getString("playerMessageToDiscord");

                if (config.getBoolean("callServerStart")) {
                    serverStartMessage = config.getString("serverStartMessage");
                }

                if (config.getBoolean("callServerStop")) {
                    serverStopMessage = config.getString("serverStopMessage");
                }

                if (config.getBoolean("callPlayerJoin")) {
                    playerJoinMessage = config.getString("playerJoinMessage");
                }

                if (config.getBoolean("callPlayerQuit")) {
                    playerQuitMessage = config.getString("playerQuitMessage");
                }


            }

        }

        System.out.println("[EasyDiscordBridge] Plugin v1.0.0.0 enabled");

    }

    @Override
    public void onDisable() {

        // Send shut down message to discord
        sendMessageToDiscord("", "", 2);

        System.out.println("[EasyDiscordBridge] Plugin v1.0.0.0 disabled");
    }

    /**
     * Sends a message via console command into the game chat
     * @param msg: String, Message to send in the game chat
     * @throws ExecutionException: ExecutionException
     * @throws InterruptedException: InterruptedException
     */
    public void sendMessageToGame(String msg, String author) throws ExecutionException, InterruptedException {

        String fullMessage = playerMessageToGame.replace("%p", author);
        String finalFullMessage = fullMessage.replace("%s", msg);

        Bukkit.getScheduler().callSyncMethod( this, () -> Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "tellraw @a \"" + finalFullMessage + "\"")
        ).get();

    }

    /**
     * Sends a message into the discord chat. Calls method in bot class.
     * @param msg: String, Message
     * @param type: int, Message Type (0=default, 1=serverStart, 2=serverStop, 3=playerJoin, 4=playerQuit,
     *            5=playerDeath)
     */
    public void sendMessageToDiscord(String msg, String userName, int type) {

        switch (type) {

            // default
            case 0 -> {
                String chatMsg = playerMessageToDiscord.replace("%p", userName);
                chatMsg = chatMsg.replace("%s", msg);
                bot.sendMessageOnChannel(writeChannelID, chatMsg);
            }

            // Server start and stop
            case 1 -> bot.sendMessageOnChannel(writeChannelID, serverStartMessage);
            case 2 -> bot.sendMessageOnChannel(writeChannelID, serverStopMessage);

            // Player join
            case 3 -> {
                String joinMsg = playerJoinMessage.replace("%p", userName);
                bot.sendMessageOnChannel(writeChannelID, joinMsg);
            }

            // Player quit
            case 4 -> {
                String quitMsg = playerQuitMessage.replace("%p", userName);
                bot.sendMessageOnChannel(writeChannelID, quitMsg);
            }

            // Player death
            case 5 -> bot.sendMessageOnChannel(writeChannelID, msg);
        }
    }


}

