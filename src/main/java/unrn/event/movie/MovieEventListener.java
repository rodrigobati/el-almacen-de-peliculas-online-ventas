package unrn.event.movie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class MovieEventListener {

    private static final Logger log = LoggerFactory.getLogger(MovieEventListener.class);

    private final MovieEventHandler handler;

    public MovieEventListener(MovieEventHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${rabbitmq.event.movie.queue.name}", durable = "true"), exchange = @Exchange(value = "${rabbitmq.catalogo.events.exchange}", type = "topic"), key = {
            "MovieCreated.v1", "MovieUpdated.v1", "MovieRetired.v1" }))
    public void onMovieEvent(MovieEventEnvelope envelope) {
        log.info("Evento de catalogo recibido: {}", envelope.eventType());
        handler.handle(envelope);
    }
}
