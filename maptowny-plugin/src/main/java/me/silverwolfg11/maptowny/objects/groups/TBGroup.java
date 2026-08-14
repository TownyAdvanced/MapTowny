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

import me.silverwolfg11.maptowny.objects.MarkerOptions;
import me.silverwolfg11.maptowny.objects.StaticTB;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * TBGroups are a collection of townblocks grouped by unique properties.
 * Each group has an associated styling function that can modify marker options
 * when the group is rendered.
 * <br><br>
 * Methods will return an immutable view to the data.
 * <br></br>
 * Pre-conditions:
 * <br>- There must be at least one townblock within the group.
 * <br>- All {@link StaticTB}s within the group must be in the same world.
 */
public record TBGroup(@NotNull List<StaticTB> townblocks,
                      @NotNull Consumer<MarkerOptions.Builder> postGroupingStyling) implements Cloneable {

    @Override
    public TBGroup clone() {
        return new TBGroup(new ArrayList<>(this.townblocks), this.postGroupingStyling);
    }

    public static class Builder implements Cloneable {
        private List<StaticTB> townblocks = new ArrayList<>();
        private Consumer<MarkerOptions.Builder> postGroupingStyling = builder -> {};

        private Builder() {
        }

        // All parameters in this constructor are copied.
        private Builder(List<StaticTB> townblocks, Consumer<MarkerOptions.Builder> postGroupingStyling) {
            this.townblocks = new ArrayList<>(townblocks);
            this.postGroupingStyling = postGroupingStyling;
        }

        public Builder addTownblock(StaticTB townblock) {
            this.townblocks.add(townblock);
            return this;
        }

        public Builder addTownblocks(Collection<StaticTB> townblocks) {
            this.townblocks.addAll(townblocks);
            return this;
        }

        public Builder postGroupingStyling(@NotNull Consumer<MarkerOptions.Builder> stylingFunc) {
            this.postGroupingStyling = stylingFunc;
            return this;
        }

        public TBGroup build() {
            return new TBGroup(Collections.unmodifiableList(townblocks), postGroupingStyling);
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return new Builder(townblocks, postGroupingStyling);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}