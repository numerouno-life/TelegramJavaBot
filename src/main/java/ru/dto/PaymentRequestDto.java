package ru.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.model.enums.ServiceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {

    BigDecimal amount;
    String clientPhone;
    String clientName;
    ServiceType serviceType;
    LocalDateTime serviceDate;
    String comment;

}
