package com.falyrion.discordbridge;

import gamelistener.GameEventListener_Chat;
import gamelistener.GameEventListener_Death;
import gamelistener.GameEventListener_Join;
import gamelistener.GameEventListener_Quit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONValue;

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
    private String playerDeathMessage;
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

                if (config.getBoolean("callPlayerDeath")) {
                    playerDeathMessage = config.getString("playerDeathMessage");
                    if (playerDeathMessage == null) {
                        // Probably v1.0.0.0 config, use implicit default from that version
                        playerDeathMessage = "**%s**";
                    }
                }
                
            }

        }

        System.out.println("[EasyDiscordBridge] Plugin v1.0.0.0 enabled");

    }

    @Override
    public void onDisable() {

        // Send shut down message to discord
        sendMessageToDiscord(null, null, 2);

        System.out.println("[EasyDiscordBridge] Plugin v1.0.0.0 disabled");
    }

    /**
     * Sends a message via console command into the game chat
     * @param msg: String, Message to send in the game chat
     * @throws ExecutionException: ExecutionException
     * @throws InterruptedException: InterruptedException
     */
    public void sendMessageToGame(String msg, String author) throws ExecutionException, InterruptedException {
        String safeAuthor = sanitizeForGame(author);
        String safeMsg = sanitizeForGame(msg);
        String fullMessage = applyTemplate(playerMessageToGame, safeMsg, safeAuthor);

        Bukkit.getScheduler().callSyncMethod( this, () -> Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "tellraw @a \"" + fullMessage + "\"")
        ).get();

    }
    
    /**
     * Sanitizes user input to ensure it is a valid JSON string and can safely be inserted into Minecraft chat.
     * @param unsafe the raw data to be sanitized
     * @return escaped data
     */
    private String sanitizeForGame(String unsafe) {
        return JSONValue.escape(ChatColor.stripColor(unsafe));
    }

    /**
     * Sends a message into the discord chat. Calls method in bot class.
     * @param msg: String, Message
     * @param type: int, Message Type (0=default, 1=serverStart, 2=serverStop, 3=playerJoin, 4=playerQuit,
     *            5=playerDeath)
     */
    public void sendMessageToDiscord(String userMsg, String userName, int type) {
        
        if (userMsg != null && !userMsg.isEmpty()) {
            userMsg = sanitizeForDiscord(userMsg);
        }
        
        if (userName != null && !userName.isEmpty()) {
            userName = sanitizeForDiscord(userName);
        }
        
        String template = switch (type) {
        case 0 -> playerMessageToDiscord;
        case 1 -> serverStartMessage;
        case 2 -> serverStopMessage;
        case 3 -> playerJoinMessage;
        case 4 -> playerQuitMessage;
        case 5 -> playerDeathMessage;
        default -> throw new IllegalArgumentException("Unexpected type: " + type);
        };
        
        String discordMsg = applyTemplate(template, userMsg, userName);
        
        bot.sendMessageOnChannel(writeChannelID, discordMsg);
    }
    
    /**
     * Sanitizes user input to ensure it is can safely be inserted into Discord chat.
     * @param unsafe the raw data to be sanitized
     * @return escaped data
     */
    private String sanitizeForDiscord(String unsafe) {
        return ChatColor.stripColor(unsafe);
    }

    /**
     * Inserts given message and author information into the provided template.
     * @param template the template to use
     * @param safeMsg sanitized message, or {@code null} to ignore <tt>%s</tt>
     * @param safeAuthor sanitized author name, or {@code null} to ignore <tt>%p</tt>
     * @return the constructed message
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

