package com.heroclock.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HeroClockAPITest {
    private Entity entity;
    private ServerLevel level;
    private MinecraftServer server;
    private CompoundTag data;

    @BeforeEach void setup() {
        entity = mock(Entity.class);
        level = mock(ServerLevel.class);
        server = mock(MinecraftServer.class);
        data = new CompoundTag();
        when(entity.level()).thenReturn(level);
        when(entity.getPersistentData()).thenReturn(data);
        when(level.getServer()).thenReturn(server);
        when(server.overworld()).thenReturn(level);
        when(server.isSameThread()).thenReturn(true);
        when(level.getGameTime()).thenReturn(100L);
    }

    @Test void readsDoNotCreateTimerData() {
        assertEquals(0, HeroClockAPI.remaining(entity, "missing"));
        assertTrue(data.isEmpty());
    }

    @Test void savedDeadlineSurvivesNbtRoundTrip() throws Exception {
        assertEquals(300, HeroClockAPI.set(entity, "power.cooldown", 200));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        NbtIo.writeCompressed(data, output);
        CompoundTag restored = NbtIo.readCompressed(new ByteArrayInputStream(output.toByteArray()));
        when(entity.getPersistentData()).thenReturn(restored);
        when(level.getGameTime()).thenReturn(200L);
        assertEquals(100, HeroClockAPI.remaining(entity, "power.cooldown"));
        when(level.getGameTime()).thenReturn(300L);
        assertTrue(HeroClockAPI.expired(entity, "power.cooldown"));
    }

    @Test void dimensionChangesUseTheSameClock() {
        HeroClockAPI.set(entity, "cooldown", 200);
        ServerLevel otherDimension = mock(ServerLevel.class);
        when(otherDimension.getServer()).thenReturn(server);
        when(otherDimension.getGameTime()).thenReturn(9000L);
        when(entity.level()).thenReturn(otherDimension);
        assertEquals(200, HeroClockAPI.remaining(entity, "cooldown"));
    }

    @Test void negativeAdditionClearsAndOverflowSaturates() {
        HeroClockAPI.set(entity, "cooldown", 20);
        assertEquals(0, HeroClockAPI.add(entity, "cooldown", Long.MIN_VALUE));
        assertTrue(data.isEmpty());
        assertEquals(Long.MAX_VALUE, HeroClockAPI.set(entity, "cooldown", Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE - 100, HeroClockAPI.remaining(entity, "cooldown"));
    }

    @Test void blockTimerWritesMarkStorageDirty() {
        BlockEntity block = mock(BlockEntity.class);
        when(block.getLevel()).thenReturn(level);
        when(block.getPersistentData()).thenReturn(data);
        HeroClockAPI.set(block, "cooldown", 20);
        HeroClockAPI.set(block, "cooldown", 20);
        verify(block, times(1)).setChanged();
        HeroClockAPI.clear(block, "cooldown");
        verify(block, times(2)).setChanged();
    }

    @Test void invalidAccessDoesNotSilentlyReturnZero() {
        assertThrows(IllegalArgumentException.class, () -> HeroClockAPI.now(new Object()));
        assertThrows(IllegalArgumentException.class, () -> HeroClockAPI.setSeconds(entity, "x", Double.NaN));
        when(server.isSameThread()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> HeroClockAPI.set(entity, "x", 20));
        assertTrue(data.isEmpty());
    }
}
