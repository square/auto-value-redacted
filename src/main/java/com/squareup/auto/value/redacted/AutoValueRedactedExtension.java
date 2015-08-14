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

import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValueExtension;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

@AutoService(AutoValueExtension.class)
public final class AutoValueRedactedExtension implements AutoValueExtension {

  @Override
  public boolean applicable(Context context) {
    Map<String, ExecutableElement> properties = context.properties();
    for (ExecutableElement element : properties.values()) {
      if (getAnnotations(element).contains("Redacted")) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean mustBeAtEnd(Context context) {
    return false;
  }

  @Override
  public String generateClass(Context context, String className, String classToExtend,
      boolean isFinal) {
    String packageName = context.packageName();
    Name superName = context.autoValueClass().getSimpleName();
    Map<String, ExecutableElement> properties = context.properties();

    TypeSpec subclass = TypeSpec.classBuilder(className) //
        .addModifiers(isFinal ? Modifier.FINAL : Modifier.ABSTRACT) //
        .superclass(ClassName.get(packageName, classToExtend)) //
        .addMethod(generateConstructor(properties)) //
        .addMethod(generateToString(superName, properties)) //
        .build();

    JavaFile javaFile = JavaFile.builder(packageName, subclass).build();
    return javaFile.toString();
  }

  private static MethodSpec generateConstructor(Map<String, ExecutableElement> properties) {
    List<ParameterSpec> params = Lists.newArrayList();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      TypeName typeName = TypeName.get(entry.getValue().getReturnType());
      params.add(ParameterSpec.builder(typeName, entry.getKey()).build());
    }

    StringBuilder body = new StringBuilder("super(");
    for (int i = properties.size(); i > 0; i--) {
      body.append("$N");
      if (i > 1) body.append(", ");
    }
    body.append(")");

    return MethodSpec.constructorBuilder() //
        .addParameters(params) //
        .addStatement(body.toString(), properties.keySet().toArray()) //
        .build();
  }

  private static MethodSpec generateToString(Name superName,
      Map<String, ExecutableElement> properties) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("toString") //
        .addAnnotation(Override.class) //
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL) //
        .returns(String.class) //
        .addCode("return \"$L{\"\n", superName) //
        .addCode("$>$>");

    int count = 0;
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      String propertyName = entry.getKey();
      ExecutableElement propertyElement = entry.getValue();
      TypeName propertyType = TypeName.get(entry.getValue().getReturnType());
      ImmutableSet<String> propertyAnnotations = getAnnotations(propertyElement);

      builder.addCode("+ \"$N=\" + ", propertyName);
      if (propertyAnnotations.contains("Redacted")) {
        if (propertyAnnotations.contains("Nullable")) {
          builder.addCode("($N() != null ? \"██\" : null)", propertyName);
        } else {
          builder.addCode("\"██\"");
        }
      } else if (propertyType instanceof ArrayTypeName) {
        builder.addCode("$T.toString($N())", Arrays.class, propertyName);
      } else if (propertyAnnotations.contains("Nullable")) {
        builder.addCode("($N() != null ? $N() : \"null\")", propertyName, propertyName);
      } else {
        builder.addCode("$N()", propertyName);
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

  private static ImmutableSet<String> getAnnotations(ExecutableElement element) {
    ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<String>();

    List<? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
    for (AnnotationMirror annotation : annotations) {
      builder.add(annotation.getAnnotationType().asElement().getSimpleName().toString());
    }

    return builder.build();
  }
}
