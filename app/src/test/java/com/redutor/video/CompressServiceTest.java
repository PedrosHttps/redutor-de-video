package com.redutor.video;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CompressServiceTest {

    @Test
    public void targetBitrate() {
        assertEquals(5_000_000, CompressService.targetBitrate(10_000_000, 0.5f));
        assertEquals(300_000, CompressService.targetBitrate(100_000, 0.3f)); // piso mínimo
        assertEquals(Integer.MAX_VALUE, CompressService.targetBitrate(Long.MAX_VALUE / 2, 0.7f));
    }

    @Test
    public void estimate() {
        CompressService.Video v = new CompressService.Video(null);
        assertEquals(0, CompressService.estimate(v, 0.5f)); // duração desconhecida
        v.durationMs = 60_000;
        v.bitrate = 8_000_000;
        assertEquals(30_000_000, CompressService.estimate(v, 0.5f)); // 4 Mbps × 60 s
    }

    @Test
    public void scaledHeight() {
        assertEquals(720, CompressService.scaledHeight(1920, 1080));   // deitado
        assertEquals(1280, CompressService.scaledHeight(1080, 1920));  // em pé → 720×1280
        assertEquals(720, CompressService.scaledHeight(3840, 2160));   // 4K
        assertEquals(0, CompressService.scaledHeight(1280, 720));      // já é 720p
        assertEquals(0, CompressService.scaledHeight(0, 0));           // desconhecido
    }

    @Test
    public void outputName() {
        assertEquals("ferias_reduzido.mp4", CompressService.outputName("ferias.MP4"));
        assertEquals("clip_reduzido.mp4", CompressService.outputName("clip.mov"));
        assertEquals("video_reduzido.mp4", CompressService.outputName("video"));
    }
}
