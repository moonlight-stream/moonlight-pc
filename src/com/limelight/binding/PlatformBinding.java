package com.limelight.binding;

import com.limelight.binding.audio.JavaxAudioRenderer;
import com.limelight.binding.crypto.PcCryptoProvider;
import com.limelight.binding.video.GLDecoderRenderer;
import com.limelight.nvstream.av.audio.AudioRenderer;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;
import com.limelight.nvstream.http.LimelightCryptoProvider;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Used for platform-specific video/audio bindings.
 *
 * @author Cameron Gutman
 */
public class PlatformBinding {
    /**
     * Gets an instance of a video decoder/renderer.
     *
     * @return a video decoder and renderer
     */
    public static VideoDecoderRenderer getVideoDecoderRenderer() {
    	return new GLDecoderRenderer();
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

    /**
     * Gets an instance of a crypto provider
     *
     * @return a PcCryptoProvider object
     */
    public static LimelightCryptoProvider getCryptoProvider() {
        return new PcCryptoProvider();
    }
}
