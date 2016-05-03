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

import com.gabrielittner.auto.value.util.ElementUtil;
import com.gabrielittner.auto.value.util.Property;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.Arrays;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

import static com.gabrielittner.auto.value.util.AutoValueUtil.newTypeSpecBuilder;

@AutoService(AutoValueExtension.class)
public final class AutoValueRedactedExtension extends AutoValueExtension {

  @Override
  public boolean applicable(Context context) {
    Map<String, ExecutableElement> properties = context.properties();
    for (ExecutableElement element : properties.values()) {
      if (ElementUtil.hasAnnotationWithName(element, "Redacted")) {
        return true;
      }
    }

    return false;
  }

  @Override
  public String generateClass(Context context, String className, String classToExtend,
      boolean isFinal) {
    String packageName = context.packageName();
    Name superName = context.autoValueClass().getSimpleName();
    ImmutableList<Property> properties = properties(context);

    TypeSpec subclass = newTypeSpecBuilder(context, className, classToExtend, isFinal)
        .addMethod(generateToString(superName, properties)) //
        .build();

    JavaFile javaFile = JavaFile.builder(packageName, subclass).build();
    return javaFile.toString();
  }

  private static MethodSpec generateToString(Name superName,
      ImmutableList<Property> properties) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("toString") //
        .addAnnotation(Override.class) //
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL) //
        .returns(String.class) //
        .addCode("return \"$L{\"\n", superName) //
        .addCode("$>$>");

    int count = 0;
    for (Property property : properties) {
      builder.addCode("+ \"$N=\" + ", property.humanName());

      CodeBlock propertyToString;
      if (property.annotations().contains("Redacted")) {
        propertyToString = CodeBlock.builder() //
            .add("\"██\"") //
            .build();
      } else if (property.type() instanceof ArrayTypeName) {
        propertyToString = CodeBlock.builder() //
            .add("$T.toString($N())", Arrays.class, property.methodName()) //
            .build();
      } else {
        propertyToString = CodeBlock.builder() //
            .add("$N()", property.methodName()) //
            .build();
      }

      if (property.nullable()) {
        builder.addCode("($N() != null ? $L : null)", property.methodName(), propertyToString);
      } else {
        builder.addCode(propertyToString);
      }

      if (count++ < properties.size() - 1) {
        builder.addCode(" + \", \"");
      }

      builder.addCode("\n");
    }

    return builder //
        .addStatement("+ '}'") //
        .addCode("$<$<")
        .build();
  }

  private static ImmutableList<Property> properties(AutoValueExtension.Context context) {
    ImmutableList.Builder<Property> values = ImmutableList.builder();
    for (Map.Entry<String, ExecutableElement> entry : context.properties().entrySet()) {
      values.add(new Property(entry.getKey(), entry.getValue()));
    }
    return values.build();
  }
}
