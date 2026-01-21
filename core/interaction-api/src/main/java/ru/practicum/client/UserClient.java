// interaction-api/src/main/java/ru/practicum/client/UserClient.java
package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.user.UserDtoOut;

import java.util.List;

@FeignClient(name = "user-service", path = "/internal/users")
public interface UserClient {

    @GetMapping("/{userId}")
    UserDtoOut getUserById(@PathVariable Long userId);

    @GetMapping("/batch")
    List<UserDtoOut> getUsersByIds(@RequestParam List<Long> ids);

    @GetMapping("/exists/{userId}")
    Boolean userExists(@PathVariable Long userId);

    // Добавляем метод для проверки нескольких пользователей
    @GetMapping("/exists/batch")
    List<Boolean> usersExist(@RequestParam List<Long> userIds);
}