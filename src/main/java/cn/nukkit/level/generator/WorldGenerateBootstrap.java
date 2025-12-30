package cn.nukkit.level.generator;

import cn.nukkit.command.defaults.WorldGenerateCommand;

public final class WorldGenerateBootstrap {

    private WorldGenerateBootstrap() {
    }

    public static WorldGenerateCommand createCommand() {
        return new WorldGenerateCommand("worldgenerate");
    }
}
