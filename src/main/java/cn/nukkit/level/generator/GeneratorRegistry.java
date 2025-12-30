package cn.nukkit.level.generator;

import java.util.Map;

public final class GeneratorRegistry {

    private static final Map<String, Integer> GENERATORS;

    static {
        GENERATORS = Map.of(
                "void", Generator.TYPE_VOID,
                "normal", Generator.TYPE_OLD,
                "flat", Generator.TYPE_FLAT
        );
    }

    private GeneratorRegistry() {
    }

    public static boolean exists(String name) {
        return GENERATORS.containsKey(name.toLowerCase());
    }

    public static int get(String name) {
        return GENERATORS.get(name.toLowerCase());
    }

    public static Map<String, Integer> getAll() {
        return GENERATORS;
    }
}
