package org.example.sapsanuserservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/debug") // Исправил аннотацию
public class DebugController {

    @GetMapping("/headers") // Исправил аннотацию
    public Map<String, String> echoHeaders(@RequestHeader Map<String, String> headers) {
        return headers.entrySet().stream()
                .filter(x -> x.getKey().toLowerCase().startsWith("x-user-"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        // Логика декодирования: берем значение и превращаем из URL-формата в UTF-8
                        entry -> {
                            try {
                                return URLDecoder.decode(entry.getValue(), StandardCharsets.UTF_8);
                            } catch (Exception e) {
                                // Если вдруг пришла строка не в URL формате, возвращаем как есть
                                return entry.getValue();
                            }
                        }
                ));
    }
}