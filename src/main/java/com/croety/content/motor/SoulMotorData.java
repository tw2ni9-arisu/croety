package com.croety.content.motor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** 跨维度记录已施法的马达；卸载区块中的淘汰和到期记录会等待区块加载再删除。 */
public class SoulMotorData extends SavedData {
    public static final long LIFETIME = 20L * 60 * 10;
    private final List<Entry> entries = new ArrayList<>();
    private long nextId = 1;

    public static SoulMotorData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SoulMotorData::load, SoulMotorData::new, "croety_soul_motors");
    }

    public long add(UUID owner, ServerLevel level, BlockPos pos) {
        long id = nextId++;
        entries.add(new Entry(id, owner, level.dimension().location().toString(), pos.immutable(),
                level.getServer().overworld().getGameTime() + LIFETIME, false));
        entries.stream().filter(e -> e.owner.equals(owner) && !e.removed)
                .sorted(Comparator.comparingLong(e -> e.id))
                .limit(Math.max(0, activeCount(owner) - 3))
                .forEach(e -> e.removed = true);
        setDirty();
        return id;
    }

    public int activeCount(UUID owner) {
        return (int) entries.stream().filter(e -> e.owner.equals(owner) && !e.removed).count();
    }

    public boolean contains(long id) {
        return entries.stream().anyMatch(e -> e.id == id && !e.removed);
    }

    public void forget(long id) {
        if (entries.removeIf(e -> e.id == id))
            setDirty();
    }

    public void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (Entry entry : List.copyOf(entries)) {
            if (now >= entry.expiresAt && !entry.removed) {
                entry.removed = true;
                setDirty();
            }
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(entry.dimension));
            ServerLevel level = server.getLevel(key);
            if (level == null || !level.hasChunkAt(entry.pos))
                continue;
            if (level.getBlockEntity(entry.pos) instanceof SoulMotorBlockEntity motor && motor.getRecordId() == entry.id) {
                if (entry.removed)
                    motor.dissolve();
            } else {
                forget(entry.id);
                continue;
            }
            if (entry.removed)
                forget(entry.id);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("NextId", nextId);
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            CompoundTag row = new CompoundTag();
            row.putLong("Id", entry.id);
            row.putUUID("Owner", entry.owner);
            row.putString("Dimension", entry.dimension);
            row.putLong("Pos", entry.pos.asLong());
            row.putLong("Expires", entry.expiresAt);
            row.putBoolean("Removed", entry.removed);
            list.add(row);
        }
        tag.put("Motors", list);
        return tag;
    }

    private static SoulMotorData load(CompoundTag tag) {
        SoulMotorData data = new SoulMotorData();
        data.nextId = Math.max(1, tag.getLong("NextId"));
        ListTag list = tag.getList("Motors", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag row = list.getCompound(i);
            if (row.hasUUID("Owner"))
                data.entries.add(new Entry(row.getLong("Id"), row.getUUID("Owner"), row.getString("Dimension"),
                        BlockPos.of(row.getLong("Pos")), row.getLong("Expires"), row.getBoolean("Removed")));
        }
        return data;
    }

    private static class Entry {
        final long id;
        final UUID owner;
        final String dimension;
        final BlockPos pos;
        final long expiresAt;
        boolean removed;

        Entry(long id, UUID owner, String dimension, BlockPos pos, long expiresAt, boolean removed) {
            this.id = id;
            this.owner = owner;
            this.dimension = dimension;
            this.pos = pos;
            this.expiresAt = expiresAt;
            this.removed = removed;
        }
    }
}
