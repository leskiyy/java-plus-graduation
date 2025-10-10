package ru.practicum.client;

import feign.FeignException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;

@FeignClient(name = "user-service", path = "api/v1/user")
public interface UserClient {
    @GetMapping
    UserDto getUserById(@RequestParam Long userId) throws FeignException;

    @GetMapping("/short")
    UserShortDto getUserShortDroById(@RequestParam Long userId) throws FeignException;
}
