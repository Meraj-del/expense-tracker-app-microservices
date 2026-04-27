package authSerivce.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import authSerivce.model.UserInfoDto;
import java.util.Map;

public class UserInfoSerializer implements Serializer<UserInfoDto> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public byte[] serialize(String arg0, UserInfoDto arg1) {
        byte[] retVal=null;
        ObjectMapper mapper = new ObjectMapper();
        try{
            retVal=mapper.writeValueAsString(arg1).getBytes();
        }catch (Exception e){
            e.printStackTrace();
        }
        return retVal;
    }

    @Override
    public void close() {}
}
