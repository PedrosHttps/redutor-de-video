package com.redutor.video;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MainActivityTest {

    @Test
    public void targetBitrate() {
        assertEquals(5_000_000, MainActivity.targetBitrate(10_000_000, 0.5f));
        assertEquals(300_000, MainActivity.targetBitrate(100_000, 0.3f)); // piso mínimo
        assertEquals(Integer.MAX_VALUE, MainActivity.targetBitrate(Long.MAX_VALUE / 2, 0.7f));
    }

    @Test
    public void outputName() {
        assertEquals("ferias_reduzido.mp4", MainActivity.outputName("ferias.MP4"));
        assertEquals("video_reduzido.mp4", MainActivity.outputName("video"));
    }
}
