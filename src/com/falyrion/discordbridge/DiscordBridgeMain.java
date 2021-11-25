package com.falyrion.discordbridge;

import commands.Cmd_DiscordLink;
import gamelistener.GameEventListener_Chat;
import gamelistener.GameEventListener_Death;
import gamelistener.GameEventListener_Join;
import gamelistener.GameEventListener_Quit;
import gamelistener.GameEventListener_SayCommand;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONValue;

import javax.security.auth.login.LoginException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;


public class DiscordBridgeMain extends JavaPlugin implements Listener {

    private static DiscordBridgeMain instance;

    public static DiscordBridgeMain getInstance() {
        return instance;
    }

    public String discordLink;
    public String discordInfoMsg;
    public String readChannelID;
    private String writeChannelID;
    private String serverStartMessage;
    private String serverStopMessage;
    private String playerJoinMessage;
    private String playerQuitMessage;
    private String playerDeathMessage;
    private String playerMessageToGame;
    private String playerMessageToDiscord;
    private boolean enableCmdLink;
    private boolean botLoaded = false;

    FileConfiguration config = getConfig();

    private final Logger log = getLogger();

    DiscordBot bot = new DiscordBot();


    @Override
    public void onEnable() {

        instance = this;

        // Create config file if not existing
        this.saveDefaultConfig();

        // -------------------------------------------------------------------------------------------------------------
        // Read values from config

        String discordBotToken = config.getString("clientID");
        readChannelID = config.getString("textChannelRead");
        writeChannelID = config.getString("textChannelWrite");
        discordLink = config.getString("discordLink");
        discordInfoMsg = config.getString("discordInfoMessage");
        enableCmdLink = config.getBoolean("enableLinkCommand");

        // -------------------------------------------------------------------------------------------------------------
        // Bot related

        if (discordBotToken == null || readChannelID == null || writeChannelID == null) {
            log.warning("Config file not available or invalid.");
        } else if (discordBotToken.equals("0") || readChannelID.equals("0") || writeChannelID.equals("0")) {
            log.warning("Bot data not set up in config file yet. Can not start bot. Please update the token and channel IDs in the config file and restart your server.");
        } else {
            log.info("Read channel ID: " + readChannelID + "; write channel ID: " + writeChannelID);

            // Create and load discord bot
            boolean botSuccessful = true;
            try {
                bot.createBot(discordBotToken);
            } catch (LoginException e) {
                botSuccessful = false;
                log.warning("Failed to start bot. Check if your token is correct.");
                e.printStackTrace();
            }

            // If bot successfully started, register in game chat events
            if (botSuccessful) {

                // Register chat events
                Bukkit.getServer().getPluginManager().registerEvents(new GameEventListener_Chat(), this);
                Bukkit.getServer().getPluginManager().registerEvents(new GameEventListener_SayCommand(), this);

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

                if (config.getBoolean("callPlayerDeath")) {
                    playerDeathMessage = config.getString("playerDeathMessage");
                    if (playerDeathMessage == null) {
                        // Probably v1.0.0.0 config, use implicit default from that version
                        playerDeathMessage = "**%s**";
                    }
                }

                botLoaded = true;

            }

        }

        // -------------------------------------------------------------------------------------------------------------
        // Enable Commands

        if (enableCmdLink) {
            getCommand("discord").setExecutor(new Cmd_DiscordLink());
        }

    }

    @Override
    public void onDisable() {

        // Send shut down message to discord if bot is available
        if (botLoaded) {
            sendMessageToDiscord(null, null, 2);
        }
    }

    /**
     * Sends a message via console command into the game chat
     * @param msg: String, Message to send in the game chat
     * @throws ExecutionException: ExecutionException
     * @throws InterruptedException: InterruptedException
     */
    public void sendMessageToGame(String msg, String author) throws ExecutionException, InterruptedException {
        String safeAuthor = sanitizeMessageForGame(author);
        String safeMsg = sanitizeMessageForGame(msg);
        String fullMessage = applyTemplate(playerMessageToGame, safeMsg, safeAuthor);

        Bukkit.getScheduler().callSyncMethod( this, () -> Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "tellraw @a \"" + fullMessage + "\"")
        ).get();
        
        log.info("<" + author + "> " + msg);
    }

    /**
     * Sanitizes user input to from Discord chat ensure it is a valid JSON string and can safely be inserted into Minecraft chat.
     * @param unsafe: String, The raw data to be sanitized
     * @return String, The escaped data
     */
    private String sanitizeMessageForGame(String unsafe) {
        return JSONValue.escape(ChatColor.stripColor(unsafe));
    }

    /**
     * Sends a message into the discord chat. Calls method in bot class.
     * @param userMsg: String, Message
     * @param type: int, Message Type (0=default, 1=serverStart, 2=serverStop, 3=playerJoin, 4=playerQuit,
     *            5=playerDeath)
     */
    public void sendMessageToDiscord(String userMsg, String userName, int type) {

        if (userMsg != null && !userMsg.isEmpty()) {
            userMsg = sanitizeMessageForDiscord(userMsg);
        }

        if (userName != null && !userName.isEmpty()) {
            userName = sanitizeMessageForDiscord(userName);
        }

        String msgTemplate = switch (type) {
            case 0 -> playerMessageToDiscord;
            case 1 -> serverStartMessage;
            case 2 -> serverStopMessage;
            case 3 -> playerJoinMessage;
            case 4 -> playerQuitMessage;
            case 5 -> playerDeathMessage;
            default -> throw new IllegalArgumentException("Unexpected type: " + type);
        };

        String discordMsg = applyTemplate(msgTemplate, userMsg, userName);

        bot.sendMessageOnChannel(writeChannelID, discordMsg);
    }

    /**
     * Sanitizes user input to ensure it can safely be inserted into Discord chat.
     * @param unsafe: String; The raw data to be sanitized
     * @return String; The escaped data
     */
    private String sanitizeMessageForDiscord(String unsafe) {
        return ChatColor.stripColor(unsafe);
    }

    /**
     * Inserts given message and author information into the provided template.
     * @param template: String; The template to use
     * @param safeMsg: String; Sanitized message, or {@code null} to ignore <tt>%s</tt>
     * @param safeAuthor: String; Sanitized author name, or {@code null} to ignore <tt>%p</tt>
     * @return String; The constructed message
     */
    private String applyTemplate(String template, String safeMsg, String safeAuthor) {
        if (safeMsg == null && safeAuthor == null) {
            return template;
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);

            // Attempt to interpret the character as the beginning of the sequence
            if (c == '%' && i != template.length() - 1) {

                // Whether the '%' and the following char should be appended themselves
                boolean consume = true;

                switch (template.charAt(i + 1)) {

                    // %% - this is an escaped single '%'
                    case '%':
                        result.append("%");
                        break;

                    // %s - replace with message
                    case 's':
                        if (safeMsg != null) {
                            result.append(safeMsg);
                        } else {
                            consume = false;
                        }
                        break;

                    // %p - replace with author
                    case 'p':
                        if (safeAuthor != null) {
                            result.append(safeAuthor);
                        } else {
                            consume = false;
                        }
                        break;

                    // Someone placed a single % - this is not a sequence
                    default:
                        consume = false;
                        break;
                }

                if (consume) {
                    // Skip '%' and the following character
                    i += 1;
                    continue;
                }
            }

            result.append(c);
        }

        return result.toString();
    }

}
