package com.zjw.launcher;

import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.List;

/**
 * An object input stream that only allows classes used by the launcher protocol to be in the
 * serialized stream. See SPARK-20922.
 */
class FilteredObjectInputStream extends ObjectInputStream {

  private static final List<String> ALLOWED_PACKAGES = Arrays.asList(
    "org.apache.spark.launcher.",
    "java.lang.");

  FilteredObjectInputStream(InputStream is) throws IOException {
    super(is);
  }

  @Override
  protected Class<?> resolveClass(ObjectStreamClass desc)
      throws IOException, ClassNotFoundException {

    boolean isValid = ALLOWED_PACKAGES.stream().anyMatch(p -> desc.getName().startsWith(p));
    if (!isValid) {
      throw new IllegalArgumentException(
        String.format("Unexpected class in stream: %s", desc.getName()));
    }
    return super.resolveClass(desc);
  }

}
