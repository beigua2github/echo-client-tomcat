package com.spring.app.mvc.codec;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.*;

public class JacksonEncoder implements Encoder {
    private final ObjectMapper mapper;

    public JacksonEncoder() {
        this((Iterable) Collections.emptyList());
    }

    public JacksonEncoder(Iterable<Module> modules) {
        this((new ObjectMapper()).setSerializationInclusion(Include.NON_NULL).configure(SerializationFeature.INDENT_OUTPUT, true).registerModules(modules));
    }

    public JacksonEncoder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void encode(Object object, Type bodyType, RequestTemplate template) {
        try {
            if (template.method().equalsIgnoreCase(RequestMethod.POST.name())) {
                if (template.toString().indexOf(MediaType.APPLICATION_JSON_VALUE) >= 0) {
                    JavaType javaType = this.mapper.getTypeFactory().constructType(bodyType);
                    template.body(this.mapper.writerFor(javaType).writeValueAsString(object));
                } else {
                    String params = this.getParam(object);
                    if (!StringUtils.isEmpty(params)) {
                        template.body(params);
                    }
                }
            } else {
                Field field = template.getClass().getDeclaredField("url");
                field.setAccessible(true);
                StringBuilder url = new StringBuilder(field.get(template).toString());
                String params = this.getParam(object);
                if (!StringUtils.isEmpty(params)) {
                    url.append("?").append(params);
                    field.set(template, url);
                }
            }
        } catch (Exception var5) {
            throw new EncodeException(var5.getMessage(), var5);
        }
    }

    private String getParam(Object object) {
        StringBuilder params = new StringBuilder("");
        if (object instanceof Map) {
            Set<Map.Entry<Object, Object>> set = ((Map) object).entrySet();
            set.stream().forEach(entry -> {
                Object value = entry.getValue();
                try {
                    params.append("&").append(entry.getKey()).append("=").append(URLEncoder.encode(this.toStr(value), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                }
            });
        } else {
            List<Field> fields = new ArrayList<>();
            Class objClass = object.getClass();
            while (objClass != null) {
                if (objClass.getDeclaredFields() != null) {
                    fields.addAll(Arrays.asList(objClass.getDeclaredFields()));
                }
                objClass = objClass.getSuperclass();
            }
            fields.stream().forEach(field -> {
                field.setAccessible(true);
                try {
                    params.append("&").append(field.getName()).append("=").append(URLEncoder.encode(this.toStr(field.get(object)), "UTF-8"));
                } catch (Exception e) {
                }
            });
        }
        if (params.length() > 0) {
            return params.substring(1);
        }
        return "";
    }

    private String toStr(Object value) {
        return value != null ? (String) value : "";
    }

}
