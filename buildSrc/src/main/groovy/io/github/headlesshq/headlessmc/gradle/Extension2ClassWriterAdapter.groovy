package io.github.headlesshq.headlessmc.gradle

import org.objectweb.asm.ClassWriter

import static ModuleExtension.ACC_MANDATED
import static org.objectweb.asm.Opcodes.ACC_MODULE
import static org.objectweb.asm.Opcodes.V9

class Extension2ClassWriterAdapter {
    static ClassWriter toClassWriter(ModuleExtension me) {
        def cw = new ClassWriter(0)
        cw.visit(V9, ACC_MODULE, "module-info", null, null, null)
        def mw = cw.visitModule(
                me.name.get(), me.access.getOrElse(0), me.version.orNull)

        if (me.mainClass.present) {
            mw.visitMainClass(me.mainClass.get())
        }

        for (ExportsOrOpens export : me.exports) {
            mw.visitExport(export.pkg, export.access, export.to)
        }

        for (ExportsOrOpens opens : me.opens) {
            mw.visitOpen(opens.pkg, opens.access, opens.to)
        }

        if (me.requireJavaBase.getOrElse(true)) {
            // TODO: version?
            mw.visitRequire('java.base', ACC_MANDATED, null)
        }

        for (Requires requires : me.requires) {
            mw.visitRequire(requires.pkg, requires.access, requires.version)
        }

        for (Provides provides : me.provides) {
            mw.visitProvide(provides.service, provides.with)
        }

        for (String use : me.uses) {
            mw.visitUse(use)
        }

        for (String pack : me.packages) {
            mw.visitPackage(pack)
        }

        mw.visitEnd()
        cw.visitEnd()
        return cw
    }

}
