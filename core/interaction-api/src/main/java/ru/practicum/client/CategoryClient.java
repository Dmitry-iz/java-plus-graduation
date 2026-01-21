// interaction-api/src/main/java/ru/practicum/client/CategoryClient.java
package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.category.CategoryDtoOut;

import java.util.List;

@FeignClient(name = "event-service", path = "/internal/categories")
public interface CategoryClient {

    @GetMapping("/{categoryId}")
    CategoryDtoOut getCategoryById(@PathVariable Long categoryId);

    @GetMapping
    List<CategoryDtoOut> getCategoriesByIds(@RequestParam List<Long> ids);

    @GetMapping("/exists/{categoryId}")
    Boolean categoryExists(@PathVariable Long categoryId);
}