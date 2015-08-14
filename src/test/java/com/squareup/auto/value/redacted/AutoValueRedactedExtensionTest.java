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
        + "package test;"
        + "import java.lang.annotation.Retention;"
        + "import java.lang.annotation.Target;"
        + "import static java.lang.annotation.ElementType.FIELD;"
        + "import static java.lang.annotation.ElementType.METHOD;"
        + "import static java.lang.annotation.ElementType.PARAMETER;"
        + "import static java.lang.annotation.RetentionPolicy.SOURCE;"
        + "@Retention(SOURCE)"
        + "@Target({METHOD, PARAMETER, FIELD})"
        + "public @interface Redacted {"
        + "}");
    nullable = JavaFileObjects.forSourceString("test.Nullable", ""
        + "package test;"
        + "import java.lang.annotation.Retention;"
        + "import java.lang.annotation.Target;"
        + "import static java.lang.annotation.ElementType.FIELD;"
        + "import static java.lang.annotation.ElementType.METHOD;"
        + "import static java.lang.annotation.ElementType.PARAMETER;"
        + "import static java.lang.annotation.RetentionPolicy.CLASS;"
        + "@Retention(CLASS)"
        + "@Target({METHOD, PARAMETER, FIELD})"
        + "public @interface Nullable {"
        + "}");
  }

  @Test public void simple() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;"
            + "import com.google.auto.value.AutoValue;"
            + "@AutoValue public abstract class Test {"
            + "@Redacted public abstract String a();"
            + "@Redacted @Nullable public abstract String b();"
            + "@Redacted public abstract int c();"
            + "public abstract String d();"
            + "@Nullable public abstract String e();"
            + "public abstract int f();"
            + "public abstract int[] g();"
            + "}"
    );

    JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
            + "package test;"
            + "import java.lang.Override;"
            + "import java.lang.String;"
            + "import java.util.Arrays;"
            + "final class AutoValue_Test extends $AutoValue_Test {"
            + "  AutoValue_Test(String a, String b, int c, String d, String e, int f, int[] g) {"
            + "    super(a, b, c, d, e, f, g);"
            + "  }"
            + "  @Override public final String toString() {"
            + "    return \"Test{\""
            + "        + \"a=\" + \"██\" + \", \""
            + "        + \"b=\" + (b() != null ? \"██\" : null) + \", \""
            + "        + \"c=\" + \"██\" + \", \""
            + "        + \"d=\" + d() + \", \""
            + "        + \"e=\" + (e() != null ? e() : \"null\") + \", \""
            + "        + \"f=\" + f() + \", \""
            + "        + \"g=\" + Arrays.toString(g())"
            + "        + '}';"
            + "  }"
            + "}"
    );

    assertAbout(javaSources())
        .that(Arrays.asList(redacted, nullable, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedSource);
  }
}
