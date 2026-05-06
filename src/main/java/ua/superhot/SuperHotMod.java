package ua.superhot;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SuperHotMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("superhot");

    private final Map<UUID, Vec3d> lastPositions = new HashMap<>();
    private final Map<UUID, Float> lastYaw = new HashMap<>();

    private static final double MOVE_THRESHOLD = 0.01;
    private static final float LOOK_THRESHOLD = 1.5f;

    @Override
    public void onInitialize() {
        LOGGER.info("SuperHot mod loaded — you move, time moves.");
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        for (var world : server.getWorlds()) {
            boolean anyPlayerMoving = false;

            for (ServerPlayerEntity player : world.getPlayers()) {
                UUID id = player.getUuid();
                Vec3d pos = player.getPos();
                float yaw = player.getYaw();

                Vec3d lastPos = lastPositions.get(id);
                Float lastY = lastYaw.get(id);

                if (lastPos != null && lastY != null) {
                    double moved = pos.squaredDistanceTo(lastPos);
                    float looked = Math.abs(yaw - lastY);
                    if (moved > MOVE_THRESHOLD || looked > LOOK_THRESHOLD) {
                        anyPlayerMoving = true;
                    }
                } else {
                    anyPlayerMoving = true;
                }

                lastPositions.put(id, pos);
                lastYaw.put(id, yaw);
            }

            for (Entity entity : world.iterateEntities()) {
                if (entity instanceof ServerPlayerEntity) continue;

                if (!anyPlayerMoving) {
                    entity.setFrozenTicks(Integer.MAX_VALUE);
                    entity.setVelocity(Vec3d.ZERO);
                    entity.setNoGravity(true);
                } else {
                    if (entity.getFrozenTicks() > 0) {
                        entity.setFrozenTicks(0);
                    }
                    entity.setNoGravity(false);
                }
            }

            boolean finalAnyMoving = anyPlayerMoving;
            server.getCommandManager().executeWithPrefix(
                server.getCommandSource().withSilent(),
                "gamerule doDaylightCycle " + finalAnyMoving
            );
            server.getCommandManager().executeWithPrefix(
                server.getCommandSource().withSilent(),
                "gamerule doWeatherCycle " + finalAnyMoving
            );
            server.getCommandManager().executeWithPrefix(
                server.getCommandSource().withSilent(),
                "gamerule doMobSpawning " + finalAnyMoving
            );
        }
    }
}
