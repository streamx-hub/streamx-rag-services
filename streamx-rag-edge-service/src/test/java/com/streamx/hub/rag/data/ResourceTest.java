package com.streamx.hub.rag.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.streamx.blueprints.data.Resource;
import com.streamx.blueprints.data.Typed;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class ResourceTest {

  @Test
  void verifyReflectConfigJson() {
    assertThat(Resource.class)
        .hasAnnotation(RegisterForReflection.class);
  }

  @Test
  void nonRecordDataModelClassesShouldContainJsonCreatorAnnotation() {
    assertThat(Resource.class.getConstructors())
        .anyMatch(c -> c.isAnnotationPresent(JsonCreator.class));
  }

  @Test
  void dataModelClassesShouldBeNullSafe() throws Exception {
    Class<Resource> dataModelClass = Resource.class;
    if (Modifier.isAbstract(dataModelClass.getModifiers())) {
      return;
    }
    for (Constructor<?> constructor : dataModelClass.getConstructors()) {
      Object dataClassInstance = instantiateWithNullParameters(constructor);
      if (dataClassInstance instanceof Resource resource) {
        assertThat(Resource.isEmpty(resource)).isTrue();
        assertThat(resource.getContent()).isNull();
        assertThat(resource.getContentAsBytes()).isNull();
        assertThat(resource.getContentAsString()).isNull();
      }
      if (dataClassInstance instanceof Typed typed) {
        assertThat(typed.getType()).isNull();
      }
    }
  }

  private static Object instantiateWithNullParameters(Constructor<?> constructor) throws Exception {
    Object[] nulls = IntStream
        .rangeClosed(1, constructor.getParameterCount())
        .mapToObj(i -> null)
        .toArray(Object[]::new);
    return constructor.newInstance(nulls);
  }
}
