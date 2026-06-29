package com.plusls.carpet.mixin.rule.pcaSyncProtocol.block;

import com.plusls.carpet.ModInfo;
import com.plusls.carpet.PcaSettings;
import com.plusls.carpet.network.PcaSyncProtocol;
import com.plusls.carpet.util.PcaBlockEntityDirtyHook;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BeehiveBlockEntity.class)
public abstract class MixinBeehiveBlockEntity extends BlockEntity implements PcaBlockEntityDirtyHook
{
	public MixinBeehiveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	@Override
	public void pca$onMarkDirty()
	{
		if (PcaSettings.pcaSyncProtocol && PcaSyncProtocol.syncBlockEntityToClient(this))
		{
			ModInfo.LOGGER.debug("update BeehiveBlockEntity: {}", this.worldPosition);
		}
	}

	@Inject(
			method = "tickOccupants",
			at = @At(value = "INVOKE", target = "Ljava/util/Iterator;remove()V", shift = At.Shift.AFTER)
	)
	private static void postTickOccupants(Level level, BlockPos pos, BlockState state, List<?> bees, BlockPos flowerPos, CallbackInfo ci)
	{
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity != null && PcaSettings.pcaSyncProtocol && PcaSyncProtocol.syncBlockEntityToClient(blockEntity))
		{
			ModInfo.LOGGER.debug("update BeehiveBlockEntity: {}", blockEntity.getBlockPos());
		}
	}

	@Inject(method = "releaseAllOccupants", at = @At(value = "RETURN"))
	private void postReleaseAllOccupants(CallbackInfoReturnable<List<Entity>> cir)
	{
		if (PcaSettings.pcaSyncProtocol && PcaSyncProtocol.syncBlockEntityToClient(this) && cir.getReturnValue() != null)
		{
			ModInfo.LOGGER.debug("update BeehiveBlockEntity: {}", this.worldPosition);
		}
	}

	@Inject(method = "loadAdditional", at = @At(value = "RETURN"))
	private void postLoadAdditional(ValueInput input, CallbackInfo ci)
	{
		if (PcaSettings.pcaSyncProtocol && PcaSyncProtocol.syncBlockEntityToClient(this))
		{
			ModInfo.LOGGER.debug("update BeehiveBlockEntity: {}", this.worldPosition);
		}
	}
}
