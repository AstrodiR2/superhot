package ua.superhot;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SuperHotMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("superhot");

    private final Map<UUID, Vec3> lastPositions = new HashMap<>();
    private final Map<UUID, Float> lastYaw = new HashMap<>();

    private static final double MOVE_THRESHOLD = 0.01;
    private static final float LOOK_THRESHOLD = 1.5f;
    private boolean wasMoving = true;

    @Override
    public void onInitialize() {
        LOGGER.info("SuperHot mod loaded — you move, time moves.");
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        boolean anyPlayerMoving = false;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            Vec3 pos = player.position();
            float yaw = player.getYRot();

            Vec3 lastPos = lastPositions.get(id);
            Float lastY = lastYaw.get(id);

            if (lastPos != null && lastY != null) {
                double moved = pos.distanceToSqr(lastPos);
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

        // Тільки при зміні стану — запускаємо gamerule
        if (anyPlayerMoving != wasMoving) {
            String val = anyPlayerMoving ? "true" : "false";
            server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withSuppressedOutput(),
                "gamerule doDaylightCycle " + val
            );
            server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withSuppressedOutput(),
                "gamerule doWeatherCycle " + val
            );
            server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withSuppressedOutput(),
                "gamerule doMobSpawning " + val
            );
            wasMoving = anyPlayerMoving;
        }

        for (var level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ServerPlayer) continue;

                if (!anyPlayerMoving) {
                    entity.setTicksFrozen(Integer.MAX_VALUE);
                    entity.setDeltaMovement(Vec3.ZERO);
                    entity.setNoGravity(true);
                } else {
                    if (entity.getTicksFrozen() > 0) {
                        entity.setTicksFrozen(0);
                    }
                    entity.setNoGravity(false);
                }
            }
        }
    }
}
