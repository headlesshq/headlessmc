/***
 * This class was taken from
 * org.mutabilitydetector.asm.typehierarchy.TypeHierarchy
 * of Grundlefleck/ASM-NonClassloadingExtensions.
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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Type;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.objectweb.asm.Type.getType;

/**
 * Wraps a {@link Type} to provide equivalents of methods found on {@link Class}, e.g. isInterface, getSuperClass,
 * but without loading any instances of Class.
 * <br>
 * TypeHierarchy aims to provide an API that is equivalent to a subset of java.lang.Class. TypeHierarchy should provide
 * the same results as if the equivalent method is called on the relevant instances of Class.
 *
 * @see Class#isInterface()
 * @see Class#isArray()
 * @see Class#isAssignableFrom(Class)
 * @see Class#getInterfaces()
 * @see Class#getSuperclass()
 */
@Getter
@SuppressWarnings("ClassCanBeRecord")
final class TypeHierarchy {
    private static final List<Type> IMPLEMENTS_NO_INTERFACES = Collections.emptyList();
    private static final List<Type> IMPLICIT_ARRAY_INTERFACES = unmodifiableList(
        asList(getType(Cloneable.class), getType(Serializable.class))
    );

    public static final TypeHierarchy JAVA_LANG_OBJECT = new TypeHierarchy(
        Type.getType(Object.class),
        null,
        IMPLEMENTS_NO_INTERFACES,
        false
    );

    public static final TypeHierarchy BOOLEAN_HIERARCHY = typeHierarchyForPrimitiveType(Type.BOOLEAN_TYPE);
    public static final TypeHierarchy BYTE_HIERARCHY = typeHierarchyForPrimitiveType(Type.BYTE_TYPE);
    public static final TypeHierarchy CHAR_HIERARCHY = typeHierarchyForPrimitiveType(Type.CHAR_TYPE);
    public static final TypeHierarchy SHORT_HIERARCHY = typeHierarchyForPrimitiveType(Type.SHORT_TYPE);
    public static final TypeHierarchy INT_HIERARCHY = typeHierarchyForPrimitiveType(Type.INT_TYPE);
    public static final TypeHierarchy LONG_HIERARCHY = typeHierarchyForPrimitiveType(Type.LONG_TYPE);
    public static final TypeHierarchy FLOAT_HIERARCHY = typeHierarchyForPrimitiveType(Type.FLOAT_TYPE);
    public static final TypeHierarchy DOUBLE_HIERARCHY = typeHierarchyForPrimitiveType(Type.DOUBLE_TYPE);
    public static final TypeHierarchy VOID_HIERARCHY = typeHierarchyForPrimitiveType(Type.VOID_TYPE);

    public static TypeHierarchy hierarchyForArrayOfType(Type t) {
        return new TypeHierarchy(t, JAVA_LANG_OBJECT.type(), IMPLICIT_ARRAY_INTERFACES, false);
    }

    /**
     * Use constant values declared in this class as an alternative.
     *
     * @see TypeHierarchy#BOOLEAN_HIERARCHY
     * @see TypeHierarchy#BYTE_HIERARCHY
     * @see TypeHierarchy#CHAR_HIERARCHY
     * @see TypeHierarchy#SHORT_HIERARCHY
     * @see TypeHierarchy#INT_HIERARCHY
     * @see TypeHierarchy#LONG_HIERARCHY
     * @see TypeHierarchy#FLOAT_HIERARCHY
     * @see TypeHierarchy#DOUBLE_HIERARCHY
     * @see TypeHierarchy#VOID_HIERARCHY
     */
    private static TypeHierarchy typeHierarchyForPrimitiveType(Type primitiveType) {
        return new TypeHierarchy(primitiveType, null, IMPLEMENTS_NO_INTERFACES, false);
    }

    private final Type thisType;
    private final @Nullable Type superClass;
    private final List<Type> interfaces;
    private final boolean isInterface;

    public TypeHierarchy(
        Type thisType,
        @Nullable Type superClass,
        List<Type> interfaces,
        boolean isInterface
    ) {
        this.thisType = thisType;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.isInterface = isInterface;
    }

    public Type type() {
        return thisType;
    }

    public boolean representsType(Type t) {
        return t.equals(thisType);
    }

    /**
     * Equivalent to {@link Class#isArray()}
     *
     * @see Class#isArray()
     */
    public boolean isArray() {
        return thisType.getSort() == Type.ARRAY;
    }

    /**
     * Equivalent to {@link Class#isAssignableFrom(Class)}.
     * <br>
     * Uses the given {@link TypeHierarchyReader} to obtain information about any types that are needed to determine
     * if this type is assignable from the given type. This can include superclasses and interfaces.
     *
     * @see Class#isAssignableFrom(Class)
     */
    public boolean isAssignableFrom(Type type, TypeHierarchyReader reader) {
        return isAssignableFrom(reader.hierarchyOf(type), reader);
    }

    /**
     * Equivalent to {@link Class#isAssignableFrom(Class)}.
     * <br>
     * Uses the given {@link TypeHierarchyReader} to obtain information about any types that are needed to determine
     * if this type is assignable from the given type. This can include superclasses and interfaces.
     *
     * @see Class#isAssignableFrom(Class)
     */
    public boolean isAssignableFrom(TypeHierarchy u, TypeHierarchyReader typeHierarchyReader) {
        if (assigningToObject()) {
            return true;
        }

        if (this.isSameType(u)) {
            return true;
        } else if (this.isSuperTypeOf(u)) {
            return true;
        } else if (this.isInterfaceImplementedBy(u)) {
            return true;
        } else if (bothAreArrayTypes(u) && haveSameDimensionality(u)) {
            return (JAVA_LANG_OBJECT.representsType(typeOfArray()) && u.isReferenceArrayType())
                || arrayTypeIsAssignableFrom(u, typeHierarchyReader);
        } else if (bothAreArrayTypes(u)
            && isObjectArrayWithSmallerDimensionalityThan(u)) {
            return true;
        } else if (u.extendsObject() && !u.implementsAnyInterfaces()) {
            return false;
        }

        if (u.hasSuperClass()
            && u.getSuperClass() != null
            && isAssignableFrom(u.getSuperClass(), typeHierarchyReader)) {
            return true;
        }

        return u.implementsAnyInterfaces() && isAssignableFromAnyInterfaceImplementedBy(u, typeHierarchyReader);
    }

    private boolean assigningToObject() {
        return JAVA_LANG_OBJECT.representsType(type());
    }

    private boolean isAssignableFromAnyInterfaceImplementedBy(TypeHierarchy u, TypeHierarchyReader typeHierarchyReader) {
        for (Type ui : u.interfaces) {
            if (isAssignableFrom(ui, typeHierarchyReader)) {
                return true;
            }
        }

        return false;
    }

    private boolean haveSameDimensionality(TypeHierarchy u) {
        return arrayDimensionality() == u.arrayDimensionality();
    }

    private boolean isObjectArrayWithSmallerDimensionalityThan(TypeHierarchy u) {
        return JAVA_LANG_OBJECT.representsType(typeOfArray())
            && arrayDimensionality() <= u.arrayDimensionality();
    }

    private boolean arrayTypeIsAssignableFrom(
        TypeHierarchy u,
        TypeHierarchyReader reader
    ) {

        TypeHierarchy thisArrayType = reader.hierarchyOf(typeOfArray());
        return typeOfArray().getSort() == u.typeOfArray().getSort()
            && thisArrayType.isAssignableFrom(reader.hierarchyOf(u.typeOfArray()), reader);
    }

    private boolean bothAreArrayTypes(TypeHierarchy u) {
        return this.isArray() && u.isArray();
    }

    private Type typeOfArray() {
        return Type.getType(thisType.getInternalName()
                                .substring(thisType.getDimensions()));
    }

    private int arrayDimensionality() {
        return thisType.getDimensions();
    }

    public boolean isReferenceArrayType() {
        return isArray() && typeOfArray().getSort() == Type.OBJECT;
    }

    public boolean isInterfaceImplementedBy(TypeHierarchy u) {
        return u.interfaces.contains(type());
    }

    public boolean isSuperTypeOf(TypeHierarchy u) {
        return type().equals(u.getSuperClass());
    }

    public boolean hasSuperClass() {
        return getSuperClass() != null && !JAVA_LANG_OBJECT.representsType(getSuperClass());
    }

    public boolean implementsAnyInterfaces() {
        return !interfaces.isEmpty();
    }

    public boolean extendsObject() {
        return getSuperClass() != null
            && JAVA_LANG_OBJECT.representsType(getSuperClass());
    }

    public boolean isSameType(TypeHierarchy u) {
        return u.type().equals(type());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        return prime * thisType.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }

        TypeHierarchy other = (TypeHierarchy) obj;
        return thisType.equals(other.thisType);
    }

    @Override
    public String toString() {
        return String.format(
            "%s [type=%s]",
            getClass().getSimpleName(),
            thisType
        );
    }

}
