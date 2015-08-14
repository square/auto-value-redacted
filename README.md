# AutoValue: Redacted Extension

An extension for Google's [AutoValue](https://github.com/google/auto/tree/master/value) that omits
`@Redacted` field values from `toString()`.

## Usage

Include the extension in your project, define a `@Redacted` annotation, and apply it to any
fields that you wish to redact.

```java
@Retention(SOURCE)
@Target({METHOD, PARAMETER, FIELD})
public @interface Redacted {
}
```

```java
@AutoValue public abstract class User {
  @Redacted public abstract String name();
}
```

## Download

Add a Gradle dependency:

```groovy
compile 'com.squareup.auto.value:auto-value-redacted:0.1-SNAPSHOT'
```

## License

```
Copyright 2015 Square, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
