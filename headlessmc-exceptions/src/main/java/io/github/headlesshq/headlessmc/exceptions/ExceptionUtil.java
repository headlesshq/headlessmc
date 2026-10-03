package io.github.headlesshq.headlessmc.exceptions;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ExceptionUtil {
    public Throwable handleInterruptions(Throwable t) {
        if (t instanceof UncheckedInterruptedException) {
            throw (UncheckedInterruptedException) t;
        } else if (t instanceof InterruptedException) {
            throw new UncheckedInterruptedException(t);
        } else if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        } else {
            return t;
        }
    }

}
