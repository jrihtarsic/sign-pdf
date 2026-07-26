package org.r7c.pdf.ui;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoordinateConverterTest {

    private static final float DELTA = 0.01f;

    @Test
    void screenRectToPdfPoints_scale15() throws Exception {
        float scale = 1.5f;
        // A rectangle drawn flush against the top-left of the rendered image.
        float[] rect = CoordinateConverter.screenRectToPdfPoints(0, 60, 150, 60, scale);

        assertEquals(0f, rect[0], DELTA);
        assertEquals( 40f, rect[1], DELTA); // 60px / 1.5 = 40pt tall, f
        assertEquals(100f, rect[2], DELTA); // 150px / 1.5
        assertEquals(40f, rect[3], DELTA);  // 60px / 1.5
    }

    @Test
    void screenRectToPdfPoints_scale2() {
        float scale = 2f;
        float[] rect = CoordinateConverter.screenRectToPdfPoints(20, 1400, 60, 200, scale);

        assertEquals(10f, rect[0], DELTA);
        assertEquals(700f, rect[1], DELTA);
        assertEquals(30f, rect[2], DELTA);
        assertEquals(100f, rect[3], DELTA);
    }
}
