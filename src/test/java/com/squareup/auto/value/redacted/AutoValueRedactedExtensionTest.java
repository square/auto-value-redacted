/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.auto.value.redacted;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.testing.compile.JavaFileObjects;
import java.util.Arrays;
import javax.tools.JavaFileObject;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public final class AutoValueRedactedExtensionTest {
  private JavaFileObject redacted;
  private JavaFileObject nullable;

  @Before public void setUp() {
    redacted = JavaFileObjects.forSourceString("test.Redacted", ""
        + "package test;\n"
        + "import java.lang.annotation.Retention;\n"
        + "import java.lang.annotation.Target;\n"
        + "import static java.lang.annotation.ElementType.FIELD;\n"
        + "import static java.lang.annotation.ElementType.METHOD;\n"
        + "import static java.lang.annotation.ElementType.PARAMETER;\n"
        + "import static java.lang.annotation.RetentionPolicy.SOURCE;\n"
        + "@Retention(SOURCE)\n"
        + "@Target({METHOD, PARAMETER, FIELD})\n"
        + "public @interface Redacted {\n"
        + "}");
    nullable = JavaFileObjects.forSourceString("test.Nullable", ""
        + "package test;\n"
        + "import java.lang.annotation.Retention;\n"
        + "import java.lang.annotation.Target;\n"
        + "import static java.lang.annotation.ElementType.FIELD;\n"
        + "import static java.lang.annotation.ElementType.METHOD;\n"
        + "import static java.lang.annotation.ElementType.PARAMETER;\n"
        + "import static java.lang.annotation.RetentionPolicy.CLASS;\n"
        + "@Retention(CLASS)\n"
        + "@Target({METHOD, PARAMETER, FIELD})\n"
        + "public @interface Nullable {\n"
        + "}");
  }

  @Test public void simple() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "@AutoValue public abstract class Test {\n"
            // Reference type
            + "@Redacted public abstract String a();\n"
            + "@Redacted @Nullable public abstract String b();\n"
            + "@Nullable public abstract String c();\n"
            + "public abstract String d();\n"
            // Array type
            + "@Redacted public abstract int[] e();\n"
            + "@Redacted @Nullable public abstract int[] f();\n"
            + "@Nullable public abstract int[] g();\n"
            + "public abstract int[] h();\n"
            // Primitive type
            + "@Redacted public abstract int i();\n"
            + "public abstract int j();\n"
            + "}\n"
    );

    JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
            + "package test;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.String;\n"
            + "import java.util.Arrays;\n"
            + "final class AutoValue_Test extends $AutoValue_Test {\n"
            + "  AutoValue_Test(\n"
            + "      String a, String b, String c, String d,\n"
            + "      int[] e, int[] f, int[] g, int[] h,\n"
            + "      int i, int j) {\n"
            + "    super(a, b, c, d, e, f, g, h, i, j);\n"
            + "  }\n"
            + "  @Override public final String toString() {\n"
            + "    return \"Test{\"\n"
            // Reference type
            + "        + \"a=██, \"\n"
            + "        + \"b=\" + (b() != null ? \"██\" : null) + \", \"\n"
            + "        + \"c=\" + (c() != null ? c() : null) + \", \"\n"
            + "        + \"d=\" + d() + \", \"\n"
            // Array type
            + "        + \"e=██, \"\n"
            + "        + \"f=\" + (f() != null ? \"██\" : null) + \", \"\n"
            + "        + \"g=\" + (g() != null ? Arrays.toString(g()) : null) + \", \"\n"
            + "        + \"h=\" + Arrays.toString(h()) + \", \"\n"
            // Primitive type
            + "        + \"i=██, \"\n"
            + "        + \"j=\" + j()\n"
            + "        + '}';\n"
            + "  }\n"
            + "}\n"
    );

    assertAbout(javaSources())
        .that(Arrays.asList(redacted, nullable, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedSource);
  }

  @Test public void beanPrefix() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Test {\n"
        + "@Redacted public abstract String getOne();\n"
        + "public abstract String getTwo();\n"
        + "@Redacted @Nullable public abstract String getThree();\n"
        + "@Nullable public abstract String getFour();\n"
        + "}\n"
    );

    JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
        + "package test;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "final class AutoValue_Test extends $AutoValue_Test {\n"
        + "  AutoValue_Test(String one, String two, String three, String four) {\n"
        + "    super(one, two, three, four);\n"
        + "  }\n"
        + "  @Override public final String toString() {\n"
        + "    return \"Test{\"\n"
        + "        + \"one=██, \"\n"
        + "        + \"two=\" + getTwo() + \", \"\n"
        + "        + \"three=\" + (getThree() != null ? \"██\" : null) + \", \"\n"
        + "        + \"four=\" + (getFour() != null ? getFour() : null)\n"
        + "        + '}';\n"
        + "  }\n"
        + "}\n"
    );

    assertAbout(javaSources())
        .that(Arrays.asList(redacted, nullable, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedSource);
  }

  @Test public void preConcatStrings() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Test {\n"
        + "@Redacted public abstract String one();\n"
        + "@Redacted public abstract String two();\n"
        + "}\n"
    );

    JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
        + "package test;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "final class AutoValue_Test extends $AutoValue_Test {\n"
        + "  AutoValue_Test(String one, String two) {\n"
        + "    super(one, two);\n"
        + "  }\n"
        + "  @Override public final String toString() {\n"
        + "    return \"Test{\"\n"
        + "        + \"one=██, \"\n"
        + "        + \"two=██\"\n"
        + "        + '}';\n"
        + "  }\n"
        + "}\n"
    );

    assertAbout(javaSources())
        .that(Arrays.asList(redacted, nullable, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedSource);
  }

  @Test public void generics() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Test<T extends String & Runnable, U extends T> {\n"
        + "@Redacted public abstract T tee();\n"
        + "public abstract U you();\n"
        + "}\n"
    );

    JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
        + "package test;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.Runnable;\n"
        + "import java.lang.String;\n"
        + "final class AutoValue_Test<T extends String & Runnable, U extends T> extends $AutoValue_Test<T, U> {\n"
        + "  AutoValue_Test(T tee, U you) {\n"
        + "    super(tee, you);\n"
        + "  }\n"
        + "  @Override public final String toString() {\n"
        + "    return \"Test{\"\n"
        + "        + \"tee=██, \"\n"
        + "        + \"you=\" + you()\n"
        + "        + '}';\n"
        + "  }\n"
        + "}\n"
    );

    assertAbout(javaSources())
        .that(Arrays.asList(redacted, nullable, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedSource);
  }
}
