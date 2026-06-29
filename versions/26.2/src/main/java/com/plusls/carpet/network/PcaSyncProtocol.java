package com.plusls.carpet.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.plusls.carpet.ModInfo;
import com.plusls.carpet.PcaMod;
import com.plusls.carpet.PcaSettings;
import com.plusls.carpet.fakefapi.PacketSender;
import com.plusls.carpet.util.CarpetHelper;
import com.plusls.carpet.util.EntityUtils;
import io.netty.buffer.Unpooled;
import me.fallenbreath.fanetlib.api.event.FanetlibServerEvents;
import me.fallenbreath.fanetlib.api.packet.FanetlibPackets;
import me.fallenbreath.fanetlib.api.packet.PacketCodec;
import me.fallenbreath.fanetlib.api.packet.PacketHandlerS2C;
import me.fallenbreath.fanetlib.api.packet.PacketId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.TagValueOutput;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class PcaSyncProtocol {

    public static final List<Identifier> ALL_PACKET_IDS = Lists.newArrayList();

    public static final ReentrantLock lock = new ReentrantLock(true);
    public static final ReentrantLock pairLock = new ReentrantLock(true);
    private static final Identifier ENABLE_PCA_SYNC_PROTOCOL = newId("enable_pca_sync_protocol");
    private static final Identifier DISABLE_PCA_SYNC_PROTOCOL = newId("disable_pca_sync_protocol");
    private static final Identifier UPDATE_ENTITY = newId("update_entity");
    private static final Identifier UPDATE_BLOCK_ENTITY = newId("update_block_entity");
    public static final Identifier SYNC_BLOCK_ENTITY = newId("sync_block_entity");
    public static final Identifier SYNC_ENTITY = newId("sync_entity");
    public static final Identifier CANCEL_SYNC_BLOCK_ENTITY = newId("cancel_sync_block_entity");
    public static final Identifier CANCEL_SYNC_ENTITY = newId("cancel_sync_entity");
    private static final Map<ServerPlayer, Pair<Identifier, BlockPos>> playerWatchBlockPos = new HashMap<>();
    private static final Map<ServerPlayer, Pair<Identifier, Entity>> playerWatchEntity = new HashMap<>();
    private static final Map<Pair<Identifier, BlockPos>, Set<ServerPlayer>> blockPosWatchPlayerSet = new HashMap<>();
    private static final Map<Pair<Identifier, Entity>, Set<ServerPlayer>> entityWatchPlayerSet = new HashMap<>();
    private static final MutablePair<Identifier, Entity> identifierEntityPair = new MutablePair<>();
    private static final MutablePair<Identifier, BlockPos> identifierBlockPosPair = new MutablePair<>();

    @FunctionalInterface
    private interface FapiCallback
    {
        void process(MinecraftServer server, ServerPlayer player,
                     ServerGamePacketListenerImpl handler, FriendlyByteBuf buf,
                     PacketSender responseSender);
    }

    private static final Set<Identifier> s2cIds = Sets.newHashSet();
    private static final PacketSender packetSender = new PacketSender();
    private static final AtomicBoolean registeredPackers = new AtomicBoolean();

    public static void registerPackets() {
        if (!registeredPackers.compareAndSet(false, true)) {
            return;
        }
        PacketCodec<FriendlyByteBuf> codec = PacketCodec.of(
                (p, buf) -> buf.writeBytes(p.copy()),
                buf -> {
                    FriendlyByteBuf p = new FriendlyByteBuf(Unpooled.buffer());
                    p.writeBytes(buf);
                    return p;
                }
        );

        BiConsumer<Identifier, FapiCallback> c2s = (id, cb) -> {
            FanetlibPackets.registerC2S(PacketId.of(id), codec, (buf, ctx) -> {
                cb.process(ctx.getServer(), ctx.getPlayer(), ctx.getNetworkHandler(), buf, packetSender);
            });
        };
        Consumer<Identifier> s2c = (id) -> {
            s2cIds.add(id);
            FanetlibPackets.registerS2C(PacketId.of(id), codec, PacketHandlerS2C.dummy());
        };

        c2s.accept(SYNC_BLOCK_ENTITY, PcaSyncProtocol::syncBlockEntityHandler);
        c2s.accept(SYNC_ENTITY, PcaSyncProtocol::syncEntityHandler);
        c2s.accept(CANCEL_SYNC_BLOCK_ENTITY, PcaSyncProtocol::cancelSyncBlockEntityHandler);
        c2s.accept(CANCEL_SYNC_ENTITY, PcaSyncProtocol::cancelSyncEntityHandler);

        s2c.accept(ENABLE_PCA_SYNC_PROTOCOL);
        s2c.accept(DISABLE_PCA_SYNC_PROTOCOL);
        s2c.accept(UPDATE_ENTITY);
        s2c.accept(UPDATE_BLOCK_ENTITY);
    }

    public static void init() {
        registerPackets();
        FanetlibServerEvents.registerPlayerJoinListener((svr, nh, p) -> onJoin(nh, packetSender, svr));
        FanetlibServerEvents.registerPlayerDisconnectListener((svr, nh, p) -> onDisconnect(nh, svr));
    }

    private static class ServerPlayNetworking {
        public static void send(ServerPlayer player, Identifier identifier, FriendlyByteBuf buf) {
            if (!s2cIds.contains(identifier)) {
                throw new RuntimeException("unknown identifier " + identifier);
            }
            player.connection.send(FanetlibPackets.createS2C(PacketId.of(identifier), buf));
        }
    }

    private static Identifier newId(String path) {
        Identifier id = ModInfo.id(path);
        ALL_PACKET_IDS.add(id);
        return id;
    }

    private static Identifier getDimId(Level world) {
        return world.dimension().identifier();
    }

    public static void enablePcaSyncProtocol(@NotNull ServerPlayer player) {
        ModInfo.LOGGER.debug("Try enablePcaSyncProtocol: {}", player.getName().getString());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ServerPlayNetworking.send(player, ENABLE_PCA_SYNC_PROTOCOL, buf);
        ModInfo.LOGGER.debug("send enablePcaSyncProtocol to {}!", player.getName().getString());
        lock.lock();
        lock.unlock();
    }

    public static void disablePcaSyncProtocol(@NotNull ServerPlayer player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ServerPlayNetworking.send(player, DISABLE_PCA_SYNC_PROTOCOL, buf);
        ModInfo.LOGGER.debug("send disablePcaSyncProtocol to {}!", player.getName().getString());
    }

    public static void updateEntity(@NotNull ServerPlayer player, @NotNull Entity entity) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeIdentifier(getDimId(EntityUtils.getEntityWorld(entity)));
        buf.writeInt(entity.getId());
        try (ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(entity.problemPath(), ModInfo.LOGGER)) {
            var view = TagValueOutput.createWithContext(logging, entity.registryAccess());
            entity.saveWithoutId(view);
            buf.writeNbt(view.buildResult());
        }
        ServerPlayNetworking.send(player, UPDATE_ENTITY, buf);
    }

    public static void updateBlockEntity(@NotNull ServerPlayer player, @NotNull BlockEntity blockEntity) {
        Level world = blockEntity.getLevel();

        if (world == null) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeIdentifier(getDimId(world));
        buf.writeBlockPos(blockEntity.getBlockPos());
        buf.writeNbt(blockEntity.saveWithoutMetadata(world.registryAccess()));
        ServerPlayNetworking.send(player, UPDATE_BLOCK_ENTITY, buf);
    }

    public static void onDisconnect(ServerGamePacketListenerImpl serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        if (PcaSettings.pcaSyncProtocol) {
            ModInfo.LOGGER.debug("onDisconnect remove: {}", serverPlayNetworkHandler.player.getName().getString());
        }
        PcaSyncProtocol.clearPlayerWatchData(serverPlayNetworkHandler.player);
    }

    public static void onJoin(ServerGamePacketListenerImpl serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
        if (PcaSettings.pcaSyncProtocol) {
            enablePcaSyncProtocol(serverPlayNetworkHandler.player);
        }
    }

    public static void cancelSyncBlockEntityHandler(MinecraftServer server, ServerPlayer player,
                                                     ServerGamePacketListenerImpl handler, FriendlyByteBuf buf,
                                                     PacketSender responseSender) {
        if (!PcaSettings.pcaSyncProtocol) {
            return;
        }
        ModInfo.LOGGER.debug("{} cancel watch blockEntity.", player.getName().getString());
        PcaSyncProtocol.clearPlayerWatchBlock(player);
    }

    public static void cancelSyncEntityHandler(MinecraftServer server, ServerPlayer player,
                                                ServerGamePacketListenerImpl handler, FriendlyByteBuf buf,
                                                PacketSender responseSender) {
        if (!PcaSettings.pcaSyncProtocol) {
            return;
        }
        ModInfo.LOGGER.debug("{} cancel watch entity.", player.getName().getString());
        PcaSyncProtocol.clearPlayerWatchEntity(player);
    }

    private static ServerLevel getServerWorldFromPlayer(ServerPlayer player) {
        return (ServerLevel) player.level();
    }

    public static void syncBlockEntityHandler(MinecraftServer server, ServerPlayer player,
                                               ServerGamePacketListenerImpl handler, FriendlyByteBuf buf,
                                               PacketSender responseSender) {
        if (!PcaSettings.pcaSyncProtocol) {
            return;
        }
        BlockPos pos = buf.readBlockPos();
        ServerLevel world = getServerWorldFromPlayer(player);
        BlockState blockState = world.getBlockState(pos);
        clearPlayerWatchData(player);
        ModInfo.LOGGER.debug("{} watch blockpos {}: {}", player.getName().getString(), pos, blockState);

        BlockEntity blockEntityAdj = null;
        if (blockState.getBlock() instanceof ChestBlock) {
            if (blockState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockPos posAdj = pos.relative(ChestBlock.getConnectedDirection(blockState));
                blockEntityAdj = world.getChunkAt(posAdj).getBlockEntity(posAdj);
            }
        } else if (blockState.getBlock() == Blocks.BARREL && CarpetHelper.getBoolRuleValue("largeBarrel")) {
            Direction directionOpposite = blockState.getValue(BarrelBlock.FACING).getOpposite();
            BlockPos posAdj = pos.relative(directionOpposite);
            BlockState blockStateAdj = world.getBlockState(posAdj);
            if (blockStateAdj.getBlock() == Blocks.BARREL && blockStateAdj.getValue(BarrelBlock.FACING) == directionOpposite) {
                blockEntityAdj = world.getChunkAt(posAdj).getBlockEntity(posAdj);
            }
        }

        if (blockEntityAdj != null) {
            updateBlockEntity(player, blockEntityAdj);
        }

        BlockEntity blockEntity = world.getChunkAt(pos).getBlockEntity(pos);
        if (blockEntity != null) {
            updateBlockEntity(player, blockEntity);
        }

        Pair<Identifier, BlockPos> pair = new ImmutablePair<>(getDimId(EntityUtils.getEntityWorld(player)), pos);
        lock.lock();
        playerWatchBlockPos.put(player, pair);
        if (!blockPosWatchPlayerSet.containsKey(pair)) {
            blockPosWatchPlayerSet.put(pair, new HashSet<>());
        }
        blockPosWatchPlayerSet.get(pair).add(player);
        lock.unlock();
    }

    public static void syncEntityHandler(MinecraftServer server, ServerPlayer player,
                                          ServerGamePacketListenerImpl handler, FriendlyByteBuf buf,
                                          PacketSender responseSender) {
        if (!PcaSettings.pcaSyncProtocol) {
            return;
        }
        int entityId = buf.readInt();
        ServerLevel world = getServerWorldFromPlayer(player);
        Entity entity = world.getEntity(entityId);
        if (entity == null) {
            ModInfo.LOGGER.debug("Can't find entity {}.", entityId);
        } else {
            clearPlayerWatchData(player);
            ModInfo.LOGGER.debug("{} watch entity {}: {}", player.getName().getString(), entityId, entity);
            updateEntity(player, entity);

            Pair<Identifier, Entity> pair = new ImmutablePair<>(getDimId(EntityUtils.getEntityWorld(entity)), entity);
            lock.lock();
            playerWatchEntity.put(player, pair);
            if (!entityWatchPlayerSet.containsKey(pair)) {
                entityWatchPlayerSet.put(pair, new HashSet<>());
            }
            entityWatchPlayerSet.get(pair).add(player);
            lock.unlock();
        }
    }

    private static MutablePair<Identifier, Entity> getIdentifierEntityPair(Identifier identifier, Entity entity) {
        pairLock.lock();
        identifierEntityPair.setLeft(identifier);
        identifierEntityPair.setRight(entity);
        pairLock.unlock();
        return identifierEntityPair;
    }

    private static MutablePair<Identifier, BlockPos> getIdentifierBlockPosPair(Identifier identifier, BlockPos pos) {
        pairLock.lock();
        identifierBlockPosPair.setLeft(identifier);
        identifierBlockPosPair.setRight(pos);
        pairLock.unlock();
        return identifierBlockPosPair;
    }

    private static @Nullable Set<ServerPlayer> getWatchPlayerList(@NotNull Entity entity) {
        return entityWatchPlayerSet.get(getIdentifierEntityPair(getDimId(EntityUtils.getEntityWorld(entity)), entity));
    }

    private static @Nullable Set<ServerPlayer> getWatchPlayerList(@NotNull Level world, @NotNull BlockPos blockPos) {
        return blockPosWatchPlayerSet.get(getIdentifierBlockPosPair(getDimId(world), blockPos));
    }

    public static boolean syncEntityToClient(@NotNull Entity entity) {
        if (EntityUtils.getEntityWorld(entity).isClientSide()) {
            return false;
        }
        lock.lock();
        Set<ServerPlayer> playerList = getWatchPlayerList(entity);
        boolean ret = false;
        if (playerList != null) {
            for (ServerPlayer player : playerList) {
                updateEntity(player, entity);
                ret = true;
            }
        }
        lock.unlock();
        return ret;
    }

    public static boolean syncBlockEntityToClient(@NotNull BlockEntity blockEntity) {
        boolean ret = false;
        Level world = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();
        if (world != null) {
            if (world.isClientSide()) {
                return false;
            }
            BlockState blockState = world.getBlockState(pos);
            lock.lock();
            Set<ServerPlayer> playerList = getWatchPlayerList(world, blockEntity.getBlockPos());

            Set<ServerPlayer> playerListAdj = null;

            if (blockState.getBlock() instanceof ChestBlock) {
                if (blockState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                    BlockPos posAdj = pos.relative(ChestBlock.getConnectedDirection(blockState));
                    playerListAdj = getWatchPlayerList(world, posAdj);
                }
            } else if (blockState.getBlock() == Blocks.BARREL && CarpetHelper.getBoolRuleValue("largeBarrel")) {
                Direction directionOpposite = blockState.getValue(BarrelBlock.FACING).getOpposite();
                BlockPos posAdj = pos.relative(directionOpposite);
                BlockState blockStateAdj = world.getBlockState(posAdj);
                if (blockStateAdj.getBlock() == Blocks.BARREL && blockStateAdj.getValue(BarrelBlock.FACING) == directionOpposite) {
                    playerListAdj = getWatchPlayerList(world, posAdj);
                }
            }
            if (playerListAdj != null) {
                if (playerList == null) {
                    playerList = playerListAdj;
                } else {
                    playerList.addAll(playerListAdj);
                }
            }

            if (playerList != null) {
                for (ServerPlayer player : playerList) {
                    updateBlockEntity(player, blockEntity);
                    ret = true;
                }
            }
            lock.unlock();
        }
        return ret;
    }

    private static void clearPlayerWatchEntity(ServerPlayer player) {
        lock.lock();
        Pair<Identifier, Entity> pair = playerWatchEntity.get(player);
        if (pair != null) {
            Set<ServerPlayer> playerSet = entityWatchPlayerSet.get(pair);
            playerSet.remove(player);
            if (playerSet.isEmpty()) {
                entityWatchPlayerSet.remove(pair);
            }
            playerWatchEntity.remove(player);
        }
        lock.unlock();
    }

    private static void clearPlayerWatchBlock(ServerPlayer player) {
        lock.lock();
        Pair<Identifier, BlockPos> pair = playerWatchBlockPos.get(player);
        if (pair != null) {
            Set<ServerPlayer> playerSet = blockPosWatchPlayerSet.get(pair);
            playerSet.remove(player);
            if (playerSet.isEmpty()) {
                blockPosWatchPlayerSet.remove(pair);
            }
            playerWatchBlockPos.remove(player);
        }
        lock.unlock();
    }

    public static void disablePcaSyncProtocolGlobal() {
        lock.lock();
        playerWatchBlockPos.clear();
        playerWatchEntity.clear();
        blockPosWatchPlayerSet.clear();
        entityWatchPlayerSet.clear();
        lock.unlock();
        if (PcaMod.server != null) {
            for (ServerPlayer player : PcaMod.server.getPlayerList().getPlayers()) {
                disablePcaSyncProtocol(player);
            }
        }
    }

    public static void enablePcaSyncProtocolGlobal() {
        if (PcaMod.server == null) {
            return;
        }
        for (ServerPlayer player : PcaMod.server.getPlayerList().getPlayers()) {
            enablePcaSyncProtocol(player);
        }
    }

    public static void clearPlayerWatchData(ServerPlayer player) {
        PcaSyncProtocol.clearPlayerWatchBlock(player);
        PcaSyncProtocol.clearPlayerWatchEntity(player);
    }
}
