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
apt 'com.squareup.auto.value:auto-value-redacted:0.1.0'
```
(Using the [android-apt][apt] plugin)

or Maven:
```xml
<dependency>
  <groupId>com.squareup.auto.value</groupId>
  <artifactId>auto-value-redacted</artifactId>
  <version>0.1.0</version>
  <scope>provided</scope>
</dependency>
```

Note: The current release is only confirmed to support AutoValue 1.2-rc1.

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].


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




 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
 [apt]: https://bitbucket.org/hvisser/android-apt
