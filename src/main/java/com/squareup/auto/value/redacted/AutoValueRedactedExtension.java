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
import com.google.auto.value.extension.AutoValueExtension;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

@AutoService(AutoValueExtension.class)
public final class AutoValueRedactedExtension extends AutoValueExtension {

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
  public String generateClass(Context context, String className, String classToExtend,
      boolean isFinal) {
    String packageName = context.packageName();
    TypeElement autoValueClass = context.autoValueClass();
    List<? extends TypeParameterElement> typeParameters = autoValueClass.getTypeParameters();
    Name superName = autoValueClass.getSimpleName();
    Map<String, ExecutableElement> properties = context.properties();

    TypeSpec.Builder subclass = TypeSpec.classBuilder(className) //
        .addModifiers(isFinal ? Modifier.FINAL : Modifier.ABSTRACT) //
        .addMethod(generateConstructor(properties)) //
        .addMethod(generateToString(superName, properties));

    ClassName superclass = ClassName.get(packageName, classToExtend);
    if (typeParameters.isEmpty()) {
      subclass.superclass(superclass);
    } else {
      List<TypeVariableName> typeVariables = new ArrayList<>();
      for (TypeParameterElement typeParameter : typeParameters) {
        typeVariables.add(TypeVariableName.get(typeParameter));
      }
      subclass.addTypeVariables(typeVariables)
          .superclass(
              ParameterizedTypeName.get(superclass, typeVariables.toArray(new TypeName[0])));
    }

    JavaFile javaFile = JavaFile.builder(packageName, subclass.build()).build();
    return javaFile.toString();
  }

  private static MethodSpec generateConstructor(Map<String, ExecutableElement> properties) {
    List<ParameterSpec> params = new ArrayList<>();
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
      String methodName = propertyElement.getSimpleName().toString();
      TypeName propertyType = TypeName.get(entry.getValue().getReturnType());
      Set<String> propertyAnnotations = getAnnotations(propertyElement);

      boolean redacted = propertyAnnotations.contains("Redacted");
      boolean nullable = propertyAnnotations.contains("Nullable");
      boolean last = ++count == properties.size();

      // Special-case this configuration since we can pre-concat constant strings.
      if (redacted && !nullable) {
        builder.addCode("+ \"$N=██", propertyName);
        if (!last) builder.addCode(", ");
        builder.addCode("\"\n");
        continue;
      }

      builder.addCode("+ \"$N=\" + ", propertyName);

      CodeBlock propertyToString;
      if (redacted) {
        propertyToString = CodeBlock.of("\"██\"");
      } else if (propertyType instanceof ArrayTypeName) {
        propertyToString = CodeBlock.of("$T.toString($N())", Arrays.class, methodName);
      } else {
        propertyToString = CodeBlock.of("$N()", methodName);
      }

      if (nullable) {
        builder.addCode("($N() != null ? $L : null)", methodName, propertyToString);
      } else {
        builder.addCode(propertyToString);
      }

      if (!last) {
        builder.addCode(" + \", \"");
      }

      builder.addCode("\n");
    }

    return builder //
        .addStatement("+ '}'") //
        .addCode("$<$<")
        .build();
  }

  private static Set<String> getAnnotations(ExecutableElement element) {
    Set<String> set = new LinkedHashSet<>();

    List<? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
    for (AnnotationMirror annotation : annotations) {
      set.add(annotation.getAnnotationType().asElement().getSimpleName().toString());
    }

    return Collections.unmodifiableSet(set);
  }

  @Override
  public IncrementalExtensionType incrementalType(ProcessingEnvironment processingEnvironment) {
    return IncrementalExtensionType.ISOLATING;
  }
}
