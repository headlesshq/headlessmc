package org.lwjgl.system;

public interface FunctionProvider {
    long getFunctionAddress(CharSequence name);
}
