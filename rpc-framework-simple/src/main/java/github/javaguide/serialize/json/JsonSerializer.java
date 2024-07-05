package github.javaguide.serialize.json;


import com.alibaba.fastjson.JSON;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import github.javaguide.exception.SerializeException;
import github.javaguide.serialize.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * use fastJSON
 *
 */
public class JsonSerializer implements Serializer {
    @Override
    public byte[] serialize(Object obj) {
        try {
            byte[] jsonBytes = JSON.toJSONBytes(obj);
            return jsonBytes;
        } catch (Exception e) {
            throw new SerializeException("Serialization failed");
        }

    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {

        try {
            return JSON.parseObject(bytes, clazz);
        } catch (Exception e) {
            throw new SerializeException("Deserialization failed");
        }

    }
}
