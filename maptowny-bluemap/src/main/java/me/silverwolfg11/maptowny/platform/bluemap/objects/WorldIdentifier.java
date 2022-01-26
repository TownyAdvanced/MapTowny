/*
 * Copyright (c) 2022 Silverwolfg11
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.silverwolfg11.maptowny.platform.bluemap.objects;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// A world identifier class that can be passed around to use in BlueMap API
public class WorldIdentifier {
    private final String worldName;
    private final UUID worldUUID;

    public WorldIdentifier(@NotNull String worldName, @NotNull UUID worldUUID) {
        this.worldName = worldName;
        this.worldUUID = worldUUID;
    }

    @NotNull
    public String getWorldName() {
        return worldName;
    }

    @NotNull
    public UUID getWorldUUID() {
        return worldUUID;
    }

    @Nullable
    public BlueMapWorld getBlueMapWorld(BlueMapAPI api) {
        return api.getWorld(worldUUID).orElse(null);
    }

    @Nullable
    public BlueMapMap getWorldMap(BlueMapAPI api) {
        return api.getMap(worldName).orElse(null);
    }

    public static WorldIdentifier from(World world) {
        return new WorldIdentifier(world.getName(), world.getUID());
    }
}
