package com.sigursoft.jpms.k8s.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JSONObjectTests {

    @Test
    public void testSimpleJson(){
      // given
      String key = "KEY";
      String value = "VALUE";
      JSONObject serializer = new JSONObject();
      // when
      serializer.put(key, value);
      // then
      assertEquals("{\"KEY\":\"VALUE\"}", serializer.toString());
    }

}
