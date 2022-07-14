package commands;

import com.falyrion.discordbridge.DiscordBridgeMain;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Cmd_DiscordLink implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String commandLabel, String[] arguments) {

        if (commandSender instanceof Player) {
            TextComponent textComponentDiscordLink = new TextComponent(DiscordBridgeMain.getInstance().discordInfoMsg);
            textComponentDiscordLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, DiscordBridgeMain.getInstance().discordLink));
            commandSender.spigot().sendMessage(textComponentDiscordLink);
        }

        return true;

    }
}
