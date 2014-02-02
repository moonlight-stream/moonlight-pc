package com.limelight.binding;

import com.limelight.binding.audio.JavaxAudioRenderer;
import com.limelight.binding.video.SwingCpuDecoderRenderer;
import com.limelight.binding.video.debug.DebugCpuDecoderRenderer;
import com.limelight.nvstream.av.audio.AudioRenderer;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Used for platform-specific video/audio bindings.
 *
 * @author Cameron Gutman
 */
public class PlatformBinding {
    private static final boolean DEBUG = true;

    /**
     * Gets an instance of a video decoder/renderer.
     *
     * @return a video decoder and renderer
     */
    public static VideoDecoderRenderer getVideoDecoderRenderer() {
        if (DEBUG) {
            System.out.println("Using Debug Renderer");
            return new DebugCpuDecoderRenderer();
        }
        return new SwingCpuDecoderRenderer();
    }

    /**
     * Gets the name of this device.
     * <br>Currently, the hostname of the system.
     *
     * @return the name of this device
     */
    public static String getDeviceName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "LimelightPC";
        }
    }

    /**
     * Gets an instance of an audio decoder/renderer.
     *
     * @return an audio decoder and renderer
     */
    public static AudioRenderer getAudioRenderer() {
        return new JavaxAudioRenderer();
    }
}
