/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Lundegaard a.s.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package eu.lundegaard.liferay.db.setup;

import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.junit.Before;
import org.junit.Test;
import eu.lundegaard.liferay.db.setup.domain.Setup;
import org.junit.jupiter.api.Assertions;


public class MarshallUtilTest extends MockTestCore {

    private InputStream stream;
    private InputStream streamInvalid;

    @Before
    public void setUp() throws Exception {
        stream = Files.newInputStream(validConfiguration.toPath());
        streamInvalid = Files.newInputStream(invalidConfiguration.toPath());
    }

    @Test
    public void testUnmarshallFile() throws Exception {
        Setup setup = MarshallUtil.unmarshall(validConfiguration);
        assertNotNull(setup);
        assertNotNull(setup.getConfiguration());
        assertNotNull(setup.getConfiguration().getRunasuser());
    }

    @Test
    public void testUnmarshallInputStream() throws Exception {
        Setup setup = MarshallUtil.unmarshall(stream);
        assertNotNull(setup);
        assertNotNull(setup.getConfiguration());
        assertNotNull(setup.getConfiguration().getRunasuser());
    }

    @Test
    public void validateAgainstXSD() {
        boolean b;
        try {
            b = MarshallUtil.validateAgainstXSD(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Assertions.assertTrue(b);
    }

    @Test
    public void validateAgainstXSD_Invalid() {
        boolean b;
        try {
            b = MarshallUtil.validateAgainstXSD(streamInvalid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertFalse(b);
    }
}
