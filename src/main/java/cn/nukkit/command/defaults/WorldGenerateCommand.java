package cn.nukkit.command.defaults;

import cn.nukkit.Server;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.level.generator.GeneratorRegistry;
import cn.nukkit.utils.TextFormat;

import java.util.Random;

/**
 * @author YourName
 * Nukkit Project
 */
public class WorldGenerateCommand extends VanillaCommand {

    public WorldGenerateCommand(String name) {
        super(
                name,
                "Creates a new world",
                "/worldgenerate <world> <generator>"
        );
        this.setPermission("nukkit.command.worldgenerate");
        this.commandParameters.clear();
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(TextFormat.RED +
                    "Usage: /worldgenerate <world> <generator>");
            return true;
        }

        String worldName = args[0];
        String generatorName = args[1];

        Server server = sender.getServer();

        if (server.isLevelGenerated(worldName)) {
            sender.sendMessage(TextFormat.RED + "This world already exists!");
            return true;
        }

        if (!GeneratorRegistry.exists(generatorName)) {
            sender.sendMessage(TextFormat.RED + "Invalid generator!");
            sender.sendMessage(TextFormat.GRAY +
                    "Available: " + String.join(", ", GeneratorRegistry.getAll().keySet()));
            return true;
        }

        int generatorType = GeneratorRegistry.get(generatorName);
        long seed = new Random().nextLong();
        Class <? extends Generator> generator = Generator.getGenerator(generatorType);

        server.generateLevel(worldName, seed, generator);

        sender.sendMessage(TextFormat.GREEN +
                "World created: " + worldName + " (" + generatorName + ")");

        return true;
    }
}