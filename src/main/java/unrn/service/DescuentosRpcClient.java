package unrn.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import unrn.dto.ValidarCuponRequest;
import unrn.dto.ValidarCuponResponse;

@Component
public class DescuentosRpcClient {

    static final String ERROR_DESCUENTOS_NO_DISPONIBLE = "No se pudo validar el cupón (servicio descuentos no disponible)";
    static final String ERROR_RESPUESTA_INVALIDA = "Respuesta inválida del servicio de descuentos";

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final long timeoutMs;

    public DescuentosRpcClient(
            RabbitTemplate rabbitTemplate,
            @Value("${rabbitmq.descuentos.exchange}") String exchange,
            @Value("${rabbitmq.descuentos.cupon.validar.routing-key}") String routingKey,
            @Value("${rabbitmq.descuentos.cupon.validar.timeout-ms:1500}") long timeoutMs) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.timeoutMs = timeoutMs;
    }

    public ValidarCuponResponse validarCupon(String nombreCupon) {
        rabbitTemplate.setReplyTimeout(timeoutMs);

        Object resp = rabbitTemplate.convertSendAndReceive(
                exchange,
                routingKey,
                new ValidarCuponRequest(nombreCupon)
        );

        if (resp == null) {
            throw new RuntimeException(ERROR_DESCUENTOS_NO_DISPONIBLE);
        }
        if (!(resp instanceof ValidarCuponResponse r)) {
            throw new RuntimeException(ERROR_RESPUESTA_INVALIDA + ": " + resp.getClass());
        }
        return r;
    }
}