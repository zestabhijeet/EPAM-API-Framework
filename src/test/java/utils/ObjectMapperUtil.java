package utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import pojo.UserType;
import java.io.File;

public class ObjectMapperUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();


   static {
       objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
   }
   public static ObjectMapper getInstance(){
       return objectMapper;
   }

}

