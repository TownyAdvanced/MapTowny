/*
 * Copyright (c) 2024 Silverwolfg11
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

package me.silverwolfg11.maptowny.platform;

/**
 * Provides an observer interface for map-platform events.
 * These functions may be called on any thread.
 * <br><b>Note: Platforms may not implement all events.</b>
 *
 * @since 3.0.0
 */
public interface MapPlatformObserver {
    /**
     * Run initial logic for the observer.
     * <br>
     * Called immediately if the map-platform is already enabled
     * or delayed until the map-platform is enabled for the first time.
     * Only runs once after the observer is registered (not re-run on platform re-enable).
     */
    default void onObserverSetup() {}

    /**
     * Called when the map-platform is enabling.
     * <br><br>
     * If the map-platform is already enabled when the observer is registered, then this is not called.
     * If {@link #onObserverSetup()} has not been called when the platform is enabling,
     * then {@link #onObserverSetup()} will be called and this will not.
     * <br><br>
     * At this stage, some platforms may not have started marker processing
     * and may not have process queues, so to be safe avoid
     * performing platform processing logic in this event.
     */
    default void onPlatformEnabled() {}

    /**
     * Called when the map-platform is disabling.
     * <br>
     * At this stage, some platforms may have already stopped marker processing,
     * so to be safe, avoid performing platform processing logic in this event.
     */
    default void onPlatformDisabled() {}
}
