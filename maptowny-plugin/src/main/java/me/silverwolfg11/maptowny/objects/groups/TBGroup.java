/*
 * Copyright (c) 2023 Silverwolfg11
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

package me.silverwolfg11.maptowny.objects.groups;

import me.silverwolfg11.maptowny.objects.StaticTB;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TBGroups are a collection of townblocks grouped by unique properties.
 * These properties may be embedded within group data.
 * <br><br>
 * Methods will return an immutable view to the data.
 * <br></br>
 * Pre-conditions:
 * <br>- There must be at least one townblock within the group.
 * <br>- All {@link StaticTB}s within the group must be in the same world.
 */
public record TBGroup(@NotNull List<StaticTB> townblocks, @NotNull Map<String, Object> groupData) implements Cloneable {

    @Override
    public TBGroup clone() {
        return new TBGroup(new ArrayList<>(this.townblocks), new HashMap<>(this.groupData));
    }

    public static class Builder implements Cloneable {
        private List<StaticTB> townblocks = new ArrayList<>();
        private Map<String, Object> groupData = new HashMap<>();

        private Builder() {
        }

        // All parameters in this constructor are copied.
        private Builder(List<StaticTB> townblocks, Map<String, Object> groupData) {
            this.townblocks = new ArrayList<>(townblocks);
            this.groupData = new HashMap<>(groupData);
        }

        public Builder addTownblock(StaticTB townblock) {
            this.townblocks.add(townblock);
            return this;
        }

        public Builder addTownblocks(Collection<StaticTB> townblocks) {
            this.townblocks.addAll(townblocks);
            return this;
        }

        public Builder addData(String key, Object value) {
            this.groupData.put(key, value);
            return this;
        }

        public Builder addData(@NotNull Map<String, Object> data) {
            if ((data != null) && (!data.isEmpty())) {
                this.groupData.putAll(data);
            }

            return this;
        }

        public TBGroup build() {
            return new TBGroup(Collections.unmodifiableList(townblocks), Collections.unmodifiableMap(groupData));
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return new Builder(townblocks, groupData);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}