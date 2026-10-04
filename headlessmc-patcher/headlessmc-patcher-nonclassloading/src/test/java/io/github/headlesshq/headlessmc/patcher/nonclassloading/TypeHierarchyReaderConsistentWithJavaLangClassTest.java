/***
 * This class was taken from
 * org.mutabilitydetector.asm.TypeHierarchyReaderConsistentWithJavaLangClassTest
 * of Grundlefleck/ASM-NonClassloadingExtensions,
 * and updated to a newer version of Junit.
 * (https://github.com/Grundlefleck/ASM-NonClassloadingExtensions).
 * Which is licensed with the Apache License 2.0:
 * <p>
 *
 *                                 Apache License
 *                           Version 2.0, January 2004
 *                        http://www.apache.org/licenses/
 * <p>
 *   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION
 * <p>
 *   1. Definitions.
 * <p>
 *      "License" shall mean the terms and conditions for use, reproduction,
 *      and distribution as defined by Sections 1 through 9 of this document.
 * <p>
 *      "Licensor" shall mean the copyright owner or entity authorized by
 *      the copyright owner that is granting the License.
 * <p>
 *      "Legal Entity" shall mean the union of the acting entity and all
 *      other entities that control, are controlled by, or are under common
 *      control with that entity. For the purposes of this definition,
 *      "control" means (i) the power, direct or indirect, to cause the
 *      direction or management of such entity, whether by contract or
 *      otherwise, or (ii) ownership of fifty percent (50%) or more of the
 *      outstanding shares, or (iii) beneficial ownership of such entity.
 * <p>
 *      "You" (or "Your") shall mean an individual or Legal Entity
 *      exercising permissions granted by this License.
 * <p>
 *      "Source" form shall mean the preferred form for making modifications,
 *      including but not limited to software source code, documentation
 *      source, and configuration files.
 * <p>
 *      "Object" form shall mean any form resulting from mechanical
 *      transformation or translation of a Source form, including but
 *      not limited to compiled object code, generated documentation,
 *      and conversions to other media types.
 * <p>
 *      "Work" shall mean the work of authorship, whether in Source or
 *      Object form, made available under the License, as indicated by a
 *      copyright notice that is included in or attached to the work
 *      (an example is provided in the Appendix below).
 * <p>
 *      "Derivative Works" shall mean any work, whether in Source or Object
 *      form, that is based on (or derived from) the Work and for which the
 *      editorial revisions, annotations, elaborations, or other modifications
 *      represent, as a whole, an original work of authorship. For the purposes
 *      of this License, Derivative Works shall not include works that remain
 *      separable from, or merely link (or bind by name) to the interfaces of,
 *      the Work and Derivative Works thereof.
 * <p>
 *      "Contribution" shall mean any work of authorship, including
 *      the original version of the Work and any modifications or additions
 *      to that Work or Derivative Works thereof, that is intentionally
 *      submitted to Licensor for inclusion in the Work by the copyright owner
 *      or by an individual or Legal Entity authorized to submit on behalf of
 *      the copyright owner. For the purposes of this definition, "submitted"
 *      means any form of electronic, verbal, or written communication sent
 *      to the Licensor or its representatives, including but not limited to
 *      communication on electronic mailing lists, source code control systems,
 *      and issue tracking systems that are managed by, or on behalf of, the
 *      Licensor for the purpose of discussing and improving the Work, but
 *      excluding communication that is conspicuously marked or otherwise
 *      designated in writing by the copyright owner as "Not a Contribution."
 * <p>
 *      "Contributor" shall mean Licensor and any individual or Legal Entity
 *      on behalf of whom a Contribution has been received by Licensor and
 *      subsequently incorporated within the Work.
 * <p>
 *   2. Grant of Copyright License. Subject to the terms and conditions of
 *      this License, each Contributor hereby grants to You a perpetual,
 *      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
 *      copyright license to reproduce, prepare Derivative Works of,
 *      publicly display, publicly perform, sublicense, and distribute the
 *      Work and such Derivative Works in Source or Object form.
 * <p>
 *   3. Grant of Patent License. Subject to the terms and conditions of
 *      this License, each Contributor hereby grants to You a perpetual,
 *      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
 *      (except as stated in this section) patent license to make, have made,
 *      use, offer to sell, sell, import, and otherwise transfer the Work,
 *      where such license applies only to those patent claims licensable
 *      by such Contributor that are necessarily infringed by their
 *      Contribution(s) alone or by combination of their Contribution(s)
 *      with the Work to which such Contribution(s) was submitted. If You
 *      institute patent litigation against any entity (including a
 *      cross-claim or counterclaim in a lawsuit) alleging that the Work
 *      or a Contribution incorporated within the Work constitutes direct
 *      or contributory patent infringement, then any patent licenses
 *      granted to You under this License for that Work shall terminate
 *      as of the date such litigation is filed.
 * <p>
 *   4. Redistribution. You may reproduce and distribute copies of the
 *      Work or Derivative Works thereof in any medium, with or without
 *      modifications, and in Source or Object form, provided that You
 *      meet the following conditions:
 * <p>
 *      (a) You must give any other recipients of the Work or
 *          Derivative Works a copy of this License; and
 * <p>
 *      (b) You must cause any modified files to carry prominent notices
 *          stating that You changed the files; and
 * <p>
 *      (c) You must retain, in the Source form of any Derivative Works
 *          that You distribute, all copyright, patent, trademark, and
 *          attribution notices from the Source form of the Work,
 *          excluding those notices that do not pertain to any part of
 *          the Derivative Works; and
 * <p>
 *      (d) If the Work includes a "NOTICE" text file as part of its
 *          distribution, then any Derivative Works that You distribute must
 *          include a readable copy of the attribution notices contained
 *          within such NOTICE file, excluding those notices that do not
 *          pertain to any part of the Derivative Works, in at least one
 *          of the following places: within a NOTICE text file distributed
 *          as part of the Derivative Works; within the Source form or
 *          documentation, if provided along with the Derivative Works; or,
 *          within a display generated by the Derivative Works, if and
 *          wherever such third-party notices normally appear. The contents
 *          of the NOTICE file are for informational purposes only and
 *          do not modify the License. You may add Your own attribution
 *          notices within Derivative Works that You distribute, alongside
 *          or as an addendum to the NOTICE text from the Work, provided
 *          that such additional attribution notices cannot be construed
 *          as modifying the License.
 * <p>
 *      You may add Your own copyright statement to Your modifications and
 *      may provide additional or different license terms and conditions
 *      for use, reproduction, or distribution of Your modifications, or
 *      for any such Derivative Works as a whole, provided Your use,
 *      reproduction, and distribution of the Work otherwise complies with
 *      the conditions stated in this License.
 * <p>
 *   5. Submission of Contributions. Unless You explicitly state otherwise,
 *      any Contribution intentionally submitted for inclusion in the Work
 *      by You to the Licensor shall be under the terms and conditions of
 *      this License, without any additional terms or conditions.
 *      Notwithstanding the above, nothing herein shall supersede or modify
 *      the terms of any separate license agreement you may have executed
 *      with Licensor regarding such Contributions.
 * <p>
 *   6. Trademarks. This License does not grant permission to use the trade
 *      names, trademarks, service marks, or product names of the Licensor,
 *      except as required for reasonable and customary use in describing the
 *      origin of the Work and reproducing the content of the NOTICE file.
 * <p>
 *   7. Disclaimer of Warranty. Unless required by applicable law or
 *      agreed to in writing, Licensor provides the Work (and each
 *      Contributor provides its Contributions) on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *      implied, including, without limitation, any warranties or conditions
 *      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
 *      PARTICULAR PURPOSE. You are solely responsible for determining the
 *      appropriateness of using or redistributing the Work and assume any
 *      risks associated with Your exercise of permissions under this License.
 * <p>
 *   8. Limitation of Liability. In no event and under no legal theory,
 *      whether in tort (including negligence), contract, or otherwise,
 *      unless required by applicable law (such as deliberate and grossly
 *      negligent acts) or agreed to in writing, shall any Contributor be
 *      liable to You for damages, including any direct, indirect, special,
 *      incidental, or consequential damages of any character arising as a
 *      result of this License or out of the use or inability to use the
 *      Work (including but not limited to damages for loss of goodwill,
 *      work stoppage, computer failure or malfunction, or any and all
 *      other commercial damages or losses), even if such Contributor
 *      has been advised of the possibility of such damages.
 * <p>
 *   9. Accepting Warranty or Additional Liability. While redistributing
 *      the Work or Derivative Works thereof, You may choose to offer,
 *      and charge a fee for, acceptance of support, warranty, indemnity,
 *      or other liability obligations and/or rights consistent with this
 *      License. However, in accepting such obligations, You may act only
 *      on Your own behalf and on Your sole responsibility, not on behalf
 *      of any other Contributor, and only if You agree to indemnify,
 *      defend, and hold each Contributor harmless for any liability
 *      incurred by, or claims asserted against, such Contributor by reason
 *      of your accepting any such warranty or additional liability.
 * <p>
 *   END OF TERMS AND CONDITIONS
 */
package io.github.headlesshq.headlessmc.patcher.nonclassloading;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

public class TypeHierarchyReaderConsistentWithJavaLangClassTest {
    private final TypeHierarchyReader typeHierarchyReader = new TypeHierarchyReader(type -> {
        try {
            return new ClassReader(type.getInternalName());
        } catch (IOException e) {
            throw new HeadlessMcIOException(e);
        }
    });

    @Test
    public void testSuperClassOfConcreteClassExtendingObjectImplicitlyIsTypeRepresentingJavaLangObject() {
        assertSuperClass(Object.class, UnrelatedType.class);
    }

    @Test
    public void testSuperClassOfSubclass() {
        assertSuperClass(Superclass.class, Subclass.class);
    }

    @Test
    public void testSuperClassOfInterfaceWithNoSuperInterfaceIsObject() {
        assertSuperClass(null, Interface.class);
    }

    @Test
    public void testSuperClassOfSubInterfaceIsJavaLangObject() {
        assertSuperClass(null, SubInterface.class);
    }

    @Test
    public void testSuperclassOfArrayClassHasSameSemanticsAsJavaLangClass_getSuperClass() throws Exception {
        assertSuperClass(Object.class, Object[].class);
        assertSuperClass(Object.class, Interface[].class);
        assertSuperClass(Object.class, Superclass[].class);
    }

    @Test
    public void testClassIsAssignableFromItself() {
        assertIsAssignableFrom(AssignableFromItself.class, AssignableFromItself.class);
    }

    @Test
    public void testClassIsNotAssignableToUnrelatedType() {
        assertIsNotAssignableFrom(AssignableFromItself.class, UnrelatedType.class);
        assertIsNotAssignableFrom(UnrelatedType.class, AssignableFromItself.class);
    }

    @Test
    public void testSuperclassIsAssignableFromSubclass() throws Exception {
        assertIsAssignableFrom(Superclass.class, Subclass.class);
    }

    @Test
    public void testIndirectSubclassIsAssignableToSuperclass() throws Exception {
        assertIsAssignableFrom(Superclass.class, SubSubclass.class);
    }

    @Test
    public void testSubclassIsNotAssignableToOtherClassWithSameSuperclass() throws Exception {
        assertIsNotAssignableFrom(Subclass.class, OtherSubclass.class);
    }

    @Test
    public void testSubclassIsNotAssignableFromSuperclass() throws Exception {
        assertIsNotAssignableFrom(Subclass.class, Superclass.class);
    }

    @Test
    public void testInterfaceIsAssignableFromImplementingClass() throws Exception {
        assertIsAssignableFrom(Interface.class, ImplementsInterface.class);
    }

    @Test
    public void testInterfaceIsAssignableFromSubclassOfImplementingClass() throws Exception {
        assertIsAssignableFrom(Interface.class, ExtendsImplementsInterface.class);
        assertIsNotAssignableFrom(ExtendsImplementsInterface.class, Interface.class);
    }

    @Test
    public void testSuperInterfaceIsAssignableFromSubInterface() throws Exception {
        assertIsAssignableFrom(SuperInterface.class, SubInterface.class);
        assertIsNotAssignableFrom(SubInterface.class, SuperInterface.class);
    }

    @Test
    public void testImplementingClassIsNotAssignableFromInterface() throws Exception {
        assertIsNotAssignableFrom(ImplementsInterface.class, Interface.class);
    }

    @Test
    public void testObjectIsAssignableFromAnything() throws Exception {
        assertIsAssignableFrom(Object.class, Superclass.class);
        assertIsAssignableFrom(Object.class, Subclass.class);
        assertIsAssignableFrom(Object.class, Interface.class);
        assertIsAssignableFrom(Object.class, SubInterface.class);
    }

    @Test
    public void testAllImplementedInterfacesAreAssignableFromImplementingClass() throws Exception {
        assertIsAssignableFrom(Interface.class, ImplementsSeveralInterfaces.class);
        assertIsAssignableFrom(SubInterface.class, ImplementsSeveralInterfaces.class);
        assertIsAssignableFrom(SuperInterface.class, ImplementsSeveralInterfaces.class);
        assertIsNotAssignableFrom(OtherImplementsInterface.class, ImplementsSeveralInterfaces.class);
    }

    @Test
    public void testInterfaceIsAssignableFromClassWithSuperclassOutwithInterfaceHierarchy() throws Exception {
        assertIsAssignableFrom(SuperInterface.class, ExtendsClassOutwithInterfaceHierarchy.class);
    }

    @Test
    public void testArrayTypeAssignment() throws Exception {
        assertIsAssignableFrom(Object.class, Interface[].class);
        assertIsAssignableFrom(Cloneable.class, Interface[].class);
        assertIsAssignableFrom(Serializable.class, Interface[].class);
        assertIsAssignableFrom(Object[].class, Interface[].class);
        assertIsAssignableFrom(Object[].class, Interface[].class);
        assertIsAssignableFrom(Interface[].class, Interface[].class);
        assertIsAssignableFrom(Interface[].class, ImplementsInterface[].class);
        assertIsNotAssignableFrom(ImplementsInterface[].class, Interface[].class);
        assertIsAssignableFrom(Interface[].class, ExtendsImplementsInterface[].class);
        assertIsAssignableFrom(Object[].class, Superclass[].class);
        assertIsAssignableFrom(Object[].class, Subclass[].class);
        assertIsAssignableFrom(Superclass[].class, Subclass[].class);
        assertIsNotAssignableFrom(Subclass[].class, Superclass[].class);
    }

    @Test
    public void testArrayDimensionAssignment() throws Exception {
        assertIsAssignableFrom(Object.class, Object[].class);
        assertIsNotAssignableFrom(Object[].class, Object.class);
        assertIsAssignableFrom(Object.class, Interface[].class);
        assertIsAssignableFrom(Object[].class, Interface[][].class);
        assertIsAssignableFrom(Object[][].class, Interface[][].class);
        assertIsNotAssignableFrom(Interface.class, Interface[].class);
        assertIsNotAssignableFrom(Interface[].class, Interface.class);
        assertIsNotAssignableFrom(Interface[].class, Interface[][].class);
        assertIsNotAssignableFrom(Interface[][].class, Interface[].class);
    }

    @Test
    public void testAnonymousInnerClasses() throws Exception {
        assertIsAssignableFrom(Interface.class, new Interface() {}.getClass());
        assertIsNotAssignableFrom(new Interface() {}.getClass(), Interface.class);
    }

    @Test
    public void testAssignmentOfPrimitiveArrayTypes() throws Exception {
        assertIsAssignableFrom(boolean[].class, boolean[].class);
        assertIsAssignableFrom(byte[].class, byte[].class);
        assertIsAssignableFrom(char[].class, char[].class);
        assertIsAssignableFrom(short[].class, short[].class);
        assertIsAssignableFrom(int[].class, int[].class);
        assertIsAssignableFrom(long[].class, long[].class);
        assertIsAssignableFrom(float[].class, float[].class);
        assertIsAssignableFrom(double[].class, double[].class);
    }

    @Test
    public void testAssignmentOfPrimitiveArrayTypesToObjectType() throws Exception {
        assertIsNotAssignableFrom(Object[].class, boolean[].class);
        assertIsNotAssignableFrom(Object[].class, byte[].class);
        assertIsNotAssignableFrom(Object[].class, char[].class);
        assertIsNotAssignableFrom(Object[].class, short[].class);
        assertIsNotAssignableFrom(Object[].class, int[].class);
        assertIsNotAssignableFrom(Object[].class, long[].class);
        assertIsNotAssignableFrom(Object[].class, float[].class);
        assertIsNotAssignableFrom(Object[].class, double[].class);
    }

    @Test
    public void testAssignmentOfObjectArrayToPrimitiveArrayTypes() throws Exception {
        assertIsNotAssignableFrom(boolean[].class, Object[].class);
        assertIsNotAssignableFrom(byte[].class, Object[].class);
        assertIsNotAssignableFrom(char[].class, Object[].class);
        assertIsNotAssignableFrom(short[].class, Object[].class);
        assertIsNotAssignableFrom(int[].class, Object[].class);
        assertIsNotAssignableFrom(long[].class, Object[].class);
        assertIsNotAssignableFrom(float[].class, Object[].class);
        assertIsNotAssignableFrom(double[].class, Object[].class);
    }

    private void assertSuperClass(Class<?> expectedSuperClass, Class<?> subclass) {
        assertEquals(
            expectedSuperClass, subclass.getSuperclass(),
            "Assertion is not consistent with Class.getSuperclass"
        );

        Type superclassType = expectedSuperClass == null ? null : Type.getType(expectedSuperClass);
        Type subclassType = Type.getType(subclass);
        Type actualSuperclassType = typeHierarchyReader.getSuperClass(subclassType);
        assertEquals(
            actualSuperclassType,
            superclassType,
            "Type Hierarchy visitor is not consistent with Class.getSuperclass"
        );
    }

    private void assertIsAssignableFrom(Class<?> to, Class<?> from) {
        assertTrue(to.isAssignableFrom(from), "Assertion is not consistent with Class.isAssignableFrom");
        Type toType = Type.getType(to);
        Type fromType = Type.getType(from);
        assertTrue(
            typeHierarchyReader.isAssignableFrom(toType, fromType),
            "Type Hierarchy visitor is not consistent with Class.isAssignableFrom"
        );
    }

    private void assertIsNotAssignableFrom(Class<?> to, Class<?> from) {
        assertFalse(to.isAssignableFrom(from), "Assertion is not consistent with Class.isAssignableFrom");
        Type toType = Type.getType(to);
        Type fromType = Type.getType(from);
        assertFalse(
            typeHierarchyReader.isAssignableFrom(toType, fromType),
            "Type Hierarchy visitor is not consistent with Class.isAssignableFrom"
        );
    }


    static class AssignableFromItself {}

    static class UnrelatedType {}

    static class Superclass {}

    static class Subclass extends Superclass {}

    static class OtherSubclass extends Superclass {}

    static class SubSubclass extends Subclass {}

    interface Interface {}

    static class ImplementsInterface implements Interface {}

    static class ExtendsImplementsInterface extends ImplementsInterface {}

    interface SuperInterface {}

    interface SubInterface extends SuperInterface {}

    static class ImplementsSeveralInterfaces implements Interface, SubInterface {}

    static class OtherImplementsInterface implements Interface {}

    static class ExtendsClassOutwithInterfaceHierarchy extends UnrelatedType implements SubInterface {}

}
