package com.qudini.reactive.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.qudini.reactive.utils.jackson.EmptyToNullStringDeserializer;
import com.qudini.reactive.utils.jackson.UnmodifiableCollectionsDeserializerModifier;
import lombok.NoArgsConstructor;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonSetter.Value.forValueNulls;
import static com.fasterxml.jackson.annotation.Nulls.AS_EMPTY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class MoreJackson {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Object>> LIST_TYPE = new TypeReference<>() {
    };

    /**
     * To deserialize into a {@link Map}.
     */
    public static TypeReference<Map<String, Object>> toMap() {
        return MAP_TYPE;
    }

    /**
     * To deserialize into a {@link List}.
     */
    public static TypeReference<List<Object>> toList() {
        return LIST_TYPE;
    }

    /**
     * <p>Creates a new {@link ObjectMapper} with the following configuration:</p>
     * <ul>
     *     <li>Loads available modules</li>
     *     <li>Serialization:
     *         <ul>
     *             <li>Disables {@link SerializationFeature#WRITE_DATES_AS_TIMESTAMPS}</li>
     *             <li>Includes {@link JsonInclude.Include#NON_EMPTY} only</li>
     *         </ul>
     *     </li>
     *     <li>Deserialization:
     *         <ul>
     *             <li>Deserializes an empty string into null</li>
     *             <li>Disables {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}</li>
     *             <li>Makes {@link List}s, {@link Set}s and {@link Map}s unmodifiable</li>
     *             <li>Uses an empty {@link List}/{@link Set}/{@link Map} instead of null</li>
     *             <li>Uses {@link Nulls#AS_EMPTY} on setters for both value and content</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    public static ObjectMapper newObjectMapper() {
        var module = new SimpleModule()
                .addDeserializer(String.class, new EmptyToNullStringDeserializer())
                .setDeserializerModifier(new UnmodifiableCollectionsDeserializerModifier());
        return new ObjectMapper()
                .disable(FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(NON_EMPTY)
                .setDefaultSetterInfo(forValueNulls(AS_EMPTY, AS_EMPTY))
                .registerModule(module)
                .findAndRegisterModules();
    }

    /**
     * <p>Creates a new {@link WebClient} using the {@link ObjectMapper} returned by {@link #newObjectMapper()}
     * for both the encoder ({@link Jackson2JsonEncoder}) and the decoder ({@link Jackson2JsonDecoder}).</p>
     */
    public static WebClient newWebClient() {
        return newWebClient(newObjectMapper());
    }

    /**
     * <p>Creates a new {@link WebClient} using the given {@link ObjectMapper}
     * for both the encoder ({@link Jackson2JsonEncoder}) and the decoder ({@link Jackson2JsonDecoder}).</p>
     */
    public static WebClient newWebClient(ObjectMapper mapper) {
        var exchangeStrategies = ExchangeStrategies
                .builder()
                .codecs(configurer -> configureWebClient(configurer, mapper))
                .build();
        return WebClient
                .builder()
                .exchangeStrategies(exchangeStrategies)
                .build();
    }

    private static void configureWebClient(ClientCodecConfigurer configurer, ObjectMapper mapper) {
        var codecs = configurer.defaultCodecs();
        codecs.jackson2JsonEncoder(new Jackson2JsonEncoder(mapper));
        codecs.jackson2JsonDecoder(new Jackson2JsonDecoder(mapper));
    }

}