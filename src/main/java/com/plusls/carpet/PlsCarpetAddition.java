package com.plusls.carpet;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import com.plusls.carpet.network.PcaSyncProtocol;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PlsCarpetAddition implements ModInitializer, CarpetExtension
{
	private static SettingsManager settingsManager;

	@Override
	public void onInitialize()
	{
		CarpetServer.manageExtension(this);
	}

	@Override
	public void onGameStarted()
	{
		settingsManager = new SettingsManager(ModInfo.MOD_VERSION, "pls", "Carpet PLS Addition");
		settingsManager.parseSettingsClass(PcaSettings.class);
	}

	@Override
	public SettingsManager extensionSettingsManager()
	{
		return settingsManager;
	}

	@Override
	public void onServerLoaded(MinecraftServer server)
	{
		if (PcaSettings.pcaSyncProtocol)
		{
			PcaSyncProtocol.enablePcaSyncProtocolGlobal();
		}
	}

	@Override
	public String version()
	{
		return ModInfo.MOD_VERSION;
	}

	@Override
	public @NotNull Map<String, String> canHasTranslations(String lang)
	{
		return "zh_cn".equals(lang) ? zhCnTranslations() : enUsTranslations();
	}

	private static Map<String, String> enUsTranslations()
	{
		Map<String, String> map = new HashMap<>();
		map.put("pls.rule.pcaSyncProtocol.name", "PCA Sync Protocol");
		map.put("pls.rule.pcaSyncProtocol.desc", "Sync Entity and BlockEntity between server and client for multiplayer container preview");
		map.put("pls.rule.pcaSyncPlayerEntity.name", "PCA Sync Player Entity");
		map.put("pls.rule.pcaSyncPlayerEntity.desc", "Controls which player entities can be synced via PCA protocol");
		map.put("pls.rule.pcaSyncPlayerEntity.extra.0", "nobody: sync no player data");
		map.put("pls.rule.pcaSyncPlayerEntity.extra.1", "bot: only carpet bots");
		map.put("pls.rule.pcaSyncPlayerEntity.extra.2", "ops: bots and OPs can sync everyone (default)");
		map.put("pls.rule.pcaSyncPlayerEntity.extra.3", "ops_and_self: bots, self, and OPs can sync everyone");
		map.put("pls.rule.pcaSyncPlayerEntity.extra.4", "everyone: sync all player data");
		map.put("pls.category.PCA", "PCA");
		map.put("pls.category.protocal", "Protocol");
		return map;
	}

	private static Map<String, String> zhCnTranslations()
	{
		Map<String, String> map = new HashMap<>();
		map.put("pls.rule.pcaSyncProtocol.name", "PCA 同步协议");
		map.put("pls.rule.pcaSyncProtocol.desc", "在服务端和客户端之间同步 Entity、BlockEntity 数据，供 MasaGadget 等 mod 实现多人容器预览");
		map.put("pls.rule.pcaSyncPlayerEntity.name", "PCA 同步协议可同步玩家数据");
		map.put("pls.rule.pcaSyncPlayerEntity.desc", "决定哪些玩家的数据可以被 PCA 协议同步");
		map.put("pls.rule.pcaSyncPlayerEntity.extra.0", "nobody：所有玩家数据都无法同步");
		map.put("pls.rule.pcaSyncPlayerEntity.extra.1", "bot：仅 Carpet 召唤的 bot 可同步");
		map.put("pls.rule.pcaSyncPlayerEntity.extra.2", "ops：bot 可同步，OP 可同步所有人（默认）");
		map.put("pls.rule.pcaSyncPlayerEntity.extra.3", "ops_and_self：bot 和自己可同步，OP 可同步所有人");
		map.put("pls.rule.pcaSyncPlayerEntity.extra.4", "everyone：所有人数据都可同步");
		map.put("pls.category.PCA", "PCA");
		map.put("pls.category.protocal", "协议");
		return map;
	}
}
