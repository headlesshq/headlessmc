package io.github.headlesshq.headlessmc.runtime.commands;

import io.github.headlesshq.headlessmc.runtime.commands.reflection.*;
import lombok.val;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.HeadlessMcApi;
import io.github.headlesshq.headlessmc.api.command.CommandContextImpl;
import io.github.headlesshq.headlessmc.api.command.ParseUtil;
import io.github.headlesshq.headlessmc.api.command.PasswordCommand;
import io.github.headlesshq.headlessmc.api.command.impl.HelpCommand;
import io.github.headlesshq.headlessmc.api.command.MemoryCommand;
import io.github.headlesshq.headlessmc.api.command.impl.MultiCommand;
import io.github.headlesshq.headlessmc.runtime.RuntimeProperties;
import io.github.headlesshq.headlessmc.runtime.reflection.RuntimeReflection;
import io.github.headlesshq.headlessmc.runtime.reflection.VM;

public class RuntimeContext extends CommandContextImpl {
    public RuntimeContext(HeadlessMc ctx) {
        this(ctx, Thread.currentThread());
    }

    public RuntimeContext(HeadlessMc ctx, Thread thread) {
        super(ctx);
        initializeReflection(ctx, thread);
        add(new HelpCommand(ctx));
        add(new RuntimeQuitCommand(ctx));
        add(new MemoryCommand(ctx));
        add(new PasswordCommand(ctx));
        add(new MultiCommand(ctx));
    }

    protected void initializeReflection(HeadlessMc ctx, Thread thread) {
        if (ctx.getConfig().get(RuntimeProperties.ENABLE_REFLECTION, false)) {
            val vm = new VM(ctx.getConfig().get(RuntimeProperties.VM_SIZE, 128L).intValue());
            RuntimeReflection refContext = new RuntimeReflection(ctx, thread, vm);
            setAsInstance(refContext);
            add(new ClassCommand(refContext));
            add(new FieldCommand(refContext));
            add(new MethodCommand(refContext));
            add(new DumpCommand(refContext));
            add(TypeCommand.ofType(refContext, "string", s -> s));
            add(TypeCommand.ofType(refContext, "char", s -> s.charAt(0)));
            add(TypeCommand.ofType(refContext, "boolean", Boolean::parseBoolean));
            add(TypeCommand.ofType(refContext, "byte", ParseUtil::parseB));
            add(TypeCommand.ofType(refContext, "short", ParseUtil::parseS));
            add(TypeCommand.ofType(refContext, "int", ParseUtil::parseI));
            add(TypeCommand.ofType(refContext, "long", ParseUtil::parseL));
            add(TypeCommand.ofType(refContext, "float", ParseUtil::parseF));
            add(TypeCommand.ofType(refContext, "double", ParseUtil::parseD));
            add(new NewCommand(refContext));
            add(new PopCommand(refContext));
            add(new CopyCommand(refContext));
            add(new RunnableCommand(refContext));
            add(new FunctionCommand(refContext));
            add(new SupplierCommand(refContext));
            add(new IfCommand(refContext));
            add(new WhileCommand(refContext));
        }
    }

    protected void setAsInstance(RuntimeReflection reflection) {
        HeadlessMcApi.setInstance(reflection);
    }

}
