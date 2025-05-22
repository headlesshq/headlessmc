package io.github.headlesshq.headlessmc.launcher.instrumentation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import io.github.headlesshq.headlessmc.api.traits.HasName;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class EntryStream {
    private final InputStream stream;
    private final List<Target> targets;
    private final HasName entry;
    private boolean transformed;
    private boolean skipped;

    public static EntryStream of(byte[] bytes, List<Target> ts, HasName hasName) {
        InputStream is = new ByteArrayInputStream(bytes);
        EntryStream res = new EntryStream(is, ts, hasName);
        res.setTransformed(true);
        return res;
    }

}
