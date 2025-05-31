package io.github.headlesshq.headlessmc.launcher.util;

import io.github.headlesshq.headlessmc.api.util.AbstractUtilityTest;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;

public class IOUtilTest extends AbstractUtilityTest<IOUtil> {
    @Test
    public void testCopy() throws IOException {
        val array = new byte[]{0, 1, 2, 3};
        val is = new ByteArrayInputStream(array);
        val os = new ByteArrayOutputStream(4);
        IOUtil.copy(is, os);
        Assertions.assertArrayEquals(array, os.toByteArray());
    }

    @Test
    @SneakyThrows
    public void testReadBufferedReader() {
        val reader = new BufferedReader(new StringReader("test"));
        Assertions.assertEquals("test", IOUtil.read(reader));
    }

}
