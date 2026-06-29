package com.plusls.carpet;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.Identifier;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Optional;

public class ModInfo {
    public static final String MOD_ID = "carpet-pls-addition";
    public static final String MOD_PROTOCOL_ID = "pca";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static String MOD_VERSION;

    static {
        Optional<ModContainer> modContainerOptional = FabricLoader.getInstance().getModContainer(MOD_ID);
        modContainerOptional.ifPresent(modContainer -> MOD_VERSION = modContainer.getMetadata().getVersion().getFriendlyString());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_PROTOCOL_ID, path);
    }
}
