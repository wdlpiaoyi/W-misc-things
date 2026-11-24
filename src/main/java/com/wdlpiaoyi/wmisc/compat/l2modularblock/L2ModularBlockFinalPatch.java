package com.wdlpiaoyi.wmisc.compat.l2modularblock;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public final class L2ModularBlockFinalPatch {
    public static final String PACKAGE_PREFIX = "dev.xkmc.l2modularblock.";
    private static final String INTERNAL_PREFIX = "dev.xkmc.l2modularblock.".replace('.', '/');

    private L2ModularBlockFinalPatch() {
    }

    public static boolean handles(String className) {
        return className != null && className.startsWith("dev.xkmc.l2modularblock.");
    }

    public static boolean handlesInternal(String internalName) {
        return internalName != null && internalName.startsWith(INTERNAL_PREFIX);
    }

    public static List<String> stripFinal(ClassNode classNode) {
        List<String> changed = new ArrayList();
        if (classNode != null && classNode.methods != null) {
            for(MethodNode method : classNode.methods) {
                if (!method.name.startsWith("<") && (method.access & 16) != 0 && (method.access & 10) == 0) {
                    method.access &= -17;
                    changed.add(method.name + method.desc);
                }
            }

            return changed;
        } else {
            return changed;
        }
    }
}
