package com.spring.app.mvc.codec;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import feign.Response;
import feign.Util;
import feign.codec.Decoder;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.stream.Collectors;

public class JacksonDecoder implements Decoder {
    private final ObjectMapper mapper;

    public JacksonDecoder() {
        this((Iterable) Collections.emptyList());
    }

    public JacksonDecoder(Iterable<Module> modules) {
        this((new ObjectMapper()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).registerModules(modules));
    }

    public JacksonDecoder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException {
        if (response.status() == 404) {
            return Util.emptyValueOf(type);
        } else if (response.body() == null) {
            return null;
        } else if (type instanceof Class && type == String.class) {
            InputStream inputStream = response.body().asInputStream();
            String result = new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            return result;
        } else {
            Reader reader = response.body().asReader();
            if (!reader.markSupported()) {
                reader = new BufferedReader(reader, 1);
            }

            try {
                reader.mark(1);
                if (reader.read() == -1) {
                    return null;
                } else {
                    reader.reset();
                    Object o = this.mapper.readValue(reader, this.mapper.constructType(type));
                    return o;
                }
            } catch (RuntimeJsonMappingException var5) {
                if (var5.getCause() != null && var5.getCause() instanceof IOException) {
                    throw IOException.class.cast(var5.getCause());
                } else {
                    throw var5;
                }
            }
        }
    }
}
