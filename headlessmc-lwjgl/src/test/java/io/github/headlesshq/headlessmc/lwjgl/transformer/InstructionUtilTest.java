package io.github.headlesshq.headlessmc.lwjgl.transformer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

public class InstructionUtilTest extends AbstractUtilityTest<InstructionUtil> {

    @Test
    @DisplayName("Test for loadType() with primitive types")
    public void testLoadTypeWithPrimitiveTypes() {
        Type intType = Type.INT_TYPE;
        AbstractInsnNode insnNode = InstructionUtil.loadType(intType);
        assertEquals(GETSTATIC, insnNode.getOpcode());
    }

    @Test
    @DisplayName("Test for loadType() with non-primitive types")
    public void testLoadTypeWithNonPrimitiveTypes() {
        Type stringType = Type.getType(String.class);
        AbstractInsnNode insnNode = InstructionUtil.loadType(stringType);
        assertEquals(LDC, insnNode.getOpcode());
    }

    @Test
    @DisplayName("Test for box() with primitive types")
    public void testBoxWithPrimitiveTypes() {
        Type intType = Type.INT_TYPE;
        MethodInsnNode methodInsnNode = InstructionUtil.box(intType);
        assertNotNull(methodInsnNode);
        assertEquals(INVOKESTATIC, methodInsnNode.getOpcode());
    }

    @Test
    @DisplayName("Test for box() with non-primitive types")
    public void testBoxWithNonPrimitiveTypes() {
        Type stringType = Type.getType(String.class);
        MethodInsnNode methodInsnNode = InstructionUtil.box(stringType);
        assertNull(methodInsnNode);
    }

    @Test
    @DisplayName("Test for getWrapper() with primitive types")
    public void testGetWrapperWithPrimitiveTypes() {
        Type intType = Type.INT_TYPE;
        Type wrapperType = InstructionUtil.getWrapper(intType);
        assertEquals("Ljava/lang/Integer;", wrapperType.getDescriptor());
    }

    @Test
    @DisplayName("Test for getWrapper() with non-primitive types")
    public void testGetWrapperWithNonPrimitiveTypes() {
        Type stringType = Type.getType(String.class);
        Type wrapperType = InstructionUtil.getWrapper(stringType);
        assertEquals(stringType, wrapperType);
    }

    @Test
    @DisplayName("Test for isPrimitive() with primitive types")
    public void testIsPrimitiveWithPrimitiveTypes() {
        Type intType = Type.INT_TYPE;
        assertTrue(InstructionUtil.isPrimitive(intType));
    }

    @Test
    @DisplayName("Test for isPrimitive() with non-primitive types")
    public void testIsPrimitiveWithNonPrimitiveTypes() {
        Type stringType = Type.getType(String.class);
        assertFalse(InstructionUtil.isPrimitive(stringType));
    }

    @Test
    @DisplayName("Test for unbox() with primitive types")
    public void testUnboxWithPrimitiveTypes() {
        Type intType = Type.INT_TYPE;
        InsnList insnList = InstructionUtil.unbox(intType);
        assertNotNull(insnList);
        assertNotEquals(0, insnList.size());
    }

    @Test
    @DisplayName("Test for loadParam() with different types")
    public void testLoadParam() {
        Type intType = Type.INT_TYPE;
        VarInsnNode varInsnNode = InstructionUtil.loadParam(intType, 1);
        assertEquals(ILOAD, varInsnNode.getOpcode());

        Type objType = Type.getType(Object.class);
        varInsnNode = InstructionUtil.loadParam(objType, 1);
        assertEquals(ALOAD, varInsnNode.getOpcode());
    }

    @Test
    @DisplayName("Test for makeReturn() with different types")
    public void testMakeReturn() {
        Type intType = Type.INT_TYPE;
        InsnNode insnNode = InstructionUtil.makeReturn(intType);
        assertEquals(IRETURN, insnNode.getOpcode());

        Type voidType = Type.VOID_TYPE;
        insnNode = InstructionUtil.makeReturn(voidType);
        assertEquals(RETURN, insnNode.getOpcode());

        Type objType = Type.getType(Object.class);
        insnNode = InstructionUtil.makeReturn(objType);
        assertEquals(ARETURN, insnNode.getOpcode());
    }

}
