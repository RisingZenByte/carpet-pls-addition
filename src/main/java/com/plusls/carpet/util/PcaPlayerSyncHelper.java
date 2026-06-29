package com.plusls.carpet.util;

import carpet.patches.EntityPlayerMPFake;
import com.plusls.carpet.PcaSettings;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public final class PcaPlayerSyncHelper
{
	private PcaPlayerSyncHelper()
	{
	}

	public static boolean canSyncPlayerEntity(@NotNull MinecraftServer server, @NotNull ServerPlayer requester, @NotNull Entity target)
	{
		if (!(target instanceof Player))
		{
			return true;
		}

		return switch (PcaSettings.pcaSyncPlayerEntity)
		{
			case NOBODY -> false;
			case BOT -> target instanceof EntityPlayerMPFake;
			case OPS -> target instanceof EntityPlayerMPFake || isOp(requester);
			case OPS_AND_SELF -> target instanceof EntityPlayerMPFake || isOp(requester) || target == requester;
			case EVERYONE -> true;
		};
	}

	private static boolean isOp(ServerPlayer player)
	{
		return Commands.LEVEL_GAMEMASTERS.check(player.permissions());
	}
}
