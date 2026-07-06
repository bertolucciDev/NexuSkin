package NexuSkin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

final class SkinPatcher {

    static byte[] patchPlayer(byte[] original) {
        try {
            ClassNode cn = new ClassNode();
            new ClassReader(original).accept(cn, 0);

            for (MethodNode m : cn.methods) {
                if (matches(m.name, "getLocationSkin", "func_110306_f")) {
                    InsnList list = new InsnList();
                    list.add(new LdcInsnNode("nexuskin"));
                    list.add(new LdcInsnNode("skins/" + NxAgent.getUser().toLowerCase() + "_skin"));
                    list.add(new MethodInsnNode(
                            org.objectweb.asm.Opcodes.INVOKESTATIC,
                            "NexuSkin/TextureLocator",
                            "locate",
                            "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/util/ResourceLocation;",
                            false));
                    list.add(new InsnNode(org.objectweb.asm.Opcodes.ARETURN));
                    m.instructions = list;
                }
                if (matches(m.name, "getLocationCape", "func_110307_g")) {
                    InsnList list = new InsnList();
                    list.add(new LdcInsnNode("nexuskin"));
                    list.add(new LdcInsnNode("capes/" + NxAgent.getUser().toLowerCase() + "_cape"));
                    list.add(new MethodInsnNode(
                            org.objectweb.asm.Opcodes.INVOKESTATIC,
                            "NexuSkin/TextureLocator",
                            "locateCape",
                            "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/util/ResourceLocation;",
                            false));
                    list.add(new InsnNode(org.objectweb.asm.Opcodes.ARETURN));
                    m.instructions = list;
                }
            }
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    static byte[] patchNetworkPlayerInfo(byte[] original) {
        try {
            ClassNode cn = new ClassNode();
            new ClassReader(original).accept(cn, 0);
            for (MethodNode m : cn.methods) {
                if (matches(m.name, "getLocationSkin", "func_178837_e")) {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(org.objectweb.asm.Opcodes.ALOAD, 0));
                    list.add(new MethodInsnNode(
                            org.objectweb.asm.Opcodes.INVOKESTATIC,
                            "NexuSkin/NetworkSkinHook",
                            "resolveForNetwork",
                            "(Ljava/lang/Object;)Lnet/minecraft/util/ResourceLocation;",
                            false));
                    list.add(new InsnNode(org.objectweb.asm.Opcodes.ARETURN));
                    m.instructions = list;
                }
                if (matches(m.name, "getLocationCape", "func_178838_f")) {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(org.objectweb.asm.Opcodes.ALOAD, 0));
                    list.add(new MethodInsnNode(
                            org.objectweb.asm.Opcodes.INVOKESTATIC,
                            "NexuSkin/NetworkSkinHook",
                            "resolveCapeForNetwork",
                            "(Ljava/lang/Object;)Lnet/minecraft/util/ResourceLocation;",
                            false));
                    list.add(new InsnNode(org.objectweb.asm.Opcodes.ARETURN));
                    m.instructions = list;
                }
            }
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean matches(String actual, String... candidates) {
        for (String c : candidates) if (c.equals(actual)) return true;
        return false;
    }
}
