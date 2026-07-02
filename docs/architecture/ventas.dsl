workspace "Vertical Ventas" "Arquitectura C4 de la vertical Ventas de El Almacen de Peliculas Online" {

    !identifiers hierarchical

    model {
        cliente = person "Cliente" "Persona autenticada que administra su carrito y realiza compras."

        frontend = softwareSystem "Frontend Web" "Aplicacion React desde la que el cliente consulta el catalogo, administra el carrito y revisa sus compras." {
            tags "Web Application"
        }

        apiGateway = softwareSystem "API Gateway" "Punto de entrada que enruta las solicitudes HTTP hacia las verticales."

        keycloak = softwareSystem "Keycloak" "Proveedor de identidad que autentica al cliente y emite tokens JWT." {
            tags "Identity Provider"
        }

        catalogo = softwareSystem "Vertical Catalogo" "Administra las peliculas y su stock."

        descuentos = softwareSystem "Vertical Descuentos" "Valida cupones y devuelve el descuento aplicable."

        notificaciones = softwareSystem "Vertical Notificaciones" "Procesa compras confirmadas y envia las notificaciones correspondientes."

        rabbitmq = softwareSystem "RabbitMQ" "Broker compartido para eventos de peliculas, stock, compras y validacion de cupones." {
            tags "Message Broker"
        }

        ventas = softwareSystem "Vertical Ventas" "Gestiona el carrito, la confirmacion de compras, el historial y la coordinacion del stock." {
            tags "Ventas"

            api = container "API de Ventas" "Expone las operaciones de carrito y compras, mantiene una proyeccion del catalogo y coordina la confirmacion asincrona." "Java 21, Spring Boot 4" {
                tags "Ventas"
            }

            database = container "Base de Datos de Ventas" "Persiste carritos, compras, items, proyeccion de peliculas, eventos procesados y outbox." "MySQL" {
                tags "Database"
            }
        }

        cliente -> ventas "Administra el carrito, confirma compras y consulta su historial"
        ventas -> keycloak "Valida la identidad y autorizacion del cliente" "OAuth 2.0 / JWT"
        ventas -> catalogo "Sincroniza peliculas y coordina la validacion de stock"
        ventas -> descuentos "Solicita la validacion de cupones"
        ventas -> notificaciones "Emite compras confirmadas para su notificacion"

        cliente -> frontend "Usa" "HTTPS"
        frontend -> keycloak "Inicia sesion y obtiene tokens" "OIDC / OAuth 2.0"
        frontend -> apiGateway "Envia operaciones de carrito y compras" "HTTPS / JSON"
        apiGateway -> ventas.api "Enruta las solicitudes de Ventas" "HTTP / JSON"

        ventas.api -> ventas.database "Lee y persiste carritos, compras, proyecciones y eventos" "JDBC / JPA"
        ventas.api -> keycloak "Obtiene claves publicas para validar tokens" "HTTPS / JWKS"
        ventas.api -> catalogo "Reconstruye la proyeccion local de peliculas" "HTTP / JSON"

        ventas.api -> rabbitmq "Publica solicitudes de stock y compras confirmadas; consume cambios de peliculas y resultados de stock" "AMQP"
        rabbitmq -> catalogo "Entrega solicitudes de validacion de stock" "AMQP"
        catalogo -> rabbitmq "Publica cambios de peliculas y resultados de validacion de stock" "AMQP"
        rabbitmq -> descuentos "Solicita la validacion de cupones y retorna la respuesta RPC" "AMQP / RPC"
        rabbitmq -> notificaciones "Entrega eventos de compra confirmada" "AMQP"
    }

    views {
        systemContext ventas "VentasContexto" {
            title "Vertical Ventas - Diagrama de contexto"
            include cliente ventas keycloak catalogo descuentos notificaciones
            autoLayout tb 350 250
        }

        container ventas "VentasContenedores" {
            title "Vertical Ventas - Diagrama de contenedores"
            include *
            include cliente frontend
            autoLayout tb 350 250
        }

        styles {
            element "Element" {
                color #ffffff
                stroke #7f1d1d
                strokeWidth 3
                shape roundedbox
            }

            element "Person" {
                background #08427b
                stroke #052e56
                shape person
            }

            element "Software System" {
                background #6b7280
                stroke #374151
            }

            element "Ventas" {
                background #d9232b
                stroke #991b1b
            }

            element "Database" {
                background #7f1d1d
                stroke #450a0a
                shape cylinder
            }

            element "Web Application" {
                background #2563eb
                stroke #1e3a8a
                shape webBrowser
            }

            element "Identity Provider" {
                background #7c3aed
                stroke #4c1d95
                shape hexagon
            }

            element "Message Broker" {
                background #f59e0b
                color #111827
                stroke #92400e
                shape pipe
            }

            element "Boundary" {
                stroke #991b1b
                strokeWidth 4
            }

            relationship "Relationship" {
                color #4b5563
                thickness 3
                routing orthogonal
            }
        }
    }

    configuration {
        scope softwaresystem
    }
}
